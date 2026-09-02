# Architecture

This document describes the architecture of `wiki-collection-backend`, a Spring Boot 4.1.1 REST API built with **hexagonal architecture** (ports & adapters), running on Java 25 with MongoDB as its persistence store.

## Tech stack

| Concern | Choice |
|---------|--------|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 |
| Persistence | Spring Data MongoDB (`books` NOT used; collection is `BOOKS`) |
| External integration | Google Books REST API (via `RestClient`) |
| Build / CI | Maven (`mvn verify`, CI: `./mvnw clean package`) |
| Code generation | Lombok 1.18.46 (requires `annotationProcessorPaths` on JDK 25) |
| Coverage gate | JaCoCo LINE ≥ 0.80, enforced at `verify` |

## Hexagonal architecture overview

The code is organized into three top-level layers under `com.wikicollection`:

```
com.wikicollection
├── domain           <- Core (models + ports). No framework dependencies.
├── application      <- Use-case orchestration (services + exceptions).
└── infrastructure   <- Adapters (in/out) + config. Depends on domain.
```

The **dependency rule is strictly inward**: `infrastructure` → `application` → `domain`. Nothing in `domain` may import from `application` or `infrastructure`, and `application` only depends on `domain` (never on infrastructure). Each layer's responsibilities:

- **`domain`**: plain POJOs and the *ports* (interfaces) the rest of the system adapts to. No Spring annotations on domain models; ports use Spring's `Page`/`Pageable` only for pagination.
- **`application`**: services that implement the inbound ports and orchestrate use cases using the outbound ports. Business exceptions live here.
- **`infrastructure`**: concrete Spring `@Component` adapters implementing the outbound ports, plus the inbound HTTP adapter (`@RestController`) and framework configuration.

## Layers and packages

### Domain (`domain/`)

**Models** (`domain/model/`) — plain data holders:

- `Book` — the domain aggregate/entity persisted as a Mongo document. Fields: `id`, `externalId`, `title`, `descripcion`, `author` (singular String), `pages`, `type`, `state`, `comment`, `start` (0–5), `startDate` (LocalDate), `endDate` (LocalDate), `frontpage`.
- `BookSearchResult` — a DTO returned by the external catalog search (not persisted; shaped for Google Books).
- `BookType` (enum) — `MANGA`, `NOVEL`, `GRAPHIC_NOVEL`.
- `BookState` (enum) — `TO_READ`, `READING`, `COMPLETED`.

**Inbound ports** (`domain/port/in/`) — what the application offers to adapters:

- `BookUseCase` — CRUD + `findByState` (returns `Page<Book>`).
- `BookSearchUseCase` — `search(String)` returning `List<BookSearchResult>`.

**Outbound ports** (`domain/port/out/`) — what the application needs from adapters:

- `BookRepository` — persistence contract (`findAll`, `findById`, `findByState`, `save`, `deleteById`).
- `ExternalBookCatalogClient` — external catalog search contract.

### Application (`application/`)

**Services** (`application/service/`):

- `BookService implements BookUseCase` — validates existence (`BookNotFoundException`), copies updatable fields on `update`. Delegates persistence to `BookRepository`.
- `BookSearchService implements BookSearchUseCase` — rejects blank queries (`IllegalArgumentException`), delegates to `ExternalBookCatalogClient`.

**Exceptions** (`application/exception/`):

- `BookNotFoundException` — mapped to HTTP 404.

### Infrastructure (`infrastructure/`)

**Inbound web adapter** (`adapter/in/web/`):

- `BookController` — `@RestController` at `/api/books`. Exposes list (paginated, filterable by `state`), get-by-id, create, update, delete, and `/api/books/search?name=` for external search. Uses `@Validated` + Bean Validation on DTOs.
- `GlobalExceptionHandler` — `@RestControllerAdvice` mapping exceptions to `ErrorResponse`:
  - `BookNotFoundException` → 404
  - `IllegalArgumentException` → 400
  - `MethodArgumentNotValidException` → 400 (validation field errors)
  - `IllegalStateException` → 500
- **`dto/`** — `BookRequest` (inbound, validated: `title`/`author` `@NotBlank`, `start` 0–5, `pages` ≥ 0, `type`/`state` `@NotNull`), `BookResponse` (outbound), `ErrorResponse`, and `BookDtoMapper` (maps `BookRequest` ⇄ `Book` ⇄ `BookResponse`).

**Outbound persistence adapter** (`adapter/out/persistence/`):

