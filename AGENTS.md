# AGENTS.md

Spring Boot 4.1.1 REST API (`wiki-collection-backend`) for personal collection tracking. Java 25 + MongoDB, **hexagonal architecture**. `ARCHITECTURE.md` documents the Book slice in depth; `README.md` is the endpoint reference. This file holds the things those two get wrong or omit.

## Commands

- `mvn verify` is the gate — NOT `mvn test`. `verify` runs the JaCoCo **LINE coverage ≥ 0.80** rule and fails the build below it.
- Single test: `mvn test -Dtest=BookControllerTest`; single method: `-Dtest=BookServiceTest#findById_returnsBook_whenExists`.
- Skip coverage while iterating: `mvn test` (tests only) or `mvn package -DskipTests`.
- Run app: `mvn spring-boot:run` (port 8080; `SPRING_MONGODB_URI` to point elsewhere).
- No lint, no formatter, no wrapper. Use a locally installed `mvn`.

**CI is currently broken — do not trust a green/red signal from it.** `.github/workflows/checks.yml`:
- calls `./mvnw clean package`, but **no Maven wrapper exists** in the repo (never has, per `git log`; the `Dockerfile` even documents "Sin wrapper mvnw en el repo"). The step fails immediately.
- has **no `on:` trigger block**, so the workflow never runs anyway.
- sets `JWT_SECRET: ${{ secrets.GOOGLE_BOOKS_API_KEY }}` (books key reused as the JWT secret).

Verify work with a local `mvn verify`.

## Gotchas that break builds silently

- **Lombok on JDK 25** requires `<annotationProcessorPaths>` in `maven-compiler-plugin` (lombok 1.18.46). It is configured; don't remove it. If `@Getter`/`@Builder` stop being recognized, this is why. No MapStruct is used **on purpose** (avoids a second annotation processor) — mappers are hand-written and null-safe.
- **Mongo key is `spring.mongodb.uri`**, NOT `spring.data.mongodb.uri`. The only `spring.data.*` key is `auto-index-creation`.
- There must be **exactly one `@EnableMongoAuditing`** (`MongoAuditConfig`). A second one → `BeanDefinitionOverrideException` → every controller test fails.
- Add a field to a domain model ⇒ update **both** mappers of that aggregate (`XDtoMapper` + `XEntityMapper`) and their `mapper_roundTripsAllFields` / `mapper_handlesNull` tests. The coverage gate will otherwise fail you.
- Adding a new external client? Retries are **not** centralized — each client has its own properties (`scryfall.api.retry-*`, `bgg.api.retry-*`, `tmdb.api.retry-*`) and `GoogleBooksClient` hardcodes `TOTAL_ATTEMPTS = 4` / 1s. Its test constructs the client with a 0 ms interval to keep the suite fast — update it if you change the count.

## Ownership / multi-tenancy (the thing most likely to be missed)

Every collection document is owned. Writes are authorized; reads are scoped and field-filtered.

- `@CurrentUser String viewerId` on a controller param is resolved by `CurrentUserHandlerMethodArgumentResolver` (registered in `WebConfig`). It is `null` for anonymous requests — never assume non-null.
- Listings take `?owner=mine|other|all` (**default `mine`**), resolved by `OwnerScopeResolver` into a `Scope(ownerId, excludeOwnerIds)`. `mine` and `all` require authentication (`UnauthenticatedException` → 401); `other` is public but excludes private owners. An invalid value throws `IllegalArgumentException` → 400.
- Mutations go through `OwnershipValidator.validateOwner(resourceOwnerId, currentUserId)` → `ForbiddenException` (403), with a bypass for the user whose `username` matches `app.admin.username` (default `admin`). Reads use `ResponseVisibility.canSeePrivate(ownerId, CollectionType.X)` to decide whether to strip private fields via `response.withoutPrivate()`. **Copy this pattern; don't return entities directly.**
- The six owned collections are `books`, `games`, `board_games`, `magic_cards`, `decks`, `movie_shows` — repeated as a hardcoded `List<String>` in `MongoIndexMigration`, `UserOwnedBackfillMigration`, `AuthDataMigration`, and in `CollectionType`. **A new collection must be added to all of them.**
- `UserOwned` is a denormalized snapshot (`ownerId`/`ownerName`/`username`) refreshed by `OwnerResolver` on save — not a Mongo `$lookup`.

## Data migrations (no Flyway/Liquibase)

