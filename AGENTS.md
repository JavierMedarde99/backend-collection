# AGENTS.md

Spring Boot 4.1.1 backend (`wiki-collection-backend`) in **hexagonal architecture**, using Java 25 + MongoDB. Existing `README.md` is a stub; this file is the source of truth for contributors.

## Commands

- Build, test, coverage gate: `mvn verify` (NOT just `test`)
  - `verify` runs tests and then enforces the JaCoCo rule: **LINE coverage ≥ 0.80**, or the build fails.
- CI runs `./mvnw clean package` (see `.github/workflows/checks.yml`).
- Run app: `mvn spring-boot:run`
- No lint or formatter is configured.

## Critical gotchas

- **JDK 25 + Lombok:** javac does not auto-discover Lombok. The `maven-compiler-plugin` enforces it via `<annotationProcessorPaths>` (lombok 1.18.46). If you add/change Lombok usage, keep that config; if the build stops recognizing `@Getter`/`@Builder`/etc., this is the cause.
- **MongoDB property:** the correct key is `spring.mongodb.uri` (NOT `spring.data.mongodb.uri`). A past commit (98a2af28) fixed exactly this rename.
- **MongoDB creds:** the URI is `mongodb://localhost:27017/wiki-collection` by default. Do NOT hardcode real credentials in `application.properties`; set `SPRING_MONGODB_URI` env var instead.
- **Tests need no live Mongo:** `BookControllerTest` is `@SpringBootTest` with `@MockitoBean`-mocked `SpringDataBookRepository` and `GoogleBooksClient`, plus `spring.data.mongodb.auto-index-creation=false`. It does not require a running MongoDB.
- **Google Books search retries:** on a 5xx/timeout, `GoogleBooksClient` retries **3 more times at 1s intervals** (4 total attempts) and **rethrows the error** (`RestClientResponseException`/`ResourceAccessException`) if all fail; it only returns an empty list when the response has no items. A 200 with empty items is a valid empty result, not a failure. If you change the retry count/interval, keep the `GoogleBooksClientTest` in sync (it constructs the client with a 0 ms interval and verifies 4 attempts).

## Testing quirks (Spring Boot 4)

- Use `spring-boot-starter-webmvc-test` (not the old `spring-boot-starter-test` mockmvc path).
- Imports that look "wrong" are correct for Boot 4:
  - `@AutoConfigureMockMvc` → `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`
  - `@MockitoBean` → `org.springframework.test.context.bean.override.mockito.MockitoBean`
- `BookControllerTest` verifies HTTP contracts via `MockMvc` (status, `$.*` JSON, CORS).

## Hexagonal layout

Dependencies point inward only; no cross-layer imports outward:

- `domain/` — plain POJOs (`Book`, `BookState`, `BookType`, `BookSearchResult`) and ports:
  - `port/in`: `BookUseCase`, `BookSearchUseCase`
  - `port/out`: `BookRepository`, `ExternalBookCatalogClient`, `ExternalBookEnricher`
- `application/` — services (`BookService`, `BookSearchService`, `DeckService`, `DeckImportWorker`, `DeckImportService`), `DeckListParserRegistry`, `DeckImportFormatDetector` and exceptions (`BookNotFoundException`, etc.)
  - `DeckListParserRegistry` and `DeckImportFormatDetector` live **here**, not in `infrastructure`, because `DeckImportWorker` and `DeckImportService` (both `application/` beans) consume them; putting them under `adapter/in` would make `application` import `infrastructure`.
- `infrastructure/` — adapters + config:
  - `adapter/in/web`: `BookController`, `AmiiboController`, `DeckController`, `GlobalExceptionHandler`, DTOs
  - `adapter/out/persistence`: `BookEntity`, `SpringDataBookRepository`, `BookEntityMapper`, `BookPersistenceAdapter` (Mongo collection `BOOKS`)
  - `adapter/out/google`: `GoogleBooksClient` (search via external catalog)
  - `adapter/out/openlibrary`: `OpenLibraryClient` (rellena portada/descripción por ISBN cuando Google no las trae)
  - `adapter/out/amiibo`: `AmiiboApiClient` (búsqueda externa contra AmiiboAPI)
  - `config`: `WebConfig` (CORS for `http://localhost:5173`), `MongoAuditConfig`, `StringToBookStateConverter`, `StringToAmiiboStatusConverter`, `RestClientConfig`

