# Plan: Implementación backend — Colección de Amiibo

Fecha: 2026-10-13 · Rama: `feat/amiibo-collection` (desde `main` e836900) · PR: pendiente (Closes issue #353-style, a enlazar en el PR)

## Goal

Implementar el CRUD de la colección de **Amiibo** en `wiki-collection-backend` siguiendo
estrictamente el patrón de las colecciones existentes (books/board-games): cliente HTTP con
caché Caffeine, mapeo a dominio, persistencia Mongo, CRUD con auth + ownership, filtro
`owner` (`mine`/`other`/`all`) y búsqueda externa pública contra AmiiboAPI.

Criterio de terminado: `mvn verify` verde con JaCoCo LINE ≥ 0.80, todos los tests nuevos en
verde, y el API cumpliendo el contrato de `openspec/changes/feat-amiibo-collection/` (fusionado
en wiki-collection, PR #24).

## Spec de referencia

- `wiki-collection/openspec/changes/feat-amiibo-collection/` → `design.md` (modelo, puertos, API, DTOs) y `tasks.md` (tarea 2: backend).
- `wiki-collection/research/external-apis/amiiboapi.md` — contrato AmiiboAPI verificado en vivo el 2026-10-13.
- Referencia de código: stack completo de `Book` (externalId + conflict) y `BoardGame` (OWNED/WISHLIST + visibilidad), especialmente `BookService`, `BookController`, `BoardGamePersistenceAdapter`, `GoogleBooksClient` (rest client + retrys) y `BookControllerTest` (patrón de tests).

## Arquitectura

Hexagonal (reglas de `AGENTS.md` del repo): dependencias hacia dentro, `application/` nunca
importa `infrastructure/`. Paquetes nuevos bajo `com.wikicollection`:

- `domain/model`: `Amiibo`, `AmiiboStatus`, `AmiiboType`, `AmiiboReleaseDates`, `AmiiboSearchCriteria`, `AmiiboSearchResult`
- `domain/port/in`: `AmiiboUseCase`, `AmiiboSearchUseCase`
- `domain/port/out`: `AmiiboRepository`, `ExternalAmiiboCatalogClient`
- `application/service`: `AmiiboService`, `AmiiboSearchService`
- `application/exception`: `AmiiboNotFoundException`, `AmiiboConflictException`
- `infrastructure/adapter/out/amiibo`: `AmiiboApiClient` (+ DTOs JSON internos)
- `infrastructure/adapter/out/persistence`: `AmiiboEntity`, `SpringDataAmiiboRepository`, `AmiiboPersistenceAdapter`, `AmiiboEntityMapper`
- `infrastructure/adapter/in/web`: `AmiiboController` + `dto/AmiiboRequest`, `AmiiboResponse`, `AmiiboSearchResponse`, `AmiiboDtoMapper`
- `infrastructure/config`: `RestClientConfig` (+bean), `CacheConfig` (+3 cachés), `StringToAmiiboStatusConverter`, `GlobalExceptionHandler` (+409), `MongoIndexMigration` (+colección)

## Tech stack

Java 25 · Spring Boot 4.1.1 · Spring Data MongoDB · Lombok 1.18.46 (vía `maven-compiler-plugin` con
`annotationProcessorPaths`) · Caffeine · tests: JUnit 5 + Mockito + `MockRestServiceServer` +
MockMvc (`spring-boot-starter-webmvc-test`).

## Global Constraints (valores a no inventar)

- Base URL: `https://amiiboapi.org/api` — propiedad `amiiboapi.base-url=https://amiiboapi.org/api` (sin env var; AmiiboAPI no requiere key).
- Colección Mongo: **`amiibos`** (minúsculas como `books`/`board_games`; en los docs el nombre lógico es `AMIIBOS`).
- `AmiiboStatus`: `OWNED`, `WISHLIST`. `AmiiboType`: `CARD`, `FIGURE`, `BAND`, `YARN`, `BLOCK`.
- `externalId` = `head` + `tail` (16 hex, p. ej. `09d0030102bb0e02`).
- Estados de colección: **2**, sin precios ni barcode (decisión ratificada 2026-10-11).
- Imagen: `image` (PNG) de AmiiboAPI, **hotlink** (sin Catbox).
- Cachés Caffeine (en `CacheConfig`, TTL grupo `amiibos`): `amiiboSearch` (cliente, key `#query`), `amiiboList` (service, key `Objects.hash(#criteria, #pageable, #owner, #viewerId)`), `amiiboDetail` (service, key `#id`). `CACHE_NAMES` pasa de 40 a 43.
- Cliente: `RestClient` bean (`amiiboRestClient`) vía `HttpClientProperties.restClientBuilder(baseUrl)`; retrys: **4 intentos** (3 reintentos a 1s) en 5xx/timeout, `retryIntervalMillis` inyectable (0 ms en tests); **4xx NO se reintenta** (un 400 de `?id=` = id desconocido). Al fallar todo, **relanzar** (`RestClientResponseException` → 502 / `ResourceAccessException` → 503 por `GlobalExceptionHandler`); nunca lista vacía por error de red.
- AmiiboAPI no pagina. `200 {"amiibo": []}` = resultado vacío **válido** (no error). `{"amiibo": null}` también se tolera como vacío.
- `?id=` devuelve **objeto único** (`{"amiibo": {…}}`); búsqueda por `name` devuelve lista. DTOs JSON distintos para cada forma.
- **Todos** los records de deserialización del cliente llevan `@JsonIgnoreProperties(ignoreUnknown = true)` (lección Scryfall de AGENTS.md) y **un solo constructor**.
- Parámetros del catálogo exactos: `name`, `showusage` (enviar `showusage=true`, verificado equivalente a valueless), `id`. La URL canónica es `/api/amiibo/` (con slash; sin slash responde 308 y RestClient lo sigue — en tests se fija la URL que emite la implementación, ambos formatos OK).
- Búsqueda externa: `name` obligatorio; `IllegalArgumentException` si vacío (→ 400).
- `GET /api/v1/**` público; POST/PUT/DELETE con auth (`@CurrentUser`), ownership vía `OwnershipValidator`, 403 si ajeno. Admin exento.
- Unicidad `externalId`: `checkExternalIdUnique(externalId, currentId)` (patrón `BookService`) → `AmiiboConflictException`, y `save` envuelto en try/catch `DuplicateKeyException` → 409.
- Visibilidad: `ResponseVisibility.canSeePrivate(ownerId, CollectionType.AMIIBOS)` → `AmiiboResponse.withoutPrivate()` (quita `notes`).
- `CollectionType.AMIIBOS("amiibos")` (nuevo valor) → habilita owner filter, privacidad por colección, `isCollectionActive`, etc. (genérico por key).
- `MongoIndexMigration.OWNER_COLLECTIONS` += `"amiibos"`. Índices en entidad: `@Indexed` en `ownerId` y `externalId`.
- JaCoCo LINE ≥ 0.80 exigido por `mvn verify`; los tests nuevos tienen que aportar cobertura (los métodos de copia de campos del service se prueban).

## Review Focus (casos que el spec implica y el reviewer debe mirar)

1. **Case-sensitive + parámetros desconocidos:** AmiiboAPI ignora en silencio parámetros mal escritos (devolvería 950 resultados). El cliente solo construye `name`+`showusage`/`id`; el test del cliente fija la URL exacta. No se exponen filtros libres al usuario.
2. **Contenedor doble de `?id=`:** `{"amiibo":[...]}` vs `{"amiibo":{…}}`. DTOs separados: `AmiiboApiListResponse`/`AmiiboApiDetailResponse`. Un id desconocido da **400** (no lista vacía): `findById` lo traduce a `Optional.empty()` sin reintentar.
3. **Resultado vacío válido:** `{"amiibo":[]}` y `{"amiibo":null}` → lista vacía, nunca error.
4. **Mapeo tolerante del catálogo:** `type`, `release`, `character` pueden venir ausentes/nulos → `amiiboType`/`releaseDates` nulos sin romper. `externalId` (head+tail) y `name` siempre presentes.
5. **Visibilidad/privacidad por colección:** el filtro `owner` y el recorte de `notes` dependen de `CollectionType.AMIIBOS`; claves de caché de lista incluyen `viewerId` (un viewer distinto no comparte caché privada).
6. **Unicidad `externalId`:** 409 en POST/PUT cuando otro amiibo usa el mismo externalId (salvo el propio en PUT).

---

## Task 1 — Cliente externo AmiiboAPI (TDD)

**Contexto:** primeras piezas del flujo de búsqueda externa: tipos de dominio que el cliente
produces y la puerta de salida. Incluye todo el scaffold de configuración que el cliente necesita
(bean RestClient, propiedades, caché `amiiboSearch`) para no tocar `CacheConfig` dos veces.

**Desviación vs design:** `AmiiboSearchUseCase.findById` (del diseño) **no** se implementa a nivel
de use-case: el cliente expone `findById` (puerto out) porque el criterio de aceptación exige
tolerar el contenedor de objeto único, pero **no hay endpoint REST** de detalle externo (la tabla
REST del diseño no lo incluye; DA7 = mínimo viable). `ExternalAmiiboCatalogClient` sí lo lleva.

**Archivos (crear):**
- `domain/model/AmiiboType.java` — enum `CARD, FIGURE, BAND, YARN, BLOCK`.
- `domain/model/AmiiboReleaseDates.java` — record `(LocalDate au, LocalDate eu, LocalDate jp, LocalDate na)`.
- `domain/model/AmiiboSearchResult.java` — record:
  `(String externalId, String name, String character, String amiiboSeries, String gameSeries, AmiiboType amiiboType, AmiiboReleaseDates releaseDates, String imageUrl, List<String> compatibleGames, String externalSource)`.
- `domain/port/out/ExternalAmiiboCatalogClient.java`:
  ```java
  List<AmiiboSearchResult> searchByName(String query);
  Optional<AmiiboSearchResult> findById(String externalId);
  ```
- `infrastructure/adapter/out/amiibo/AmiiboApiClient.java` — `@Component`, implementa el puerto.
  - DTOs anidados **con** `@JsonIgnoreProperties(ignoreUnknown = true)` y un solo constructor:
    - `AmiiboApiListResponse(List<AmiiboApiItem> amiibo)`
    - `AmiiboApiDetailResponse(AmiiboApiItem amiibo)`
    - `AmiiboApiItem(String head, String tail, String name, String character, String amiiboSeries, String gameSeries, String type, String image, AmiiboApiRelease release, List<AmiiboApiGame> games3DS, List<AmiiboApiGame> gamesWiiU, List<AmiiboApiGame> gamesSwitch, List<AmiiboApiGame> gamesSwitch2)`
    - `AmiiboApiRelease(String au, String eu, String jp, String na)`
    - `AmiiboApiGame(String gameName)`
  - Constructores: `public AmiiboApiClient(RestClient amiiboRestClient)` ← `this(amiiboRestClient, 1000L)`; y package-private `AmiiboApiClient(RestClient, long retryIntervalMillis)` (patrón `GoogleBooksClient`).
  - `searchByName`: `@Cacheable(cacheNames = CacheConfig.AMIIBO_SEARCH, key = "#query")`; GET `path("/amiibo/")`, `queryParam("name", query)`, `queryParam("showusage", "true")`; `withRetry(...)`; `mapItems`: null/lista vacía → `List.of()`.
  - `findById`: GET `path("/amiibo/")`, `queryParam("id", externalId)`; `withRetry(...)`; catch `RestClientResponseException` **4xx → `Optional.empty()`** (rethrow el resto); item null → empty.
  - `toSearchResult(item)`: `externalId = head + tail`; `amiiboType = toType(type)` (Card→CARD…, desconocido/null→null); `releaseDates` → `AmiiboReleaseDates` (parse ISO; parsear tolerando null); `imageUrl = image`; `compatibleGames` = `gameName` de las 4 listas, filtrados no-blank, `distinct`, en orden; `externalSource = "amiiboapi"`.
  - `withRetry(Supplier<T>)`: 4 intentos; captura `RestClientResponseException` (reintenta solo **5xx**) y `ResourceAccessException`; entre intentos `Thread.sleep(retryIntervalMillis)`; al agotar relanza. (La condición de 5xx se comprueba con `e.getStatusCode().is5xxServerError()`; 4xx se relanza al toque — el catch de `findById` lo traduce después.)

**Archivos (modificar):**
- `infrastructure/config/RestClientConfig.java` — bean:
  ```java
  @Bean
  public RestClient amiiboRestClient(@Value("${amiiboapi.base-url:https://amiiboapi.org/api}") String baseUrl) {
      return httpClientProperties.restClientBuilder(baseUrl).build();
  }
  ```
- `resources/application.properties` — `amiiboapi.base-url=https://amiiboapi.org/api` y `app.cache.ttl.amiibos=3600s` (junto a los ttl de las demás colecciones).
- `infrastructure/config/CacheConfig.java` — constantes `AMIIBO_SEARCH = "amiiboSearch"`, `AMIIBO_LIST = "amiiboList"`, `AMIIBO_DETAIL = "amiiboDetail"`; añadirlas a `CACHE_NAMES` (search junto a los search, list junto a los list, detail junto a los detail → 43) y `PROPERTY_KEYS` (las 3 → `"amiibos"`).

**Test (crear):** `src/test/java/com/wikicollection/infrastructure/adapter/out/amiibo/AmiiboApiClientTest.java`
Usa `MockRestServiceServer.bindTo(RestClient.builder())` con el constructor package-private `(restClient, 0L)`. JSON fixtures en `resources` o strings.

| Test | Assert clave |
|---|---|
| `searchByName_requestsExactUrlAndParams` | matcher `requestTo("https://amiiboapi.org/api/amiibo?name=mario&showusage=true")` (ajustar si la implementación emite el trailing slash; ambos verificados en vivo); `withSuccess` listado |
| `searchByName_mapsAllFields` | externalId=head+tail, name, character, amiiboSeries, gameSeries, amiiboType=FIGURE, releaseDates{au,eu,jp,na}, imageUrl, compatibleGames aplanado+distinct, externalSource="amiiboapi" |
| `searchByName_mapsUnknownTypeToNull` | `type":"Sparkle"` → amiiboType null (no rompe) |
| `searchByName_returnsEmpty_when200EmptyList` | `{"amiibo":[]}` → lista vacía |
| `searchByName_returnsEmpty_whenNullContainer` | `{"amiibo":null}` → lista vacía |
| `searchByName_retries5xx_4Attempts_thenRethrows` | `withServerError()` ×4 (`ExpectedCount.times(4)`), luego `assertThatThrownBy(...).isInstanceOf(RestClientResponseException.class)` |
| `searchByName_propagatesTimeout` | `withException(new ResourceAccessException(...))` ×4 → `ResourceAccessException` |
| `findById_mapsSingleObjectContainer` | `{"amiibo":{...}}` (objeto) → `Optional` con el resultado mapeado |
| `findById_returnsEmpty_on400` | `withStatus(HttpStatus.BAD_REQUEST)` **×1** (no reintenta 4xx) → `Optional.empty()` |
| `findById_retries5xx_thenRethrows` | 500 ×4 → `RestClientResponseException` |

**Pasos:** escribir un test con el JSON de AmiiboAPI real (capturado en vivo) → verlo fallar (cliente no existe) → implementar el cliente + config → `./mvnw -q test -Dtest=AmiiboApiClientTest` verde → commit `feat(amiibo): AmiiboAPI client externo con caché y retrys`.

---

## Task 2 — Capa de aplicación de búsqueda (TDD)

**Contexto:** el use-case de búsqueda externa, idéntico al patrón `BoardGameSearchService`
(`PagedResults.slice`), con `name` obligatorio.

**Archivos (crear):**
- `domain/port/in/AmiiboSearchUseCase.java`:
  ```java
  Page<AmiiboSearchResult> search(String query, Pageable pageable);
  ```
- `application/service/AmiiboSearchService.java` — `@Service`, implementa el use-case; inyecta
  `ExternalAmiiboCatalogClient`; `query` null/blank → `IllegalArgumentException("El parámetro de búsqueda 'name' es obligatorio")`; `return PagedResults.slice(client.searchByName(query), pageable);`. Sin `@Cacheable` (la caché está en el cliente, Task 1).

**Test (crear):** `src/test/java/com/wikicollection/application/service/AmiiboSearchServiceTest.java`
Mock del cliente.

| Test | Assert |
|---|---|
| `search_rejectsBlankQuery` | `""`/`null` → `IllegalArgumentException` |
| `search_slicesClientResults` | 25 resultados, PageRequest(0,10) → 10 content, totalElements=25 |
| `search_returnsEmptyPage_whenClientEmpty` | cliente → `List.of()` → empty page |

**Pasos:** test → rojo → service → verde → commit `feat(amiibo): servicio de búsqueda externa`.

---

## Task 3 — Persistencia y dominio CRUD (TDD)

**Contexto:** entidad `Amiibo`, puerto `AmiiboRepository` y adaptador Mongo (patrón
`BoardGamePersistenceAdapter`). Las fechas de lanzamiento se **aplanan** en 4 campos
`LocalDate` en la entidad (cero riesgo con records anidados en Mongo; el mapper reconstruye el
record de dominio). Colección `amiibos`.

**Archivos (crear):**
- `domain/model/AmiiboStatus.java` — enum `OWNED, WISHLIST`.
- `domain/model/Amiibo.java` — Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @ToString`:
  `id, externalId, name, character, amiiboSeries, gameSeries, amiiboType (AmiiboType), releaseDates (AmiiboReleaseDates), imageUrl, compatibleGames (List<String>), status (AmiiboStatus), notes, dateAdded (LocalDate), ownerId (String), userOwned (UserOwned)`.
- `domain/model/AmiiboSearchCriteria.java` — record `(String name, AmiiboStatus status, String ownerId, List<String> excludeOwnerIds)` + `hasName()`, `hasStatus()`, `hasOwnerId()`, `hasExcludeOwnerIds()` (igual que `BoardGameSearchCriteria`).
- `domain/port/out/AmiiboRepository.java`:
  ```java
  Page<Amiibo> search(AmiiboSearchCriteria criteria, Pageable pageable);
  Optional<Amiibo> findById(String id);
  Optional<Amiibo> findByExternalId(String externalId);
  Amiibo save(Amiibo amiibo);
  void deleteById(String id);
  void updateOwnerName(String ownerId, String ownerName);
  ```
- `application/exception/AmiiboNotFoundException.java` — (`peso` mensaje "Amiibo no encontrado con id: " + id).
- `infrastructure/adapter/out/persistence/AmiiboEntity.java` — `@Document(collection = "amiibos")`:
  `@Id String id; @Indexed String ownerId; UserOwnedEntity userOwned; @Indexed String externalId; String name; String character; String amiiboSeries; String gameSeries; AmiiboType amiiboType; LocalDate releaseDateAu, releaseDateEu, releaseDateJp, releaseDateNa; String imageUrl; List<String> compatibleGames; AmiiboStatus status; String notes; LocalDate dateAdded;`
- `infrastructure/adapter/out/persistence/SpringDataAmiiboRepository.java` — `interface ... extends MongoRepository<AmiiboEntity, String>` (+ `Optional<AmiiboEntity> findByExternalId(String externalId);`).
- `infrastructure/adapter/out/persistence/AmiiboEntityMapper.java` — `@Component`; `toDomain(AmiiboEntity)` / `toEntity(Amiibo)`: mapea releaseDates record ↔ 4 campos; userOwned↔`UserOwnedEntity` (mirror de `BoardGameEntityMapper`); id no se copia hacia entidad en save (Spring Data lo gestiona).
- `infrastructure/adapter/out/persistence/AmiiboPersistenceAdapter.java` — `@Component` implementa el puerto; inyecta `SpringDataAmiiboRepository` + `MongoTemplate`. `search`: construir `Query` con `Criteria` (mirror del `BoardGamePersistenceAdapter`): `name` → `regex(Pattern.quote(name), "i")`; `status` → `is`; `ownerId` → `is`; `excludeOwnerIds` → `nin`; a la punta `skip/limit/with(Sort)` desde `Pageable`, contar con el mismo query (o `PageableExecutionUtils`); `findByExternalId`/`findById`/`save`/`deleteById` delegan en SpringData; `updateOwnerName` → `mongoTemplate.updateMulti(Query.where("ownerId").is(ownerId), Update().set("userOwned.ownerName", ownerName), AmiiboEntity.class)`.
- `infrastructure/config/MongoIndexMigration.java` — **modificar** `OWNER_COLLECTIONS` añadiendo `"amiibos"`.

**Tests (crear):**
- `AmiiboEntityMapperTest` (JUnit, sin Spring): round-trip dominio↔entidad: todos los campos, releaseDates aplanado (au/eu/jp/na), lista `compatibleGames`, enums, userOwned; y `toEntity` con releaseDates null → 4 campos null.
- `AmiiboPersistenceAdapterTest` (`@ExtendWith(MockitoExtension.class)`, mocks de SpringData repo + MongoTemplate, mirror de `BoardGamePersistenceAdapterTest`):
  - `search_combinesFiltersAndPagination` — con name+status+ownerId+excludeOwnerIds: capturar `Query` y verificar criterios (regex ci, is, nin) y `skip/limit`.
  - `search_withoutFilters_returnsAllPaged`.
  - `save_delegatesAndReturnsDomain`.
  - `findById`/`findByExternalId` presentes/ausentes.
  - `deleteById_delegates`.
  - `updateOwnerName_updatesNestedField` — capturar `Update` y verificar `userOwned.ownerName`.

**Pasos:** test mapper → rojo → mapper → verde → test adapter → rojo → adapter → verde →
`./mvnw -q test -Dtest=AmiiboEntityMapperTest,AmiiboPersistenceAdapterTest` → commit
`feat(amiibo): persistencia Mongo (colección amiibos)`.

---

## Task 4 — `AmiiboService` CRUD (TDD)

**Contexto:** service de aplicación con CRUD, ownership, unicidad externalId (patrón `BookService`
con `checkExternalIdUnique` + `saveOrConflict`) y cachés `amiiboList`/`amiiboDetail`.

**Archivos (crear/modificar):**
- `application/exception/AmiiboConflictException.java` — `RuntimeException` (mensaje "Ya existe un amiibo con externalId: " + externalId).
- `domain/model/CollectionType.java` — **modificar**: añadir `AMIIBOS("amiibos")`.
- `domain/port/in/AmiiboUseCase.java`:
  ```java
  Page<Amiibo> search(AmiiboSearchCriteria criteria, Pageable pageable, String owner, String viewerId);
  Amiibo findById(String id);
  Amiibo save(Amiibo amiibo, String ownerId);
  Amiibo update(String id, Amiibo updates, String userId);
  void delete(String id, String userId);
  ```
  *(Desviación vs design: el use-case lleva `owner`+`viewerId` como el resto de colecciones; el patrón del código manda.)*
- `application/service/AmiiboService.java` — `@Service`, inyecta `AmiiboRepository`, `OwnershipValidator`, `OwnerScopeResolver`, `OwnerResolver`:
  - `search` `@Cacheable(AMIIBO_LIST, key = "T(java.util.Objects).hash(#criteria, #pageable, #owner, #viewerId)")`; `OwnerScopeResolver.Scope scope = ownerScopeResolver.resolve(CollectionType.AMIIBOS, owner, viewerId);` → `repository.search(new AmiiboSearchCriteria(criteria.name(), criteria.status(), scope.ownerId(), scope.excludeOwnerIds()), pageable)`.
  - `findById` `@Cacheable(AMIIBO_DETAIL, key = "#id")` → `orElseThrow(AmiiboNotFoundException)`.
  - `save` `@Transactional` `@CacheEvict(AMIIBO_LIST, allEntries = true)`: setOwnerId(ownerId), setUserOwned(ownerResolver.resolveOwner(ownerId)), `checkExternalIdUnique(externalId, null)`, `saveOrConflict`.
  - `update` `@Transactional` `@CacheEvict({AMIIBO_DETAIL, AMIIBO_LIST}, allEntries = true)`: findById → `ownershipValidator.validateOwner(existing.getOwnerId(), userId)` → `checkExternalIdUnique(updates.getExternalId(), id)` → `copyUpdatableFields(existing, updates)` → saveOrConflict. **El método de copia copia TODOS los campos editables salvo id/ownerId/userOwned** (implementación completa garantiza cobertura).
  - `delete` `@CacheEvict({AMIIBO_DETAIL, AMIIBO_LIST}, allEntries = true)`: findById → validateOwner → `deleteById(id)`.
  - `checkExternalIdUnique(externalId, currentId)`: si blank → return; `repository.findByExternalId(externalId)` si presente y `currentId` no coincide → `AmiiboConflictException`.
  - `saveOrConflict`: try `repository.save` / catch `DuplicateKeyException` → `AmiiboConflictException`.

**Test (crear):** `src/test/java/com/wikicollection/application/service/AmiiboServiceTest.java`
`@ExtendWith(MockitoExtension.class)`, mocks de `AmiiboRepository`, `OwnershipValidator`, `OwnerScopeResolver`, `OwnerResolver`.

| Test | Assert |
|---|---|
| `save_setsOwnerAndUserOwned` | repo.save recibe amiibo con ownerId="u1" y userOwned resuelto |
| `save_throwsConflict_whenExternalIdExists` | `findByExternalId` → Optional.of(otro) → `AmiiboConflictException` |
| `save_throwsConflict_onDuplicateKey` | repo.save lanza `DuplicateKeyException` → `AmiiboConflictException` |
| `update_copiesAllUpdatableFields_andSaves` | verificamos repo.save con los campos copiados (incl. status, notes, releaseDates, compatibleGames) |
| `update_validatesOwnership` | `validateOwner` lanza Forbidden → propaga (verificar llamada con el ownerId del existente) |
| `update_throwsConflict_whenExternalIdUsedByOther` | `findByExternalId` → otro id ≠ currentId → conflict |
| `update_allowsSameExternalId` | `findByExternalId` → el propio (id == currentId) → no conflict |
| `delete_validatesOwnershipAndDeletes` | validateOwner(ownerId, userId) + `deleteById(id)` |
| `findById_throwsNotFound` | repo vacío → `AmiiboNotFoundException` |
| `search_resolvesOwnerScope` | OwnerScopeResolver.Scope("u1", [excluidos]) → repo.search recibe criteria con ownerId/excludeOwnerIds |

**Pasos:** test → rojo → service (+CollectionType+exception) → verde →
`./mvnw -q test -Dtest=AmiiboServiceTest` → commit `feat(amiibo): AmiiboService CRUD con ownership y unicidad`.

---

## Task 5 — Web: controller + DTOs (TDD)

**Contexto:** el API REST según la tabla del design bajo `/api/v1/amiibos`, con `@CurrentUser`
para viewerId (patrón `BookController`), `ResponseVisibility` para el recorte de `notes`, y el
converter para `?status=`.

**Archivos (crear):**
- `infrastructure/adapter/in/web/dto/AmiiboRequest.java` — record con validación:
  `@NotBlank(message = "El externalId es obligatorio") String externalId`, `@NotBlank(message = "El nombre es obligatorio") String name`, `String character, amiiboSeries, gameSeries`, `AmiiboType amiiboType`, `AmiiboReleaseDates releaseDates`, `String imageUrl`, `List<String> compatibleGames`, `@NotNull(message = "El estado es obligatorio") AmiiboStatus status`, `String notes`, `LocalDate dateAdded`.
- `infrastructure/adapter/in/web/dto/AmiiboResponse.java` — record (id, externalId, name, character, amiiboSeries, gameSeries, amiiboType, releaseDates, imageUrl, compatibleGames, status, notes, dateAdded, `UserOwnedResponse userOwned`) + método `withoutPrivate()` que devuelve el mismo record con `notes` a null (patrón `BoardGameResponse`).
- `infrastructure/adapter/in/web/dto/AmiiboSearchResponse.java` — record (externalId, name, character, amiiboSeries, gameSeries, amiiboType, releaseDates, imageUrl, compatibleGames, externalSource).
- `infrastructure/adapter/in/web/dto/AmiiboDtoMapper.java` — `@Component`: `toDomain(AmiiboRequest)` (ignora id/owner), `toResponse(Amiibo)` (mapea todo incl. userOwned→UserOwnedResponse), `toSearchResponse(AmiiboSearchResult)` (igual salvo externalSource).
- `infrastructure/config/StringToAmiiboStatusConverter.java` — `@Component implements Converter<String, AmiiboStatus>` (patrón `StringToBoardGameStatusConverter`: blank → null, `valueOf(source.trim().toUpperCase())`).
- `infrastructure/adapter/in/web/AmiiboController.java` — `@RestController @RequestMapping("/api/v1/amiibos")`, mirro `BookController`:
  - `GET /` → `list(page, size, sort, name?, status?, owner="mine", @CurrentUser String viewerId)` → `Page<AmiiboResponse>` con `visibility.canSeePrivate(ownerId, AMIIBOS)` ? response : `withoutPrivate()`. `buildSort` default `name,asc`.
  - `GET /{id}` → `getById` (misma lógica de visibilidad).
  - `POST /` → `create(@Valid @RequestBody AmiiboRequest, @CurrentUser, UriComponentsBuilder)` → 201 + Location `/api/v1/amiibos/{id}`.
  - `PUT /{id}` → `update`.
  - `DELETE /{id}` → 204.
  - `GET /search?name=` → `search(name?, page, size=10)`: `name` blank → `IllegalArgumentException("Indica 'name' para buscar")`; → `Page<AmiiboSearchResponse>`.
- `infrastructure/adapter/in/web/GlobalExceptionHandler.java` — **modificar**: `@ExceptionHandler(AmiiboConflictException.class)` → 409 (patrón `handleMovieShowConflict`).

**Test (crear):** `src/test/java/com/wikicollection/infrastructure/adapter/in/web/AmiiboControllerTest.java`
`@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false"})` (mirror
`BookControllerTest`: `@AutoConfigureMockMvc`, `@MockitoBean` de `SpringDataAmiiboRepository`,
`AmiiboApiClient`, `OwnerResolver` y `UserPreferencesRepository`; `user("u1")` para autenticar;
limpiar cachés con `CacheTestSupport.clearAll` donde toque, igual que `BookControllerTest`).

| Test | Assert |
|---|---|
| `list_returnsEmptyPage` | GET `/api/v1/amiibos` .with(user("u1")) → 200, content vacío, totalElements 0 |
| `list_filtersByNameAndStatus_andOwner` | repo.search recibe criteria con name/status/ownerId resueltos (verificar con captor) |
| `getById_returnsAmiibo` | 200 + jsonPath `$.externalId`, `$.status`, `$.userOwned.ownerName` |
| `getById_stripsPrivateNotes_forOtherUser` | viewer ajeno con colección privada → `$.notes` ausente |
| `create_returns201_withLocation` | POST `{externalId, name, status}` .with(user("u1")) → 201, Location `/api/v1/amiibos/{id}` |
| `create_returns400_whenNameMissing` | POST sin `name`/`externalId`/`status` → 400 |
| `create_returns409_whenExternalIdExists` | `findByExternalId` → existente → 409 |
| `update_returns200` | repo.findById → existente de u1; PUT → 200 campos actualizados |
| `update_returns403_forOtherOwner` | findById → ownerId="u2", user u1 → 403 |
| `delete_returns204` | DELETE → 204 |
| `search_returnsResults` | `AmiiboApiClient.searchByName` → resultados; GET `/search?name=mario` → 200, `$.content[0].externalId` |
| `search_returns400_whenNameMissing` | GET `/search` sin name → 400 |
| `search_propagatesExternalError_as502` | cliente lanza `RestClientResponseException` → 502 |
| `cors_preflightAllowed` | OPTIONS `/api/v1/amiibos` con `Origin: http://localhost:5173` → 200 + `Access-Control-Allow-Origin` |

**Pasos:** test (puede ir en 2-3 tandas: lectura/lista → escritura → búsqueda) → rojo →
controller+DTOs+converter+handler → verde →
`./mvnw -q test -Dtest=AmiiboControllerTest` → commit `feat(amiibo): API REST /api/v1/amiibos`.

---

## Task 6 — Cierre: verify completo + docs

**Contexto:** gate final y documentación del repo de código.

**Pasos:**
1. `./mvnw verify` completo — verde (todos los tests + JaCoCo LINE ≥ 0.80; los tests de la Task 4
   ya cubren el service; si JaCoCo se queja, añadir asserts, no `excludes`).
2. Actualizar `AGENTS.md` (repo backend): sección hexagonal con `adapter/out/amiibo`,
   `AmiiboController` y `StringToAmiiboStatusConverter`; nota con las trampas verificadas de
   AmiiboAPI (case-sensitive, `?id=` objeto único, 400 = id desconocido, 200 vacío válido, sin
   paginación, 4 intentos y 4xx sin reintento) — mismo estilo que la sección Scryfall.
3. PR contra `main` con `Closes #…` (issue del repo backend de la colección; si no existe issue,
   enlazar el PR del wiki #24 como referencia).
4. Post-PR (fuera de este plan): aplicar specs canónicas en wiki-collection (controllers 11→12,
   cachés 40→43, colección AMIIBOS) y el frontend.

---

## Self-Review (a hacer en el último commit)

- [ ] ¿`application/` importa algo de `infrastructure/`? (no: `PagedResults` y `CollectionType` siguen igual)
- [ ] ¿Todos los records JSON del cliente llevan `@JsonIgnoreProperties(ignoreUnknown = true)` y un solo constructor?
- [ ] ¿`withRetry` reintenta solo 5xx/timeout (4 intentos) y 4xx se traduce en `findById` a empty?
- [ ] ¿`{"amiibo":[]}`/`{"amiibo":null}` → vacío, y 5xx/timeout → exception (502/503)?
- [ ] ¿Claves de caché de lista incluyen viewerId? ¿evicts en save/update/delete?
- [ ] ¿`CollectionType.AMIIBOS` añadido y `MongoIndexMigration.OWNER_COLLECTIONS` extendido?
- [ ] ¿`mvn verify` verde con cobertura ≥ 0.80 sin exclusions nuevas?