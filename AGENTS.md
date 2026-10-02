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

`id`, `externalId`, `title`, `descripcion`, `author` (singular String), `pages`, `type`, `state`, `comment`, `start` (Integer 0–5), `startDate` (LocalDate), `endDate` (LocalDate), `frontpage`, `series`, `seriesKey`, `seriesOrder`. Enums: `BookType` (MANGA, NOVEL, GRAPHIC_NOVEL), `BookState` (TO_READ, READING, COMPLETED). Requests validate `start` 0–5.

Search (`GET /api/books/search`) uses query param **`name`** (not `q`) and hits Google Books with `intitle:<name>` + `langRestrict=es` + `maxResults=10`. `GET /api/books` filters by `state`; there is no `tag` filter (tags were removed with the BOOKS model).

## Book series (issue #425)

Three embedded fields, no `Series` entity and no migration — existing docs read `null`:

- `series` — free text written by the user (`@Size(max = 200)`), public like `publisher`
- `seriesKey` — grouping key, **never accepted from the client**; `BookService.normalizeSeries()` recomputes it from `series` via `SeriesKey.normalize()` (lowercase, collapsed whitespace) and discards any client value
- `seriesOrder` — position in the series (`@Min(0)`); cleared to `null` together with `series`/`seriesKey` when there is no series

`seriesKey` is deliberately **absent from `BookRequest` and `BookResponse`**. `GET /api/books?series=<text>` is a partial case-insensitive regex on `series` (same as `author`), so callers never need to know the key. `GET /api/books/series` returns the distinct series names in use, mirroring `GET /api/books/genres`, and respects the private-collection exclusions of `OwnerScopeResolver`.

No `@Indexed` on `series`/`seriesKey`: `spring.data.mongodb.auto-index-creation=false`, and the `?series=` regex is unanchored, so Mongo could not use the index anyway (same as `title`/`author`).

## Git workflow

Work happens on feature branches (`feat/*`, `fix/*`) opened as PRs against `main`; each PR is linked to an issue (`Closes #N`). Keep `mvn verify` green before opening a PR.