There is intentionally **only one** `@EnableMongoAuditing` (in `MongoAuditConfig`). Don't add a second one (e.g., on the main application class) — it causes `BeanDefinitionOverrideException` and fails every controller test.

## Domain model (`Book` — Mongo collection `BOOKS`)

`id`, `externalId`, `title`, `descripcion`, `author` (singular String), `pages`, `type`, `state`, `comment`, `start` (Integer 0–5), `startDate` (LocalDate), `endDate` (LocalDate), `frontpage`. Enums: `BookType` (MANGA, NOVEL, GRAPHIC_NOVEL), `BookState` (TO_READ, READING, COMPLETED). Requests validate `start` 0–5.

Search (`GET /api/books/search`) uses query param **`name`** (not `q`) and hits Google Books with `intitle:<name>` + `langRestrict=es` + `startIndex`/`maxResults`. La búsqueda por **ISBN** (`isbn=`) **no** envía `langRestrict`: un ISBN ya identifica una edición y Google descarta en silencio los volúmenes cuyo idioma no está etiquetado (devuelve 0 resultados). Además, el buscador de Google **no siempre indexa el ISBN** de ediciones sin vista previa (`q=isbn:` responde 0 resultados aunque el volumen exista: p. ej. `9788466356718` → volumen `LCPqzgEACAAJ`, verificado 2026-10-11), así que `GoogleBooksClient.searchByIsbn` tiene un **respaldo**: si `q=isbn:` vuelve vacío, resuelve el ISBN a un id con la **Dynamic Links API** (`https://books.google.com/books?jscmd=viewapi&bibkeys=ISBN:<isbn>`, acepta ISBN-10/13 y no consume cuota) y luego pide `GET /v1/volumes/{id}`. Y como esas ediciones sin vista previa suelen traer **solo metadatos** (sin `imageLinks` ni `description`), `BookSearchService.searchByIsbn` enriquece cada resultado con **Open Library** (`OpenLibraryClient`, puerto `ExternalBookEnricher`, caché `bookEnrich` claveada por ISBN): `GET /isbn/{isbn}.json` da la portada (`covers[0]` → `https://covers.openlibrary.org/b/id/{id}-M.jpg`) y los `works`; la descripción se pide a `GET /works/{key}.json` (`description` puede venir como string o como `{type, value}`). Es **best-effort**: un fallo (404/5xx/timeout) devuelve el resultado original y no se llama ninguna API si la portada **y** la descripción ya vienen de Google. Google devuelve **10 por defecto** y **40 como máximo** por llamada, así que `GoogleBooksClient` pagina con `startIndex` en ventanas de 40 y usa `totalItems` como `totalElements` de la `Page` (por eso la búsqueda puede devolver más de 10 y el cliente puede pedir páginas >0). `GET /api/books` filters by `state`; there is no `tag` filter (tags were removed with the BOOKS model).

## Scryfall: impresiones de carta (issue #451)

`MagicCard` guarda **una** impresión. Scryfall distingue la impresión (`id`) de la carta a
través de todas sus reimpresiones (`oracle_id`), y una carta puede tener cientos: sin poder
elegir, el usuario guarda la que Scryfall devolvió primero.

`GET /api/v1/magic/scryfall/{scryfallId}/printings?page=N` (base 0, público como el resto de
`GET /api/v1/**`).

**Fluent only**: `page` es base 0, `size` no existe. Scryfall **ignora** `page_size`,
`per_page` y `limit`: una página trae siempre **175** elementos (medido sobre
`is:commander`, 12 760 impresiones, con y sin `page_size=1`). La constante vive en
`MagicCardPrinting.PAGE_SIZE` — en el dominio, no en el mapper, porque `application/` no
importa `infrastructure/` (solo lo hace `MovieShowService`, y es una excepción, no una norma).

El endpoint recibe un id de **impresión** pero Scryfall solo pagina por `oracle_id`, así que
`MagicCardService.resolveOracleId()` llama primero a `findById()`. Sin ese paso, la búsqueda
devolvería siempre 0 resultados. Un `oracle_id` ausente produce una página vacía explícita:
`q=oracleid:` sin valor devuelve 200 con 0 resultados, indistinguible de "no hay reimpresiones".

