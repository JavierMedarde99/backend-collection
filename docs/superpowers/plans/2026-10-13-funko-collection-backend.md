# Plan: Implementación backend — Colección de Funko Pop

Fecha: 2026-10-13 · Rama: `feat/funko-collection` (desde `main` 4269405) · PR: #469 (`Closes #468`) · Spec: wiki PR #25

**Estado: COMPLETADO.** `mvn verify` verde (1162 tests, JaCoCo LINE ≥ 0.80). 6 commits, uno por tarea.

## Goal

Implementar el CRUD de la colección de **Funko Pop** en `wiki-collection-backend` siguiendo
estrictamente el patrón de las colecciones existentes (books/board-games/amiibos): cliente HTTP con
caché Caffeine, mapeo a dominio, persistencia Mongo, CRUD con auth + ownership, filtro `owner`
(`mine`/`other`/`all`) y búsqueda externa pública contra **HobbyQuantHQ**.

Criterio de terminado: `mvn verify` verde con JaCoCo LINE ≥ 0.80, todos los tests nuevos en verde, y
el API cumpliendo el contrato de `openspec/changes/feat-funko-collection/` (PR #25 en wiki-collection).

## Spec de referencia

- `wiki-collection/openspec/changes/feat-funko-collection/` → `design.md` (modelo, puertos, API,
  DTOs, trampas verificadas) y `tasks.md` (tarea 2: backend).
- `wiki-collection/research/external-apis/hobbyquanthq.md` — contrato HobbyQuantHQ verificado en vivo.
- Referencia de código: stack completo de `Amiibo` (coleccionable físico, 2 estados, búsqueda externa
  pública), `BoardGame` (OWNED/WISHLIST + visibilidad) y `GoogleBooksClient` (API key + retrys).

## Arquitectura

Hexagonal (reglas de `AGENTS.md`): dependencias hacia dentro, `application/` nunca importa
`infrastructure/`. Paquetes nuevos bajo `com.wikicollection`:

- `domain/model`: `Funko`, `FunkoStatus`, `FunkoSearchCriteria`, `FunkoSearchResult`, `FunkoCategory`
- `domain/port/in`: `FunkoUseCase`, `FunkoSearchUseCase`
- `domain/port/out`: `FunkoRepository`, `ExternalFunkoCatalogClient`
- `application/service`: `FunkoService`, `FunkoSearchService`
- `application/exception`: `FunkoNotFoundException`, `FunkoConflictException`
- `infrastructure/adapter/out/funko`: `FunkoApiClient` (+ DTOs JSON internos)
- `infrastructure/adapter/out/persistence`: `FunkoEntity`, `SpringDataFunkoRepository`,
  `FunkoPersistenceAdapter`, `FunkoEntityMapper`
- `infrastructure/adapter/in/web`: `FunkoController` + `dto/FunkoRequest`, `FunkoResponse`,
  `FunkoSearchResponse`, `FunkoCategoryResponse`, `FunkoDtoMapper`
- `infrastructure/config`: `RestClientConfig` (+bean), `CacheConfig` (+4 cachés),
  `StringToFunkoStatusConverter`, `GlobalExceptionHandler` (+404/+409), `MongoIndexMigration` (+colección)

## Tech stack

Java 25 · Spring Boot 4.1.1 · Spring Data MongoDB · Lombok 1.18.46 (vía `maven-compiler-plugin` con
`annotationProcessorPaths`) · Caffeine · tests: JUnit 5 + Mockito + `MockRestServiceServer` + MockMvc
(`spring-boot-starter-webmvc-test`).

## Global Constraints (valores a no inventar)

- Base URL: `https://hobbyquanthq.com/api/v1` — propiedad
  `hobbyquanthq.api.base-url=https://hobbyquanthq.com/api/v1`.
- API key: header `x-api-key`; propiedad `hobbyquanthq.api.key=${HOBBYQUANT_API_KEY:dev-key-change-me}`
  (la clave beta `dev-key-change-me` es pública; nunca hardcodear una clave real).
- Colección Mongo: **`funkos`** (minúsculas; en los docs el nombre lógico es `FUNKOS`).
- `FunkoStatus`: `OWNED`, `WISHLIST`. `externalId` = `id` (int) de la fuente, guardado como **String**.
- Imagen: `image_url` de `images.hobbydb.com`, **hotlink**. `image_valid` **no** se modela.
- `rarityScore` es un índice de la API, **no** un valor de mercado. Sin precios, sin barcode.
- Cachés Caffeine (grupo TTL `funkos`): `funkoSearch` (cliente, key `#query`), `funkoCategories`
  (cliente, key `'all'`), `funkoList` (service, key `Objects.hash(#criteria, #pageable, #owner, #viewerId)`),
  `funkoDetail` (service, key `#id`). `CACHE_NAMES` pasa de **43 a 47** y `PROPERTY_KEYS` también.
- Cliente: `RestClient` bean (`funkoRestClient`) vía `HttpClientProperties.restClientBuilder(baseUrl)`;
  header `x-api-key` por petición; retrys **4 intentos** (3 a 1 s) en 5xx/timeout, `retryIntervalMillis`
  inyectable (0 ms en tests); **4xx NO se reintenta**. Al agotar, relanzar
  (`RestClientResponseException` → 502 / `ResourceAccessException` → 503); nunca lista vacía por error de red.
- **`@Autowired` en el constructor público** (dos constructores: público `(RestClient, String apiKey)` y
  package-private `(RestClient, String apiKey, long retryIntervalMillis)`) — lección `AmiiboApiClient`.
- `/search` **no pagina**: el cliente pide `limit=100` (máximo) y `PagedResults.slice` corta en local;
  `total` de la fuente no es el total real.
- `/pops/{id}` desconocido → **404** (`{"detail":"Pop not found."}`) → `Optional.empty()` sin reintento.
- `/search` sin resultados → **200** con `results: []` = resultado vacío **válido**.
- `q` mínimo **2 caracteres**: la validación vive en `FunkoSearchService` (→ `IllegalArgumentException` → 400),
  no se propaga el 422 de la fuente.
- **Todos** los records del cliente llevan `@JsonIgnoreProperties(ignoreUnknown = true)` y **un solo constructor**
  (lección Scryfall). Nombres Jackson: `release_year`→`releaseYear`, `pop_type`→`popType`,
  `rarity_score`→`rarityScore`, `image_url`→`imageUrl`.
- `GET /api/v1/**` público; POST/PUT/DELETE con auth (`@CurrentUser`), ownership vía
  `OwnershipValidator`, 403 si ajeno. Admin exento.
- Unicidad `externalId`: `checkExternalIdUnique(externalId, currentId)` → `FunkoConflictException`, y `save`
  envuelto en try/catch `DuplicateKeyException` → 409.
- Visibilidad: `ResponseVisibility.canSeePrivate(ownerId, CollectionType.FUNKOS)` → `FunkoResponse.withoutPrivate()`.
- `CollectionType.FUNKOS("funkos")` (nuevo valor); `MongoIndexMigration.OWNER_COLLECTIONS` += `"funkos"`;
  `@Indexed` en `ownerId` y `externalId`. **Actualizar `UserPreferencesTest`** (mapa de colecciones activas).
- JaCoCo LINE ≥ 0.80 exigido por `mvn verify`.

## Review Focus

1. **Dos sobres distintos:** `/search` → `{results, query, total}`; `/pops/{id}` → objeto único;
   `/categories` → array `[{name,count}]`. DTOs separados.
2. **`limit=100` + corte local:** `/search` ignora `page`; el cliente pide el máximo y el service corta.
3. **`q` mínimo 2:** validado en el service (400), sin llamar a la fuente con 1 carácter.
4. **404 = no existe; vacío = 200 con `results: []`; 5xx = error tras 4 intentos.**
5. **Auth por cabecera:** el cliente siempre manda `x-api-key`; el test lo verifica con `header(...)`.
6. **Visibilidad/privacidad por colección:** filtro `owner` + recorte de `notes` dependen de
   `CollectionType.FUNKOS`; las claves de caché de lista incluyen `viewerId`.

---

## Task 1 — Cliente externo HobbyQuantHQ (TDD)

**Archivos (crear):**
- `domain/model/FunkoSearchResult.java` — record
  `(String externalId, String name, String series, String number, String category, String exclusive,
  Boolean vaulted, Integer releaseYear, String franchise, String popType, Integer rarityScore,
  String imageUrl, String externalSource)` con `EXTERNAL_SOURCE = "hobbyquanthq"`.
- `domain/model/FunkoCategory.java` — record `(String name, Integer count)`.
- `domain/port/out/ExternalFunkoCatalogClient.java`:
  ```java
  List<FunkoSearchResult> searchByName(String query);   // 200 con results:[] = vacío válido
  Optional<FunkoSearchResult> findById(String externalId); // 404 → empty, sin reintento
  List<FunkoCategory> listCategories();
  ```
- `infrastructure/adapter/out/funko/FunkoApiClient.java` — `@Component`, implementa el puerto.
  - DTOs anidados con `@JsonIgnoreProperties(ignoreUnknown = true)` y un solo constructor:
    - `FunkoApiSearchResponse(List<FunkoApiItem> results, String query, Integer total)`
    - `FunkoApiItem(Integer id, String name, String series, String number, String category, String exclusive,
      Boolean vaulted, Integer releaseYear, String franchise, String popType, Integer rarityScore, String imageUrl)`
      (con `@JsonProperty` para `release_year`, `pop_type`, `rarity_score`, `image_url`)
    - `FunkoApiCategoryResponse(String name, Integer count)`
  - Constructores: `@Autowired public FunkoApiClient(RestClient funkoRestClient, @Value("${hobbyquanthq.api.key:dev-key-change-me}") String apiKey)`
    ← `this(funkoRestClient, apiKey, 1000L)`; y package-private `(RestClient, String, long)`.
  - `searchByName`: `@Cacheable(cacheNames = CacheConfig.FUNKO_SEARCH, key = "#query")`; GET `/search` con
    `queryParam("q", query)` y `queryParam("limit", 100)`; header `x-api-key`; `withRetry(...)`; null/vacío → `List.of()`.
  - `findById`: `@Cacheable` no (detalle externo sin caché, como Amiibo); GET `/pops/{externalId}`;
    catch `RestClientResponseException` **4xx → `Optional.empty()`**; item null → empty.
  - `listCategories`: `@Cacheable(cacheNames = CacheConfig.FUNKO_CATEGORIES, key = "'all'")`; GET `/categories`;
    null → `List.of()`.
  - `withRetry(Supplier<T>)`: idéntico al patrón (4 intentos, solo 5xx/timeout).

**Archivos (modificar):**
- `infrastructure/config/RestClientConfig.java` — bean `funkoRestClient(@Value("${hobbyquanthq.api.base-url:https://hobbyquanthq.com/api/v1}") String baseUrl)`.
- `resources/application.properties` — `hobbyquanthq.api.base-url`, `hobbyquanthq.api.key` y
  `app.cache.ttl.funkos=3600s` (junto a los TTL de las colecciones).
- `infrastructure/config/CacheConfig.java` — constantes `FUNKO_SEARCH`, `FUNKO_CATEGORIES`, `FUNKO_LIST`,
  `FUNKO_DETAIL`; añadirlas a `CACHE_NAMES` y `PROPERTY_KEYS` (las 4 → `"funkos"`).

**Test:** `src/test/java/com/wikicollection/infrastructure/adapter/out/funko/FunkoApiClientTest.java` con
`MockRestServiceServer.bindTo(RestClient.builder()).build()` y el constructor package-private `(restClient, "test-key", 0)`.

| Test | Assert clave |
|---|---|
| `searchByName_sendsApiKeyHeader_andLimit100` | `header("x-api-key","test-key")`, `queryParam("q","batman")`, `queryParam("limit","100")` |
| `searchByName_mapsAllFields` | externalId="44", name, series, number, category, exclusive, vaulted, releaseYear, franchise, popType, rarityScore, imageUrl, externalSource="hobbyquanthq" |
| `searchByName_returnsEmpty_whenResultsEmpty` | `{"results":[],"total":0}` → lista vacía |
| `searchByName_returnsEmpty_whenNullContainer` | `{"results":null}` → lista vacía |
| `searchByName_retries5xx_4Attempts_thenRethrows` | 500 ×4 → `RestClientResponseException` |
| `searchByName_propagatesTimeout` | `ResourceAccessException` ×4 → relanza |
| `searchByName_doesNotRetry4xx` | 422 ×1 → relanza (no 4 intentos) |
| `findById_mapsObject` | `/pops/44` objeto → `Optional` mapeado |
| `findById_returnsEmpty_on404` | `withStatus(NOT_FOUND)` ×1 → `Optional.empty()` |
| `findById_retries5xx_thenRethrows` | 500 ×4 → relanza |
| `listCategories_mapsArray` | `[{"name":"Movies","count":1238}]` → `List<FunkoCategory>` |
| `listCategories_returnsEmpty_whenNull` | `null` → `List.of()` |

**Commit:** `feat(funko): cliente externo HobbyQuantHQ con caché y retrys`

---

## Task 2 — Capa de aplicación de búsqueda (TDD)

**Archivos (crear):**
- `domain/port/in/FunkoSearchUseCase.java`:
  ```java
  Page<FunkoSearchResult> search(String query, Pageable pageable);
  List<FunkoCategory> listCategories();
  ```
- `application/service/FunkoSearchService.java` — `name` obligatorio y **mínimo 2 caracteres**
  (`IllegalArgumentException` → 400); `PagedResults.slice(client.searchByName(query), pageable)`;
  `listCategories()` delega en el cliente.

**Test:** `FunkoSearchServiceTest` (unitario, `@Mock ExternalFunkoCatalogClient`):
- `search_rejectsBlankQuery` / `search_rejectsSingleCharacter`
- `search_slicesClientResults` (25 → página 10, total 25)
- `search_returnsEmptyPage_whenClientEmpty`
- `listCategories_delegatesToClient`

**Commit:** `feat(funko): servicio de búsqueda externa con mínimo de 2 caracteres`

---

## Task 3 — Persistencia Mongo (TDD)

**Archivos (crear):**
- `domain/model/FunkoStatus.java` — `OWNED, WISHLIST`.
- `domain/model/Funko.java` — POJO Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
  @ToString`): `id`, `ownerId`, `userOwned`, `externalId`, `name`, `series`, `number`, `category`, `exclusive`,
  `vaulted`, `releaseYear`, `franchise`, `popType`, `rarityScore`, `imageUrl`, `status`, `notes`, `dateAdded`.
- `domain/model/FunkoSearchCriteria.java` — record `(String name, FunkoStatus status, String ownerId,
  List<String> excludeOwnerIds)` + helpers `hasName/hasOwnerId/hasExcludeOwnerIds`.
- `domain/port/out/FunkoRepository.java` — `search`, `findById`, `findByExternalId`, `save`, `deleteById`,
  `updateOwnerName`.
- `infrastructure/adapter/out/persistence/FunkoEntity.java` — `@Document(collection = "funkos")`, `@Indexed`
  en `ownerId` y `externalId`, resto de campos.
- `.../SpringDataFunkoRepository.java` — `extends MongoRepository<FunkoEntity, String>` + `findByExternalId`.
- `.../FunkoEntityMapper.java` — `toEntity`/`toDomain` (null-safe, usa `UserOwnedMapping`).
- `.../FunkoPersistenceAdapter.java` — misma query que `AmiiboPersistenceAdapter` (name regex CI, status,
  ownerId `is`, excludeOwnerIds `nin`), `PageImpl`, `updateOwnerName`.

**Archivos (modificar):**
- `infrastructure/config/MongoIndexMigration.java` — `OWNER_COLLECTIONS` += `"funkos"`.

**Tests:** `FunkoEntityMapperTest` (round-trip, null) y `FunkoPersistenceAdapterTest` (filtros, save, delete,
updateOwnerName) — calco de los de Amiibo.

**Commit:** `feat(funko): persistencia Mongo de la colección funkos`

---

## Task 4 — Servicio CRUD + configuración (TDD)

**Archivos (crear):**
- `domain/port/in/FunkoUseCase.java` — `search`, `findById`, `save`, `update`, `delete`.
- `application/service/FunkoService.java` — calco de `AmiiboService` (`OwnerScopeResolver`, `OwnershipValidator`,
  `OwnerResolver`, unicidad `externalId`, `@Cacheable`/`@CacheEvict` de `funkoList`/`funkoDetail`,
  `copyUpdatableFields` con todos los campos).
- `application/exception/FunkoNotFoundException.java`, `application/exception/FunkoConflictException.java`.
- `infrastructure/config/StringToFunkoStatusConverter.java`.

**Archivos (modificar):**
- `domain/model/CollectionType.java` — `FUNKOS("funkos")`.
- `infrastructure/adapter/in/web/GlobalExceptionHandler.java` — `FunkoNotFoundException` → 404,
  `FunkoConflictException` → 409.
- `src/test/java/com/wikicollection/domain/model/UserPreferencesTest.java` — añadir `"funkos": true`.
- `src/test/java/com/wikicollection/infrastructure/config/StringToEnumConvertersNullSafeTest.java` — cubrir el converter nuevo.

**Test:** `FunkoServiceTest` (calco de `AmiiboServiceTest`): save (owner+userOwned), conflicto externalId,
DuplicateKey, update copia campos, ownership 403, externalId de otro, mismo externalId, delete, notFound,
search resuelve scope.

**Commit:** `feat(funko): servicio CRUD de funkos`

---

## Task 5 — API REST (TDD)

**Archivos (crear):**
- `infrastructure/adapter/in/web/dto/FunkoRequest.java` — record con `@NotBlank externalId/name`,
  `@NotNull status`.
- `.../dto/FunkoResponse.java` — + `withoutPrivate()` (quita `notes`).
- `.../dto/FunkoSearchResponse.java`, `.../dto/FunkoCategoryResponse.java`.
- `.../dto/FunkoDtoMapper.java` — `toDomain`, `toResponse`, `toSearchResponse`, `toCategoryResponse`.
- `infrastructure/adapter/in/web/FunkoController.java` — `/api/v1/funkos`: list (page/size/sort/name/status/owner),
  getById, create (201+Location), update, delete (204), `/search?name=`, `/categories`.

**Test:** `FunkoControllerTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`, `@MockitoBean FunkoRepository`,
`FunkoApiClient`, `UserRepository`, `OwnerResolver`; `CacheTestSupport.clearAll`): list vacío, filtros+owner,
getById 200/404, recorte de `notes` para otro usuario, create 201/400/409, update 200/403, delete 204,
search 200/400 (`name` ausente → 400), error externo 502, CORS.

**Commit:** `feat(funko): API REST de la colección funkos`

---

## Task 6 — Documentación y verificación

- `AGENTS.md` — sección de la colección Funko (endpoints, trampas de HobbyQuantHQ, cachés, colección).
- `mvn verify` verde (JaCoCo ≥ 0.80) y revisión del diff contra este plan.
- Abrir PR en `backend-collection` (`Closes #N`).

**Commit:** `docs(funko): trampas de HobbyQuantHQ y capa hexagonal`
