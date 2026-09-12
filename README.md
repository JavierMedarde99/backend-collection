# wiki-collection-backend

Backend Spring Boot para gestionar colecciones personales: libros, videojuegos, juegos de mesa,
cartas Magic: The Gathering (incluye mazos Commander) y películas/series. Expone una API REST
documentada con OpenAPI y persiste en MongoDB.

## Stack

- Java 25 · Spring Boot 4.1.1 · MongoDB
- Arquitectura hexagonal: `domain/` (modelos y puertos) ← `application/` (servicios) ← `infrastructure/` (adaptadores web, persistencia y clientes externos)
- Lombok (vía `annotationProcessorPaths` en `maven-compiler-plugin`), Springdoc OpenAPI, JaCoCo

## Dominios y colecciones Mongo

| Dominio | Colección | Fuente externa |
|---|---|---|
| Libros (`Book`) | `books` | Google Books |
| Videojuegos (`Game`) | `games` | RAWG, FreeToGame, Steam (logros) |
| Juegos de mesa (`BoardGame`) | `board_games` | BoardGameGeek (JSON + XML fallback) |
| Cartas Magic (`MagicCard`) | `magic_cards` | Scryfall |
| Mazos Commander (`Deck`) | `decks` | Scryfall |
| Películas/series (`MovieShow`) | `movie_shows` | TMDB |

## API REST

Paginación común en los listados: `page` (base 0), `size`, `sort` (`campo,asc|desc`).
Documentación interactiva: `/swagger-ui.html` (OpenAPI en `/v3/api-docs`).

### Libros — `/api/v1/books`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/books?name=&author=&type=&state=` | Listar con filtros |
| GET | `/api/v1/books/{id}` | Obtener por id |
| POST | `/api/v1/books` | Crear |
| PUT | `/api/v1/books/{id}` | Actualizar |
| DELETE | `/api/v1/books/{id}` | Eliminar (204) |
| GET | `/api/v1/books/search?name=` | Buscar en Google Books |

### Videojuegos — `/api/v1/games`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/games?name=&platform=&status=` | Listar con filtros |
| GET | `/api/v1/games/{id}` | Obtener por id |
| POST | `/api/v1/games` | Crear |
| PUT | `/api/v1/games/{id}` | Actualizar |
| DELETE | `/api/v1/games/{id}` | Eliminar (204) |
| GET | `/api/v1/games/{id}/achievements?steamId=` | Logros de Steam |
| GET | `/api/v1/games/search?name=` | Buscar en RAWG/FreeToGame |

### Juegos de mesa — `/api/v1/boardgames`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/boardgames?name=&status=` | Listar con filtros (`OWNED`, `WISHLIST`) |
| GET | `/api/v1/boardgames/{id}` | Obtener por id |
| POST | `/api/v1/boardgames` | Crear |
| PUT | `/api/v1/boardgames/{id}` | Actualizar |
| DELETE | `/api/v1/boardgames/{id}` | Eliminar (204) |
| GET | `/api/v1/boardgames/search?name=` | Buscar en BoardGameGeek |

### Cartas Magic — `/api/v1/magic`

Sin creación ni edición manual: las cartas se añaden desde Scryfall.

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/magic?name=&rarity=&color=&type=` | Listar colección local con filtros |
| GET | `/api/v1/magic/{id}` | Obtener por id |
| POST | `/api/v1/magic/scryfall/{scryfallId}` | Añadir carta desde Scryfall (201) |
| DELETE | `/api/v1/magic/{id}` | Eliminar (204) |
| GET | `/api/v1/magic/search?name=` | Buscar en Scryfall (incluye texto y `colorIdentity`) |
| GET | `/api/v1/magic/commanders?colors=` | Comandantes por identidad de color (`wubrg`) |

### Mazos Commander — `/api/v1/decks`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/decks?name=` | Listar (`name` opcional) |
| GET | `/api/v1/decks/{id}` | Obtener por id |
| POST | `/api/v1/decks` | Crear |
| PUT | `/api/v1/decks/{id}` | Actualizar |
| DELETE | `/api/v1/decks/{id}` | Eliminar (204) |
| POST | `/api/v1/decks/{id}/cards` | Añadir carta desde Scryfall (`{scryfallId, quantity}`; marca `inCollection`/`isProxy`) |
| DELETE | `/api/v1/decks/{id}/cards/{scryfallId}` | Quitar carta |
| GET | `/api/v1/decks/{id}/status` | `{status, message}`: `DRAFT`/`COMPLETE`/`INVALID` (`message` con la razón solo si es inválido: singleton, colores, baneadas, tamaño) |

### Películas y series — `/api/v1/movieshows`

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/v1/movieshows?name=&status=&mediaType=` | Listar con filtros (`MOVIE`/`TV`) |
| GET | `/api/v1/movieshows/{id}` | Obtener por id |
| POST | `/api/v1/movieshows` | Crear (409 si ya existe el `externalId`) |
| PUT | `/api/v1/movieshows/{id}` | Actualizar |
| DELETE | `/api/v1/movieshows/{id}` | Eliminar (204) |
| GET | `/api/v1/movieshows/search?name=&mediaType=` | Buscar en TMDB (pelis y series; `mediaType` opcional) |

Estados de visionado (`MovieStatus`): `WATCHING`, `WATCHED`, `PLAN_TO_WATCH`.

## Configuración

 Clave de MongoDB: `spring.mongodb.uri` (no `spring.data.mongodb.*`).
 No hay credenciales en el repo: se configuran por variables de entorno.

| Variable | Descripción |
|---|---|
| `SPRING_MONGODB_URI` | URI de MongoDB (defecto `mongodb://localhost:27017/wiki-collection`) |
| `GOOGLE_BOOKS_API_KEY` | Google Books |
| `RAWG_API_KEY` | RAWG |
| `STEAM_API_KEY` | Steam |
| `BGG_AUTH_TOKEN` | BoardGameGeek |
| `TMDB_API_KEY` | TMDB |

Propiedades externas con reintentos en `src/main/resources/application.properties`
(`scryfall.api.*`, `bgg.api.*`, `tmdb.api.*`). CORS abierto a `http://localhost:5173`.

## Comandos

```bash
mvn verify          # build + tests + gate de cobertura (líneas ≥ 80%)
mvn spring-boot:run # arrancar la app (puerto 8080)
```

Los tests no necesitan Mongo en ejecución (repositorios y clientes externos mockeados).
Detalles de contribución y gotchas (Lombok + JDK 25, auditoría Mongo única, etc.) en `AGENTS.md`.
