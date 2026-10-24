Closes #480

## Qué hace

La CI del repo **no se ejecutaba nunca**: `.github/workflows/checks.yml` no tenía bloque `on:`, así que GitHub Actions lo descartaba sin más. Por eso la rotura de `main` del PR #494 (issue #497) llegó sin que ningún run lo avisara.

Además, cuando se llegaba a ejecutar manualmente, el build fallaba por dos motivos más:

- llamaba a `./mvnw`, pero **el repo no incluye el wrapper de Maven** (ver `Dockerfile`);
- `JWT_SECRET` apuntaba a `secrets.GOOGLE_BOOKS_API_KEY`: si la clave de Google Books no existía como secreto de GitHub, el contexto de Spring no arrancaba y **todos** los tests fallaban.

## Cambios

```diff
+ on:
+   push:
+     branches: [main]
+   pull_request:
+     branches: [main]
+   workflow_dispatch:
+
+ permissions:
+   contents: read
+
  jobs:
    build:
      runs-on: ubuntu-latest
      env:
-       JWT_SECRET: ${{ secrets.GOOGLE_BOOKS_API_KEY }}
+       # Secreto de firma JWT SOLO para CI (>= 256 bits para que arranque Spring)
+       JWT_SECRET: Q0lfT05MWV9URVNUX1NQQUNJTkdfU0VDUkVUX0ZPUl9KV1RfMjAyNg==
+       # APIs externas: se mockean en los tests, se dejan vacías a propósito
+       GOOGLE_BOOKS_API_KEY: ""
+       TMDB_API_KEY: ""
+       RAWG_API_KEY: ""
+       STEAM_API_KEY: ""
+       BGG_AUTH_TOKEN: ""
+       CATBOX_USERHASH: ""
      steps:
        - uses: actions/checkout@v4
        - uses: actions/setup-java@v4
          with:
            java-version: '25'
            distribution: 'temurin'
+           cache: maven
-       - name: Build
-         run: ./mvnw clean package
+       - name: Build, tests y cobertura
+         run: mvn -B clean verify
```

- **`mvn -B clean verify`** en lugar de `clean package`: ejecuta los tests **y** el gate de JaCoCo (LINE ≥ 0.80), que es el requisito real de los PRs; `package` no lo aplicaba.
- **Secretos propios, uno por dependencia y sin valor por defecto**: el workflow ya no lee el secreto de Google Books para firmar JWTs ni depende de que existan claves externas que los tests mockean.
- **`cache: maven`** + `permissions: contents: read` (principio de mínimo privilegio).

## Verificación

- El contenido del workflow es el mínimo necesario para que el runner tenga lo que la aplicación necesita para arrancar en pruebas: `app.jwt.secret` se resuelve de `${JWT_SECRET:}` (vacío por defecto) y `JwtService` valida el secreto al arrancar — si falta o es corto, lanza `IllegalStateException` y **todos** los tests fallan, aunque ninguno firme un token real. Por eso el workflow fija uno ≥ 256 bits solo para CI.
- `mvn -B clean verify` en local sobre `main` (`f1d0748`, tras fusionar #501/#502/#504): **1287 tests, LINE ≥ 0.80** — mismo comando y mismo árbol (rama + workflow) que ejecutará CI.

## Nota de despliegue

- La rama está rebasada sobre el `main` actual (`f1d0748`), así que el diff del PR es **solo**
  `.github/workflows/checks.yml`.
- La primera ejecución en `main` será la que confirme el estado real de la CI — hasta ahora
  ningún run había llegado a Maven. Si algo falla, será visible en el propio PR.
- El test flaky `RateLimitFilterTest.windowsCache_isBounded` (#503) ya está **arreglado en
  `main`** (PR #504): el run de este PR parte del árbol verde (1287/1287 verificado en local).