- `BookEntity` — Mongo `@Document(collection = "BOOKS")` mapping (same fields as domain `Book`).
- `SpringDataBookRepository` — Spring Data `MongoRepository<BookEntity, String>`; declares `findByState`.
- `BookEntityMapper` — maps `Book` ⇄ `BookEntity`.
- `BookPersistenceAdapter implements BookRepository` — adapts the port to Spring Data; the only code that talks to the repository.

**Outbound catalog adapter** (`adapter/out/google/`):

- `GoogleBooksClient implements ExternalBookCatalogClient` — calls the Google Books API with `q=intitle:<name>`, `langRestrict=es`, `maxResults=10`. **Retries 3 more times (4 total) at 1s intervals** on 5xx/timeout and **rethrows** if all fail; only returns an empty list when the response has no items.

**Configuration** (`config/`):

- `WebConfig` — CORS for `http://localhost:5173` on `/api/**`.
- `MongoAuditConfig` — the single `@EnableMongoAuditing` (do not add a second one, it breaks the context).
- `RestClientConfig` — provides `RestClient` bean base-URL'd at `https://www.googleapis.com/books`.
- `StringToBookStateConverter` — `Converter<String, BookState>` so `?state=` query params bind by enum name.

## Request flow (hexagonal dependency direction)

```
Client
  │  HTTP
  ▼
BookController (infrastructure/in)            ← inbound adapter
  │  calls inbound port
  ▼
BookUseCase / BookSearchUseCase (domain/port/in)
  │
  ▼
BookService / BookSearchService (application) ← use cases orchestrate
  │  calls outbound port
  ▼
BookRepository / ExternalBookCatalogClient (domain/port/out)
  │
  ├───────────────┬───────────────────┐
  ▼               ▼                   ▼
BookPersistenceAdapter          GoogleBooksClient
  (Mongo/BOOKS)     (Google Books API)       ← outbound adapters
```

Reverse dependencies never happen: a controller/adapter depends on a port interface, not on a concrete service; a service depends on a port, not on Spring Data or RestClient.

## Data model — `Book` / Mongo collection `BOOKS`

| Field | Type | Notes |
|-------|------|-------|
| `id` | String | Mongo `@Id` (PK) |
| `externalId` | String | Optional reference to an external/Google source |
| `title` | String | Required |
| `descripcion` | String | |
| `author` | String | Singular (not a list) |
| `pages` | Integer | ≥ 0 |
| `type` | `BookType` | MANGA, NOVEL, GRAPHIC_NOVEL |
| `state` | `BookState` | TO_READ, READING, COMPLETED |
| `comment` | String | |
| `start` | Integer | 0–5 (validated) |
| `startDate` | LocalDate | |
| `endDate` | LocalDate | |
| `frontpage` | String | Cover/thumbnail URL |

`Book` (domain), `BookEntity` (persistence) and `BookResponse` (web) mirror these fields; `BookRequest` is the validated input form.

## Testing strategy

Tests mirror the layers and live in the same package structure under `src/test`:

- `application/service/` — unit tests for `BookService` and `BookSearchService` with mocked ports.
- `infrastructure/adapter/out/persistence/` — adapter tests with mocked Spring Data repo + mapper.
- `infrastructure/adapter/out/google/` — `GoogleBooksClientTest` uses `MockRestServiceServer` to stub the Google API and verify the retry behavior (constructed with a 0 ms interval).
- `infrastructure/adapter/in/web/` — `BookControllerTest` is `@SpringBootTest` + `@AutoConfigureMockMvc` with `@MockitoBean` for the Spring Data repo and Google client; verifies HTTP contracts (status, JSON, CORS, validation).

**Key property:** `spring.data.mongodb.auto-index-creation=false` so `@SpringBootTest` does not need a running MongoDB.

## Operational notes

- **MongoDB URI:** configured via `spring.mongodb.uri` (NOT `spring.data.mongodb.uri`). Defaults to `mongodb://localhost:27017/wiki-collection`; use the `SPRING_MONGODB_URI` env var to override. Do not hardcode credentials in `application.properties`.
- **Google Books key:** `google.books.api-key`, injectable via `GOOGLE_BOOKS_API_KEY`.
- **Coverage gate:** `mvn verify` fails unless LINE coverage ≥ 0.80.

## Conventions

- **Language:** field/method names in code are mix of English structure and Spanish business fields (e.g. `descripcion`, `comment`, `start`, `frontpage` mirror the domain model); error messages are in Spanish.
- **Workflow:** changes are made on feature branches (`feat/*`, `fix/*`, `docs/*`) and merged to `main` via pull requests, usually closing a linked issue.