### Restricciones de la API de Scryfall (verificadas 2026-10-02)

| Restricción | Detalle |
|-------------|---------|
| Página fija de 175 | `page_size`, `per_page` y `limit` se ignoran en silencio |
| `page` base 1 | Nuestra API es base 0: el cliente suma 1 al pedir |
| `dir`, no `direction=` | `direction=asc` se ignora en silencio; `dir=asc` funciona |
| `order=released` | Ya sale descendente, sin necesidad de `dir` |
| `unique=prints` | Una fila por reimpresión física (`art` = 389, `set` = 1, `none` = 1) |
| El total es `total_cards` | No hay `total` ni `count` |
| **404, no lista vacía** | `/cards/search` devuelve **404** cuando la consulta no tiene coincidencias (verificado con `curl`: `q=name:"Sol Ringg"` → 404, `q=name:"Sol Ring"` → 200). `searchByNameExact`/`searchSuggestions` lo traducen en lista vacía y `executeWithRetry` no reintenta en 4xx; un 404 tratado como error tumbaría cualquier importación con una carta que Scryfall no conoce |

Cuidado con los parámetros mal escritos: Scryfall devuelve **200 con 0 resultados**, así que
un `direction=` o un `q=` mal construido no falla, parece un catálogo vacío.

### Trampas de Jackson en los records de Scryfall

Los records de `MagicCardMapper` llevan `@JsonIgnoreProperties(ignoreUnknown = true)`
**todos**, no solo `ScryfallCardResponse`. Scryfall manda `object`, `has_more`, `next_page`,
`warnings` y, dentro de `prices`, `usd_foil` / `usd_etched`. Sin ignorar los desconocidos,
su deserialización depende de que la configuración global de Jackson traiga
`FAIL_ON_UNKNOWN_PROPERTIES=false`.

**No añadas un segundo constructor a estos records.** Con dos constructores y ningún
`@JsonCreator`, Jackson elige entre ellos según su configuración: con el `ObjectMapper` de
Spring se perdía `total_cards` y el total salía del tamaño de la página. Los tests los
construyen desde JSON real por eso, no con el constructor posicional.

### `PageImpl` recorta el total (y por qué no es un bug aquí)

Spring Data `PageImpl` reescribe el total a `offset + content.size()` **solo si la página
trae contenido** y `offset + pageSize > total`:

- página 0 de 955 → no recorta, `totalElements=955`, `totalPages=6`;
- última página parcial (200 totales, página 1 con 25) → recorta a `175 + 25 = 200`, correcto;
- página vacía posterior a la última → **no** recorta, conserva el total real.

Al escribir fixtures, `total_cards` tiene que ser coherente con los elementos devueltos: un
`total_cards: 10` con 2 elementos no es una respuesta de Scryfall y hace fallar los tests por
el recorte, no por un fallo del mapper.

## Amiibo: catálogo externo (AmiiboAPI)

La colección Amiibo (Mongo `amiibos`) sigue el patrón de board-games: dos estados
(`OWNED`/`WISHLIST`), filtro `owner` (`mine`/`other`/`all`) y recorte de `notes` vía
`ResponseVisibility`. `externalId` es `head` + `tail` (16 hex) del catálogo y es único en la
colección (`AmiiboConflictException` → **409**). Los endpoints están bajo `/api/v1/amiibos` y la
búsqueda externa es `GET /api/v1/amiibos/search?name=` (público, como el resto de `GET /api/v1/**`).

### Trampas verificadas de AmiiboAPI (2026-10-13)

| Trampa | Detalle |
|--------|---------|
| URL canónica `/api/amiibo/` | Sin slash responde **308**; el `RestClient` lo sigue |
| Parámetros case-sensitive | `name`, `id`, `showusage`; `gameseries` solo en minúscula (camelCase se ignora en silencio) |
| Desconocidos se ignoran | Un typo devuelve el catálogo entero (950), no un error: el cliente construye los parámetros exactos y no expone filtros libres |
| `?id=` devuelve **objeto** | La búsqueda devuelve `{"amiibo":[...]}`, el detalle `{"amiibo":{…}}`: DTOs distintos (`AmiiboApiListResponse`/`AmiiboApiDetailResponse`) |
| Sin resultados → **200 con `{"amiibo":[]}`** | Lista vacía es un resultado válido, no error; `{"amiibo":null}` también se tolera como vacío |
| `?id=` desconocido → **400** | `findById` lo traduce a `Optional.empty()` y **no** reintenta |
| Sin paginación | AmiiboAPI devuelve todos los que coinciden; la paginación es del repositorio local (`PagedResults.slice`) |
| 5xx/timeout → error | 4 intentos (3 reintentos a 1s; intervalo inyectable, 0 ms en tests) y **reintenta solo 5xx**; al agotar relanza (`RestClientResponseException` → 502 / `ResourceAccessException` → 503). A diferencia de Google Books/Scryfall, no hay lista vacía por fallo de red |