Schema/data changes are idempotent `ApplicationRunner`s in `infrastructure/config`: `AuthDataMigration`, `MongoIndexMigration`, `UserOwnedBackfillMigration`, `UserPreferencesMigration`, plus `BoardGameStatusMigration` (a `CommandLineRunner` in `adapter/out/persistence`). All are gated by `app.migration.enabled` / `app.boardgame-status-migration.enabled` and swallow `RuntimeException` so startup survives without Mongo. Add a new one in that style — don't assume a migration framework exists.

## API & conventions

- All endpoints are under **`/api/v1/*`**, not `/api/*`. Base paths: `books`, `games`, `boardgames`, `magic`, `decks`, `movieshows`, `images`, `stats`, `users`, `preferences`, `auth`.
- Listings are paged with `page` (0-based), `size`, `sort=field,asc|desc`; the default sort differs per collection — `title,asc` for books/games/boardgames/movieshows, `name,asc` for magic/decks.
- External search is paged **in the application layer** via `PagedResults.slice(...)`, not by the upstream API — `maxResults` is no longer sent to Google Books. Cached with `@Cacheable` on the client (`CacheConfig` Caffeine caches, TTLs from `app.cache.ttl.*`).
- Collection filters are multivalued: `?genre=A&genre=B` binds to `List<String>` and becomes an OR in the `Criteria`.
- `getUserIdsWithPrivateCollection(type)` returns `null` in some paths — `OwnerScopeResolver.excludedOwnerIds` null-guards it; do the same.
- Enum query params bind via explicit `StringTo*Converter` beans (`BookState`, `GameStatus`, `BoardGameStatus`, `MovieStatus`, `MovieMediaType`). Adding an enum filter means adding a converter.
- `GlobalExceptionHandler` maps each exception to a status; add a handler with any new exception, and return `ErrorResponse`.
- Swagger UI at `/swagger-ui.html`; OpenAPI annotations (`@Tag`/`@Operation`/`@ApiResponses`) are used on controllers and expected on new endpoints.

## Testing (Spring Boot 4)

- 40 Mockito unit tests (`@ExtendWith(MockitoExtension)`) + 15 `@SpringBootTest` MockMvc tests. No `src/test/resources` — everything is inline.
- Boot 4 imports that look wrong but are correct:
  - `@AutoConfigureMockMvc` → `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`
  - `@MockitoBean` → `org.springframework.test.context.bean.override.mockito.MockitoBean`
- Controller tests need **no live Mongo**: they use `@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})` plus `@MockitoBean` for the repository. Copy that exact property pair.
- Authenticated controller tests need a principal: `SecurityMockMvcRequestPostProcessors.user(...)`. GET `/api/v1/**` is `permitAll`; writes, `/api/v1/auth/me`, and `/api/v1/preferences/**` are authenticated. `SecurityMatrixTest` locks this matrix down.
- External HTTP clients are tested with **OkHttp `MockWebServer`** (real socket, not `MockRestServiceServer`) — 8 test classes use it.
- `migrations`/ownership wiring: when adding an owner-scoped endpoint, also update `*OwnerFilterTest` and the visibility assertions.

## Config & secrets

- Secrets are env vars only (`SPRING_MONGODB_URI`, `GOOGLE_BOOKS_API_KEY`, `RAWG_API_KEY`, `STEAM_API_KEY`, `BGG_AUTH_TOKEN`, `TMDB_API_KEY`, `CATBOX_USERHASH`, `JWT_SECRET`, `ADMIN_*`, `APP_CORS_ALLOWED_ORIGINS`). Defaults in `application.properties` are placeholders — never commit a real credential.
- `app.jwt.secret` has a weak literal default; anything non-local must set `JWT_SECRET` (base64, ≥256 bits: `openssl rand -base64 32`).
- Images are a **two-step flow**: upload to Catbox (`POST /api/v1/images/upload`, multipart `file`), then store the returned URL in the entity's image field. There is no multipart in the CRUD endpoints.
- Dockerfile is a multi-stage build for Render; it honors `$PORT` and builds with `-DskipTests`.

## Git workflow

Feature branches (`feat/*`, `fix/*`, `docs/*`) → PR against `main`, each closing a linked issue. Commit messages follow `type(scope): descripción` in Spanish (e.g. `feat(genres): catálogo global GET /{colección}/genres`). Keep a local `mvn verify` green before opening a PR, since CI can't confirm it.
