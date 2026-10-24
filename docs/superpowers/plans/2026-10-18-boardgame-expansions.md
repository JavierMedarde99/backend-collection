# Plan: Expansiones de juegos de mesa (#495)

Rama: `feat/boardgame-expansions` (desde `origin/main`). Diseño cerrado en la wiki
`research/future/boardgame-expansions.md` + issue #495. Un solo PR con `Closes #495`.

## Ciclos TDD (red → green → refactor)

| # | Ciclo | RED (test) | GREEN (producción) |
|---|-------|-----------|--------------------|
| 1 | Enum de tipo | `BoardGameTypeTest` (JSON redondo, claves `boardgame`/`boardgameexpansion`) | `domain/model/BoardGameType` |
| 2 | Campos en el modelo | `BoardGameEntityMapperTest` (round-trip `bggType`/`parentId`/`parentTitle`) | campos en `BoardGame`, `BoardGameEntity`, mapper |
| 3 | Índice | `MongoIndexMigrationTest` (`{ownerId, parentId}` en `board_games`) | `MongoIndexMigration` |
| 4 | Parser BGG | `BoardGameXmlMapperTest` (tipo del item, links hijos/padres, `toExpansionInfo`) | `BggXmlItem.type`, `BggLink.id/inbound`, métodos del mapper |
| 5 | Cliente | `BggXmlClientTest` (`expansionsOf`, `findByBggIds`, error → excepción) | `BggXmlClient` + puerto |
| 6 | Repositorio | `BoardGamePersistenceAdapterTest` / `SpringDataBoardGameRepository` | 4 métodos `findByOwner…` |
| 7 | Servicio | `BoardGameServiceTest` (R1–R3, R5, R6, R7, `inCollection`, evicts) | `BoardGameExpansionService` + cambios en `BoardGameService` |
| 8 | Caché | `CacheConfigCacheNamesTest` (40 → 41) | `CacheConfig.BOARDGAME_EXPANSIONS` |
| 9 | HTTP | `BoardGameControllerTest` (400/403/404/422/502, `?type=`, 201) | controller + DTOs + handlers |

Gate final: `mvn verify` (JaCoCo LINE ≥ 0.80).

## Decisiones de diseño

- **Tipos de filtro de listado**: enum aparte `BoardGameTypeFilter { BASE, EXPANSION, ALL }`
  (`?type=base|expansion|all`, por defecto `all`); no se reutiliza `BoardGameType`, que mapea 1:1
  con el vocabulario de BGG.
- **`BoardGameSearchCriteria` gana un 6.º componente `type`** (sin segundo constructor: se
  actualizan los 15 call sites; lección de Scryfall sobre constructores dobles en records).
- **Puerto nuevo**: `BoardGameExpansionUseCase` con `expansionsOf(baseId, viewerId)` y
  `addExpansions(baseId, bggIds, userId)`; servicio `BoardGameExpansionService`.
- **Catálogo de BGG**: `ExternalBoardGameCatalogClient` gana `expansionsOf(baseBggId)` y
  `findByBggIds(ids)`. A diferencia de `search()`, **no** se tragan los errores: lanzan
  `ExternalCatalogException` → 502 (inconsistencia deliberada, documentada en el issue).
- **Caché** `boardgameExpansions` claveada por `bggId` del base sobre el cliente (catálogo puro);
  `inCollection` se calcula fuera, con `findByOwnerIdAndBggIdIn`.
- **422**: excepción nueva `BoardGameExpansionException` → `HttpStatus.UNPROCESSABLE_ENTITY`.
- **R5 (parentId inmutable)**: `copyUpdatableFields` no copia `bggType`/`parentId`/`parentTitle`,
  así que el `PUT` ya los preserva; se cubre con test para que no se rompa.
- **R6 (cascada)**: en `BoardGameService.delete`, si el registro es base se borran primero sus
  expansiones con `deleteByOwnerIdAndParentId`.
- `POST`/`PUT` no aceptan `bggType` ni `parentId` (no están en `BoardGameRequest`).

## Errores

| Código | Cuándo |
|--------|--------|
| 400 | `bggIds` vacío o >20 (bean validation) |
| 403 | base ajeno (`OwnershipValidator`) |
| 404 | base inexistente |
| 422 | base no `OWNED`; `bggId` no es expansión en BGG; link `inbound` no apunta al base; base sin `bggId` |
| 502 | BGG no responde o devuelve error |

## Estado: TERMINADO → PR [#496](https://github.com/JavierMedarde99/backend-collection/pull/496)

- Los 9 ciclos se ejecutaron red → green observado. Reds observados: símbolos ausentes
  (ciclos 1–7b), fallos en runtime para R6 + propagación de `type` y para la invalidación de
  `stats` (2 tests), y **17 fallos** en `BoardGameControllerTest` (ciclo 9).
- **Red final de `mvn verify`: ninguna.** Gate verde: **1238 tests, 0 fallos, LINE ≥ 0.80**.
- Commit `5f36226` con `Closes #495`, solo `git add src/` (los planes siguen sin trackear).

### Descubrimientos durante la implementación

- **`?type=base|expansion|all` devolvía 400 en todas las peticiones del listado.** El
  `StringToEnum` por defecto de Spring **no** sube a mayúsculas, así que hasta el
  `defaultValue="all"` reventaba. Solución: `StringToBoardGameTypeFilterConverter`, el mismo
  patrón que los 7 conversores de enums que ya existen en `infrastructure/config`.
- **R5 y R7 son tests de caracterización**: no pueden fallar antes de implementarse (el
  comportamiento ya existía implícito); el red real de este bloque fue R6 y la invalidación de
  `stats`, que se provocó revirtiendo el código verde antes de escribir los tests.
- **`GET /boardgames` exige viewer** cuando `owner=mine` (por defecto) → 401 sin sesión; los
  tests del filtro tienen que autenticarse.
- **`OwnershipValidator` consulta Mongo** al comprobar si el usuario es admin: los tests HTTP
  con 403 necesitan `@MockitoBean UserRepository` (convención ya usada en `AmiiboControllerTest`
  y `FunkoControllerTest`).
- La caché `boardgameExpansions` **no** se cachea sobre el viewer: el catálogo es del base y
  `inCollection` se resuelve después, fuera de la caché.

### Desviación conocida (anotada en el PR)

`findByOwnerIdAndParentId` está en puerto/adapter/Spring Data como pide el issue pero **no
tiene llamador en producción**; el índice `{ownerId,parentId}` lo justifica
`deleteByOwnerIdAndParentId` (la cascada R6 es el consumidor real).
