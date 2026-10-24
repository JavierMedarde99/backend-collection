# #480 — comandos manuales (push pendiente por scope `workflow`)

La rama `fix/ci-workflow` está lista y rebasada sobre el `main` actual (`f1d0748`, con #501/#502/#504
fusionados), así que el PR queda reducido al único fichero `.github/workflows/checks.yml`.
Todo lo demás está hecho y publicado; esto es lo único que falta porque el token OAuth actual
tiene los scopes `gist, read:org, repo` — **falta `workflow`**, y sin él `git push` rechaza
cualquier fichero bajo `.github/workflows/`.

`gh` está configurado como credential helper de git (`git config credential.helper = gh auth git-credential`),
así que basta con ampliar el token de `gh`: no hace falta PAT.

## 1. Ampliar el scope del token

```bash
gh auth refresh -s workflow
```

Te imprime un código de 8 caracteres y te pide abrir https://github.com/login/device e introducirlo.
Al terminar, `gh auth status` debe mostrar `Token scopes: 'gist', 'read:org', 'repo', 'workflow'`.

## 2. Push de la rama

```bash
cd /home/javi/orca/workspaces/backend-collection/brill
git push -u origin fix/ci-workflow
```

## 3. Abrir el PR

```bash
cd /home/javi/orca/workspaces/backend-collection/brill
gh pr create \
  --repo JavierMedarde99/backend-collection \
  --base main \
  --head fix/ci-workflow \
  --title "ci(checks): activar el workflow, mvn clean verify y secretos propios (#480)" \
  --body-file docs/superpowers/plans/pr-480-body.md
```

Base = **`main`** directamente; la rama está rebasada sobre `main` (`f1d0748`), así que el diff
del PR queda reducido al único fichero `.github/workflows/checks.yml`.

## Qué se espera en la primera ejecución

- El workflow usa `mvn -B clean verify` sobre **1287 tests** — verificado en verde en local con
  el árbol exacto de CI (rama rebasada sobre `main` + el propio workflow).
- El test flaky `RateLimitFilterTest.windowsCache_isBounded` (#503) ya está **arreglado en
  `main`** vía PR #504, así que la primera ejecución no parte con ese riesgo de rojo espurio.

## Estado del resto (nada más pendiente)

| Issue | PR | Estado |
|-------|----|--------|
| #497 (main no compila) | #498 | **mergeado** |
| #495 (expansión de board games) | #496 | **mergeado** |
| #481 (imágenes) | #499 | **mergeado** |
| #477 (deleteAccount + amiibos/funkos) | #500 | **mergeado** |
| #487 (borrado en bloque) | #501 | **mergeado** |
| #484 (propagación ownerName) | #502 | **mergeado** |
| #503 (test flaky) | #504 | **mergeado** |
| #480 (CI) | — | **este push** (lo único que falta) |
