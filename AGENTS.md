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
  - `port/out`: `BookRepository`, `ExternalBookCatalogClient`
- `application/` — services (`BookService`, `BookSearchService`) and exceptions (`BookNotFoundException`, etc.)
- `infrastructure/` — adapters + config:
  - `adapter/in/web`: `BookController`, `GlobalExceptionHandler`, DTOs
  - `adapter/out/persistence`: `BookEntity`, `SpringDataBookRepository`, `BookEntityMapper`, `BookPersistenceAdapter` (Mongo collection `BOOKS`)
  - `adapter/out/google`: `GoogleBooksClient` (search via external catalog)
  - `config`: `WebConfig` (CORS for `http://localhost:5173`), `MongoAuditConfig`, `StringToBookStateConverter`, `RestClientConfig`

There is intentionally **only one** `@EnableMongoAuditing` (in `MongoAuditConfig`). Don't add a second one (e.g., on the main application class) — it causes `BeanDefinitionOverrideException` and fails every controller test.

## Domain model (`Book` — Mongo collection `BOOKS`)

`id`, `externalId`, `title`, `descripcion`, `author` (singular String), `pages`, `type`, `state`, `comment`, `start` (Integer 0–5), `startDate` (LocalDate), `endDate` (LocalDate), `frontpage`. Enums: `BookType` (MANGA, NOVEL, GRAPHIC_NOVEL), `BookState` (TO_READ, READING, COMPLETED). Requests validate `start` 0–5.

Search (`GET /api/books/search`) uses query param **`name`** (not `q`) and hits Google Books with `intitle:<name>` + `langRestrict=es` + `maxResults=10`. `GET /api/books` filters by `state`; there is no `tag` filter (tags were removed with the BOOKS model).

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

## Git workflow

Work happens on feature branches (`feat/*`, `fix/*`) opened as PRs against `main`; each PR is linked to an issue (`Closes #N`). Keep `mvn verify` green before opening a PR.