**`@Autowired` obligatorio en el constructor público de `AmiiboApiClient`.** El cliente tiene dos
constructores (el público de un argumento y el package-private `(RestClient, long)` para tests) y,
sin la anotación, Spring busca el constructor de por defecto y el contexto falla con
`No default constructor found` (mismo patrón que `GoogleBooksClient`).

Los records de deserialización llevan `@JsonIgnoreProperties(ignoreUnknown = true)` **todos** y un
solo constructor (lección de Scryfall). El `type` del catálogo se mapea a `AmiiboType` de forma
tolerante: un valor desconocido del catálogo se traduce a `null`, no a un error.

## Importación de mazos (issue #353)

- El import es **asíncrono**: `POST /api/v1/decks/{id}/imports` (multipart) o `.../imports/text`
  (texto plano) responden **202** + `jobId` + `Location`/`statusUrl` hacia
  `/api/v1/decks/{id}/imports/{jobId}`; el cliente hace poll de ese GET.
- El estado de los jobs vive **en memoria** (Caffeine, TTL 30 min): un reinicio los borra y el
  `GET` responde `404`. Reenviar el archivo es seguro con `mode=REPLACE`.
- `GET /api/v1/**` es `permitAll`, así que el `GET` del job **no** devuelve 401: pasa al caso de
  uso con `currentUserId = null` y la validación de propiedad lo tumba con 403.
- **Nunca llames a `DeckImportWorker.run()` desde dentro del mismo bean**: `@Async` se ignora en
  silencio por auto-invocación. El worker **no lee el `SecurityContext`**; el `userId` viaja como
  parámetro.
- El mazo se guarda **una sola vez**, al final. Si el job falla, el mazo queda intacto y el job
  pasa a `FAILED`.
- Un `UPSTREAM_ERROR` (Scryfall 5xx/timeout tras los 4 reintentos) tumba el job y **no** se
  confunde con una carta que no existe: `NOT_FOUND` es que Scryfall contestó (200 con
  coincidencias que no encajan, o 404 por no conocer el nombre) y `UPSTREAM_ERROR` es que no
  pudo contestar. Es la lección de #451, al revés.
- Solo las **cinco tierras básicas** (Plains, Island, Swamp, Mountain, Forest) se resuelven
  **en local** con `typeLine` "Basic Land": sin eso el `DeckValidator` las marca como singleton
  inválido. Nada más entra en esa lista, porque el tipo que se inventa es el que el validador
  usa para exentar de singleton: una no básica ahí (Wasteland, Tundra, Llanowar Elves...)
  permitiría cuatro copias en un mazo Commander. Las variantes van a Scryfall como cualquier
  otra carta.
- `unique=oracle` es obligatorio en toda búsqueda por nombre: sin él Scryfall devuelve todas las
  reimpresiones y cada línea del mazo parecería ambigua.
- **`page_size` no existe en Scryfall** (son 175 siempre) — no intentes paginar las
  reimpresiones desde este flujo.
- El executor saturado devuelve **429** (`TaskRejectedException` en `GlobalExceptionHandler`),
  no 500.
- `DeckControllerTest` y `DeckImportControllerTest` limpian cachés con `CacheTestSupport.clearAll`
  porque los mazos están cacheados.
- El registro de jobs **no** va en `CacheConfig.CACHE_NAMES`: es un `Cache` de Caffeine consultado
  directamente por `DeckImportJobStore`.

## Git workflow

Work happens on feature branches (`feat/*`, `fix/*`) opened as PRs against `main`; each PR is linked to an issue (`Closes #N`). Keep `mvn verify` green before opening a PR.
