# Plan: 12 PRs — auditoría backend (issues #476–#487)

Decisiones del usuario (2026-10-17):
- #476 JWT → **fail-fast**: no arranca si falta/invalido.
- #479 admin → **contraseña aleatoria impresa UNA vez en el log**.
- #485 → **eliminar `@Transactional`** (Mongo atómico por documento).
- #478 → índice único **parcial** en las 4 colecciones que ya validan unicidad (books, movie_shows, amiibos, funkos).
- #481 → **registrar propiedad de imágenes en Mongo**.
- #477/#484/#487 → PRs **apilados**, merge en ese orden.

## Orden de ejecución (rama nueva desde `origin/main`, salvo la pila)

| # | Issue | Rama | Notas |
|---|-------|------|-------|
| 1 | #480 | `fix/ci-workflow` | CI con `on:`, `mvn clean verify`, `JWT_SECRET` propio |
| 2 | #476 | `fix/jwt-secret-failfast` | Requiere `src/test/resources/application.properties` |
| 3 | #479 | `fix/admin-default-creds` | Admin con contraseña aleatoria + log |
| 4 | #485 | `fix/eliminar-transactional` | Política en AGENTS.md + test guardián |
| 5 | #478 | `fix/externalid-unique-index` | Índice parcial `$type:string, $gt:""` |
| 6 | #482 | `fix/ratelimit-xff` | Última IP de XFF + `maximumSize` |
| 7 | #483 | `fix/preferences-cache-evict` | `@CacheEvict` en 3 métodos |
| 8 | #486 | `fix/profile-amiibos-funkos` | Service + controller |
| 9 | #481 | `fix/image-ownership` | Colección `images` + puerto + adapter |
| 10 | #477 | `fix/delete-account-amiibos-funkos` | **base de la pila** |
| 11 | #487 | `fix/delete-account-bulk` | apilada sobre #477 |
| 12 | #484 | `fix/propagate-ownername-figuras` | apilada sobre #487 |

## Reglas por PR
- TDD: test rojo → verificado → implementación → verde.
- `mvn verify` verde (gate JaCoCo LINE ≥ 0.80) antes de push.
- Commit con prefijo `fix:` / `ci:` y `Closes #N` en el cuerpo.
- PR abierto contra `main` con `Closes #N`.

## Estado (actualizado 2026-10-19 — TODO fusionado; solo queda #480)

### Mergeados en `main` (f1d0748)
- [x] 2. #476 → PR #488
- [x] 3. #479 → PR #489
- [x] 4. #485 → PR #490
- [x] 5. #478 → PR #491
- [x] 6. #482 → PR #492
- [x] 7. #483 → PR #493
- [x] 8. #486 → PR #494
- [x] #495 → PR **#496** (rebasado: conflicto resuelto en `MongoIndexMigration`)
- [x] #497 → PR **#498** (hotfix que arregló `main`)
- [x] #481 → PR **#499**
- [x] #477 → PR **#500**
- [x] #487 → PR **#501** (rebasado sobre `main` tras #500: conflicto en
      `AuthService.deleteAccount` — mis bucles vs el `BoardGameSearchCriteria` de 6 componentes
      que introdujo #496 — resuelto manteniendo el borrado en bloque)
- [x] #484 → PR **#502** (rebaseó limpio sobre el #501 rebasado; incluye la revisión de
      backfills: `UserOwnedBackfillMigration` y `AuthDataMigration` tampoco cubrían
      amiibos/funkos, `MongoIndexMigration` ya los tenía)
- [x] #503 → PR **#504** (flaky de `windowsCache_isBounded`: 0/2 en rojo sobre `main` antes,
      3/3 en verde tras el bucle con deadline)

Línea base verificada en `main` (`f1d0748`): **1287 tests, LINE ≥ 0.80, BUILD SUCCESS**.

### Bloqueado (manual, en su terminal — único pendiente)
- [ ] 1. #480 → rama `fix/ci-workflow` rebasada sobre `main` (586d3af) y verificada en verde
      (1287 tests). Falta ampliar el scope del token (`gh auth refresh -s workflow` → push →
      `gh pr create` contra `main`); pasos exactos y cuerpo del PR en
      `docs/superpowers/plans/480-push-manual.md` y `pr-480-body.md`.

### Cerrado
- Issue **#503** → **PR #504, mergeado**. Test flaky `RateLimitFilterTest.windowsCache_isBounded`
  reproducido 4 veces (47, 42, 43, 28 sobre umbral 20) —dos de ellas sobre `main`—; tras el
  arreglo (bucle con deadline de 5 s, sin tocar producción ni relajar el umbral), 3/3 completas
  en verde. Línea base de `main` ahora determinista.

## Hallazgos fuera de plan

- **#495 (feature, no auditoría)** → **PR #496** abierto contra `main`, verde con 1238 tests.
- **`main` no compila** desde el PR #494: el commit `4969997` borró las constantes
  `USER_STREAMS_LIST` y `USER_PREFERENCES_DETAIL` de `CacheConfig` dejando sus usos. Issue
  **#497** + hotfix **PR #498** (`fix/cacheconfig-main`), verde con 1187 tests. Hasta que se
  fusione, todo lo nuevo va apilado sobre #498 (GitHub reorienta los PRs a `main` al mergear).
- **CI en `main` no llega a compilar**: los runs fallan a nivel de fichero de workflow (el
  propio #480), por eso la rotura de `main` pasó inadvertida.
- **Test flaky preexistente**: `RateLimitFilterTest.windowsCache_isBounded` (expulsión asíncrona
  de Caffeine; 3/3 en aislamiento). Reproducido en las ejecuciones completas de #498 (47>20)
  y #502 (42>20). → **issue #503** abierto.
