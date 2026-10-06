# Diseño: Importación asíncrona de mazos Commander (issue #353)

- **Fecha:** 2026-10-05
- **Issue:** #353 — Fase 23: Importación de mazos Commander existentes (Deck Import)
- **Rama:** `feat/353-deck-import`
- **Estado:** aprobado para planificar

## 1. Objetivo

Permitir poblar un mazo Commander existente desde una lista de cartas en texto plano,
JSON o CSV, sin tener que añadir cartas una a una con `POST /api/v1/decks/{id}/cards`.

La lista tiene ~100 cartas y resolver cada nombre contra Scryfall es una petición HTTP
con un rate limit de 100 ms entre llamadas: el import completo tarda entre 10 y 60
segundos. Por eso **la respuesta es asíncrona**: el `POST` devuelve `202` con un `jobId`
enseguida y el cliente recoge el resultado consultando ese trabajo.

Criterio de éxito: un usuario pega un `.txt` exportado de Arena/MTGO sobre un mazo
existente y, a los pocos segundos, tiene las 99 cartas con su portada, mana cost,
identidad de color y estado de "en mi colección", el comandante detectado, y un informe
del `DeckValidator` con lo que falta o sobra. Las cartas que no se pudieron resolver
vienen listadas, con candidatos, para que el frontend pida confirmación.

## 2. Decisiones tomadas

| Decisión | Elección | Por qué |
| --- | --- | --- |
| Dónde vive el estado del trabajo | **En memoria** (Caffeine) | Decisión del usuario. Se pierde al reiniciar; el `GET` de un job caducado responde `404` con un mensaje que lo explica. |
| Cómo se recoge el resultado | **`202` + `jobId` + `Location`, poll por `jobId`** | Permite importar dos veces el mismo mazo sin pisar el resultado anterior y que el frontend recargue la página y siga. |
| Cartas ambiguas / no encontradas | **Se guarda lo resuelto y se lista lo pendiente** | El mazo se guarda con lo que se pudo resolver; lo demás vuelve con candidatos para que el usuario confirme. |
| Formatos de entrada | **`.txt` + JSON + CSV** | Alcance completo de la issue #353. |
| Comandante | **Se detecta del archivo y se resuelven sus colores** | El `DeckValidator` compara la identidad de color de cada carta con la del comandante, y `DeckStatus.COMPLETE` exige comandante. Sin esto el mazo importado siempre queda en `DRAFT`. |
| Mecanismo async | **Bean separado con `@Async` + executor dedicado** | `@Async` sobre otro bean es la vía idiomática y testeable (el worker se puede llamar de forma síncrona en los tests). Auto-invocación desde el mismo bean se ignoraría en silencio. |
| Escritura del mazo | **Una sola, al final** | 100 `save()` del documento entero es trabajo inútil. Nada de mazos a medias. |

Descartados a propósito: SSE en vez de poll (superficie y complejidad para un solo caso),
jobs persistidos en Mongo (`PENDING` que sobrevive a un reinicio sin dueño que lo reanude),
import por URL externa (Moxfield/Deckbox), cancelación de jobs e import por lotes.

## 3. Contrato HTTP

Todos los endpoints cuelgan de `/api/v1/decks/{id}` y los añade `DeckController`.

### 3.1 `POST /api/v1/decks/{id}/imports` —multipart

Parámetro `file` (`MultipartFile`). Formatos aceptados: `.txt`, `.json`, `.csv`.

```http
POST /api/v1/decks/6620f1/imports?mode=replace
Content-Type: multipart/form-data

--> 202 Accepted
Location: /api/v1/decks/6620f1/imports/9f2c1d4e-0c7a-4d5b-9a1f-3b2e6c8d4a70
Content-Type: application/json

{ "jobId": "9f2c1d4e-0c7a-4d5b-9a1f-3b2e6c8d4a70",
  "status": "PENDING",
  "statusUrl": "/api/v1/decks/6620f1/imports/9f2c1d4e-0c7a-4d5b-9a1f-3b2e6c8d4a70" }
```

### 3.2 `POST /api/v1/decks/{id}/imports/text` — `text/plain`

El cuerpo **es** la lista. Para pegar en lugar de subir archivo. Mismo `202`.
El formato se deduce del contenido (empieza por `{` o `[` → JSON; segunda línea con
comas y cabecera conocida → CSV; resto → texto MTGO).

### 3.3 `GET /api/v1/decks/{id}/imports/{jobId}`

`200` con el estado del trabajo. `404` si el job no existe o caducó. `403` si el mazo
no es del usuario (se valida propiedad aunque `GET /api/v1/**` sea público, igual que
hace `addCard` con `OwnershipValidator`).

```json
{
  "jobId": "9f2c1d4e-0c7a-4d5b-9a1f-3b2e6c8d4a70",
  "status": "COMPLETED",
  "phase": "DONE",
  "format": "TXT",
  "mode": "REPLACE",
  "progress": { "total": 101, "processed": 101, "resolved": 98, "unresolved": 3, "sideboardIgnored": 15 },
  "commander": { "name": "Atraxa, Grand Unifier", "colors": ["G", "U"] },
  "validation": { "status": "DRAFT", "reasons": ["El mazo no llega a 99 cartas"] },
  "unresolved": [
    {
      "line": 42,
      "raw": "1 Not A Real Card (XXX) 7",
      "quantity": 1,
      "name": "Not A Real Card",
      "reason": "NOT_FOUND",
      "candidates": [
        { "scryfallId": "9c2f0b1a-...", "name": "Nok, the Sky-Treader",
          "setName": "Foundations", "imageUrl": "https://cards.scryfall.io/..." }
      ]
    }
  ],
  "deck": { "id": "6620f1", "name": "Atraxagift", "commander": "Atraxa, Grand Unifier", "cards": [ "..." ] },
  "error": null,
  "createdAt": "2026-10-05T10:00:00Z",
  "updatedAt": "2026-10-05T10:00:14Z",
  "completedAt": "2026-10-05T10:00:14Z"
}
```

El job **no** guarda una copia del mazo: solo metadatos. El controlador compone la
respuesta cargando el mazo de Mongo, de forma que Mongo es la única fuente de verdad
y el payload del job no duplica 100 cartas.

### 3.4 Parámetros

| Parámetro | Valores | Por defecto | Nota |
| --- | --- | --- | --- |
| `format` | `txt`, `json`, `csv` | deducido | Si se pasa y no coincide con el contenido, `400`. |
| `mode` | `replace`, `merge` | `replace` | `replace` deja el mazo con exactamente la lista importada, lo que hace el reintento idempotente. `merge` suma cantidades como hace `addCard`. |

### 3.5 Validaciones síncronas (antes de crear el job)

Se rechazan en el `POST` con `400`, sin llegar a crear nada:

- archivo vacío o solo con líneas en blanco;
- más de `deck.import.max-entries` (500) líneas de cartas;
- formato explícito que no corresponde al contenido;
- más de `deck.import.max-file-size` bytes (5 MB, coherente con `spring.servlet.multipart`).

## 4. Modelo de dominio

```java
public record DeckListEntry(
        int line,            // número de línea en el archivo, 1-based, para reportar
        int quantity,
        String name,
        String setCode,      // opcional, no se usa para resolver
        String collectorNumber,
        boolean commander    // venía en la zona COMMANDER
) {}

public record UnresolvedCardEntry(
        int line,
        String raw,
        int quantity,
        String name,
        UnresolvedReason reason,           // NOT_FOUND | AMBIGUOUS | UPSTREAM_ERROR
        List<MagicCardSearchResult> candidates
) {}

public enum UnresolvedReason { NOT_FOUND, AMBIGUOUS, UPSTREAM_ERROR }
public enum DeckImportFormat { TXT, JSON, CSV }
public enum DeckImportMode { REPLACE, MERGE }
public enum DeckImportStatus { PENDING, RUNNING, COMPLETED, FAILED }
public enum DeckImportPhase { PARSING, RESOLVING, SAVING, DONE }
```

`DeckImportJob` es un record inmutable:

```java
public record DeckImportJob(
        String jobId,
        String deckId,
        String ownerId,
        DeckImportFormat format,
        DeckImportMode mode,
        DeckImportStatus status,
        DeckImportPhase phase,
        int total,
        int processed,
        int resolved,
        int sideboardIgnored,
        String commanderName,
        List<String> commanderColors,
        List<UnresolvedCardEntry> unresolved,
        DeckStatusReport validation,
        String error,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {}
```

El registro en memoria publica **copias**: el worker reescribe el job entero con cada
avance (`cache.put(jobId, job.copy(...))`), así el `GET` nunca observa un estado a medias
sin necesidad de bloqueos.

`MagicCardSearchResult` (que ya existe) tiene justo lo necesario para los candidatos:
`scryfallId`, `name`, `manaCost`, `type`, `setName`, `imageUrl`, `colorIdentity`, `priceUsd`.

## 5. Ciclo de vida

```
POST ──(400 si no vale)──▶ crea job PENDING ──▶ 202 + Location
                                                 │
                                          worker (@Async)
                                                 ▼
                              PARSING ──▶ RESOLVING (n/total) ──▶ SAVING ──▶ DONE
                                 │              │                      │
                            parse error    UPSTREAM_ERROR          save error
                                 └──────────────┴──────────────────────┘
                                                ▼
                                             FAILED
```

- `PENDING`: job creado, el worker todavía no ha empezado.
- `RUNNING`: dentro del worker, con `phase` diciendo en qué paso va.
- `COMPLETED`: terminó, se guardó el mazo y hay informe del validador. Puede haber
  entradas en `unresolved` (NO ENCONTRADA o AMBIGUA) — eso no es un fallo.
- `FAILED`: la importación no pudo completarse. **El mazo no se toca.**

## 6. Parsers

Puerto `domain/port/out/DeckListParser.java`:

```java
public interface DeckListParser {
    DeckImportFormat format();
    ParsedDeckList parse(String content);   // record ParsedDeckList(List<DeckListEntry> entries, int sideboardIgnored)
}
```

`ParsedDeckList` lleva los errores como excepción de aplicación
(`DeckListParseException` → `400`/`FAILED` con mensaje claro), no como lista vacía:
un archivo vacío o corrupto es un fallo, no "un mazo sin cartas".

Los tres adapters viven en `infrastructure/adapter/in/decklist/`.

### 6.1 Texto plano (formato MTGO / Arena)

```
// comment
COMMANDER
1 Atraxa, Grand Unifier (ONE) 32

DECK
4 Sol Ring (NEO) 269
2 Plains
1 Not A Real Card (XXX) 7 *F*  $1.23

SIDEBOARD
2 Negate (M21) 68
```

Línea de carta (regex):

```
^(?<qty>\d+)\s+(?<name>.+?)(?:\s+\((?<set>[A-Z0-9]{2,6})\))?(?:\s+(?<num>\d+))?(?:\s+\*(?<foil>F|NF)\*)?(?:\s+\$(?<price>[\d.,]+))?$
```

- Las cabeceras `COMMANDER`, `DECK` y `SIDEBOARD` (en línea propia, sin distinguir mayúsculas)
  marcan la zona actual. `COMMANDER` → `commander = true` en las entradas de esa zona.
- Las líneas que empiezan por `//` son comentarios o cartas de sideboard: se ignoran.
- El **sideboard se ignora** y se cuenta en `sideboardIgnored`. Motivo: `DeckValidator`
  suma **todas** las cartas de `deck.cards` contra el límite de 100 y un sideboard lo
  rompería siempre.
- El comandante **no** entra en `deck.cards`: va a `deck.commander`, que es como
  `DeckValidator.evaluate` lo espera para poder marcar `COMPLETE` (99 cartas + comandante).
- Blank lines se ignoran.

### 6.2 JSON

Tres formas aceptadas, para no obligar al cliente a un esquema único:

```json
[ { "name": "Sol Ring", "quantity": 4 }, { "name": "Atraxa, Grand Unifier", "quantity": 1, "isCommander": true } ]
```

```json
{ "cards": [ { "name": "Sol Ring", "quantity": 4 } ], "commander": "Atraxa, Grand Unifier" }
```

```json
{ "deck": [ { "name": "Sol Ring", "quantity": 4, "set": "NEO", "number": "269" } ] }
```

Claves admitidas por carta: `name` (obligatoria), `quantity` (por defecto 1),
`set`/`setCode`, `number`/`collectorNumber`, `isCommander`. `commander` puede ser un
string o un objeto carta. Cualquier otra clave se ignora (el mismo criterio que los
records de Scryfall en `MagicCardMapper`).

### 6.3 CSV

Cabecera obligatoria con las columnas en cualquier orden y separador `,` o `;`:

```csv
quantity,name,set,number
4,"Sol Ring",NEO,269
1,"Atraxa, Grand Unifier",ONE,32
```

Nombres entre comillas con comillas dobles escapadas (`""`). Sin dependencias nuevas:
split por líneas, detección del separador contando fuera de comillas, y un parser
pequeño de campos entrecomillados.

## 7. Resolución contra Scryfall

Método nuevo en `ExternalMagicCardCatalogClient`:

```java
List<MagicCardSearchResult> searchByNameExact(String name);   // q=name:"X" + unique=oracle
List<MagicCardSearchResult> searchSuggestions(String name); // q=X        + unique=oracle, máx. 5
```

`unique=oracle` es lo que colapsa las reimpresiones y deja solo los nombres realmente
distintos. Scryfall ignora en silencio los parámetros que no entiende, así que si
`unique` no llegara a la petición, "Sol Ring" devolvería sus 20 reimpresiones y cada
línea del mazo parecería ambigua.

Normalización de nombres (`Normalizer` NFD + quitar diacríticos + minúsculas + colapsar
espacios + quitar puntuación final), para que "Kongou, Keeper of the Deep" y
"Kóngou, Keeper of the Deep" sean la misma carta.

Algoritmo por nombre único:

1. Si el nombre es una **tierra básica** (las 19 de una lista cerrada, en inglés)
   → resuelta **localmente**, sin llamar a Scryfall. Motivo doble: son las únicas cartas
   con cantidad > 1 en un mazo Commander, y el validador solo las exonera del singleton
   si el `DeckCard` trae `typeLine` con "Basic Land".

   > **Corrección (2026-10-06, tras la revisión del PR):** de las 19 solo cinco son tierras
   > básicas de verdad (Plains, Island, Swamp, Mountain, Forest). Wasteland, Tundra,
   > Underground Sea, Bayou, Dryad Arbor, Llanowar Elves... son no básicas, y guardarlas con
   > `typeLine` "Basic Land" de mentira hacía que `DeckValidator` las exentara de singleton
   > (cuatro Wastelands válidos en Commander). La lista local se redujo a las cinco; el resto
   > va a Scryfall como cualquier otra carta, que es donde sale su tipo real.
2. `searchByNameExact(nombre)`:
   - 1 candidato cuyo nombre normalizado coincide → **resuelta**.
   - >1 candidatos → `AMBIGUOUS` con la lista.
   - 0 resultados → paso 3.
3. `searchSuggestions(nombre)` (búsqueda amplia, máximo 5):
   - 0 candidatos → `NOT_FOUND` sin sugerencias.
   - 1 candidato cuyo nombre normalizado coincide (cubre diacríticos y mayúsculas) → **resuelta**.
   - en otro caso → `NOT_FOUND` **con las sugerencias**, que es lo que permite al
     frontend preguntar "¿querías decir esto?".

Optimizaciones y gastos:

- Se resuelve **una vez por nombre normalizado**, no por línea: 100 cartas → ~80 llamadas.
- Las búsquedas van cacheadas en `magicSearch` (`@Cacheable`), así que un mazo con
  cartas populares sale gratis y un segundo import del mismo mazo no pega nada a Scryfall.
- El rate limiter de `ScryfallClient` (`pace()`, 100 ms, `synchronized`) serializa las
  peticiones también entre dos importaciones simultáneas. Correcto: no se quiere más.
- Un nombre en español ("Llanuras") no resuelve contra Scryfall, que indexa en inglés:
  aparecerá en `unresolved` con sugerencias. Es el comportamiento honesto.

## 8. Guardado del mazo (fase `SAVING`)

Una sola escritura. Antes de resolver nada se carga el mazo y se comprueba la propiedad
(con el `userId` recibido como parámetro, nunca desde el `SecurityContext`, que no se
propaga al hilo async).

1. **Comandante**: si el archivo lo traía y no se resolvió, no se toca el que hubiera.
   Si se resolvió, `deck.setCommander(nombre)` y `deck.setCommanderColors(colorIdentity)`.
2. **Lista de cartas**:
   - `REPLACE` → `deck.setCards(listaResuelta)`.
   - `MERGE` → arranca de las cartas actuales y suma cantidades por `scryfallId`, igual
     que `DeckService.addCard`.
3. **`inCollection` / `isProxy`**: `inCollection = true` si el usuario tiene esa carta.
   Se resuelve con **una sola consulta**: `MagicCardRepository.findNamesByOwnerId(ownerId)`
   (proyección de `name` sobre el índice `ownerId`, límite 5000) y un `Set` de nombres
   normalizados en memoria.

   Ojo: `DeckService.addCard` decide `inCollection` buscando por nombre **entre todos los
   usuarios** y con una expresión regular sin índice (`MagicCardPersistenceAdapter.buildQuery`
   compila `Pattern.quote` sobre `name`, y `name` no está indexado: `auto-index-creation=false`
   y `MongoIndexMigration` solo crea el índice de `ownerId`). Hacer eso 80 veces en una
   importación es un escaneo por nombre. La versión del import es *scope del dueño*:
   más rápida y más fiel a lo que significa "en mi colección". Es una diferencia
   deliberada respecto a `addCard`, no un descuido.
4. `deck.setUpdatedAt(Instant.now())`, `deckRepository.save(deck)`, y
   `@CacheEvict(cacheNames = {"deckDetail", "deckList"}, allEntries = true)`
   (los mazos están cacheados; si no, el `GET` siguiente devolvería el mazo viejo).
5. `DeckValidator` sobre el mazo guardado → `DeckStatusReport` al job → `COMPLETED`.

Si algo falla antes del `save`, el mazo no se ha tocado: no hay estado intermedio posible.

## 9. Errores

Regla que viene de #451: **un fallo de Scryfall no puede disfrazarse de resultado vacío**.

| Situación | Efecto en el job | Mazo |
| --- | --- | --- |
| Nombre no encontrado (Scryfall responde 200 con 0) | `unresolved` con `NOT_FOUND` y sugerencias | se guarda lo demás |
| Varios candidatos | `unresolved` con `AMBIGUOUS` y candidatos | se guarda lo demás |
| Scryfall 5xx o timeout tras los 4 intentos | `unresolved` con `UPSTREAM_ERROR`, job `FAILED`, `error` con el último error | **intacto** |
| No se pudo parsear el archivo | `FAILED`, `error` con el motivo | intacto |
| Mazo borrado o cambiado de dueño a mitad del trabajo | `FAILED`, `ForbiddenException`/`DeckNotFoundException` | intacto |
| Job caducado o reinicio de la app | `404` en el `GET` con mensaje explicativo | el que hubiera |

Un solo `UPSTREAM_ERROR` tumba el job entero, aunque haya 98 cartas resueltas: es
preferible decir "Scryfall falló, no he guardado nada" que devolver un mazo con dos
huecos que el usuario no sabe si son errores suyos.

## 10. Registro en memoria

```java
@Bean
Cache<String, DeckImportJob> deckImportJobs() {
    return Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(200)
            .build();
}
```

No entra en `CacheConfig.CACHE_NAMES` porque ese registro crea las cachés de Spring
Cache Manager, y aquí lo que se necesita es un `Cache` de Caffeine usado como mapa de
jobs, consultado directamente por el service. El bean va en `AsyncConfig` (o en su
propio `DeckImportJobStore`) con `@Qualifier` explícito para que ninguna inyección
ambigua de `Cache<String, ?>` compile sin querer.

Ejecutor:

```java
@Bean("deckImportExecutor")
ThreadPoolTaskExecutor deckImportExecutor() { core 2, max 4, queue 20; }
```

Dos hilos porque el rate limiter serializa las peticiones de Scryfall a 100 ms: más
hilos no aportan nada y solo amontonarían jobs en la cola. Con 20 de cola, un pico de
importaciones no se rechaza con `TaskRejectedException` sin avisar; si se supera, el
`POST` devuelve `429`.

## 11. Estructura de ficheros

**Nuevos — dominio**

- `domain/model/DeckListEntry.java`
- `domain/model/UnresolvedCardEntry.java`
- `domain/model/DeckImportJob.java`
- `domain/model/DeckImportFormat.java`, `DeckImportMode.java`, `DeckImportStatus.java`, `DeckImportPhase.java`, `UnresolvedReason.java`
- `domain/model/ParsedDeckList.java`

**Nuevos — puertos y aplicación**

- `domain/port/in/DeckImportUseCase.java` — `startImport(...)`, `findJob(deckId, jobId, userId)`
- `domain/port/out/DeckListParser.java`
- `application/service/DeckImportService.java` — valida, crea el job, dispara el worker
- `application/service/DeckImportWorker.java` — `@Async("deckImportExecutor")`, **otro bean**
- `application/service/DeckCardFactory.java` — mapeo a `DeckCard` compartido
- `application/service/DeckImportJobStore.java` — envuelve la `Cache` de Caffeine
- `application/exception/DeckListParseException.java`, `DeckImportNotFoundException.java`

**Nuevos — infraestructura**

- `infrastructure/adapter/in/decklist/MtgoTextDeckListParser.java`, `JsonDeckListParser.java`, `CsvDeckListParser.java`, `DeckNameNormalizer.java`
- `infrastructure/config/AsyncConfig.java` — `@EnableAsync` + `deckImportExecutor` + `deckImportJobs`

**Nuevos — DTOs** (`infrastructure/adapter/in/web/dto/`)

`DeckImportAcceptedResponse`, `DeckImportJobResponse`, `DeckImportProgressResponse`,
`DeckImportUnresolvedResponse`, `DeckImportCandidateResponse`, `DeckImportCommanderResponse`.

**Tocados**

- `domain/port/out/ExternalMagicCardCatalogClient.java` — `searchByNameExact`, `searchSuggestions`
- `domain/port/out/MagicCardRepository.java` — `findNamesByOwnerId`
- `infrastructure/adapter/out/scryfall/ScryfallClient.java` — las dos búsquedas con `unique=oracle`
- `infrastructure/adapter/out/persistence/MagicCardPersistenceAdapter.java` — proyección de nombres
- `application/service/DeckService.java` — `addCard` pasa a usar `DeckCardFactory` (sin cambio de comportamiento)
- `infrastructure/adapter/in/web/DeckController.java` — 3 endpoints
- `infrastructure/adapter/in/web/GlobalExceptionHandler.java` — `DeckListParseException` → 400, `DeckImportNotFoundException` → 404, `TaskRejectedException` → 429
- `src/main/resources/application.properties` — bloque `deck.import.*`
- `AGENTS.md` — sección nueva

`pom.xml` no se toca: Caffeine y el soporte de multipart ya están.

## 12. Tests

Puerta de cobertura: `mvn verify` con línea ≥ 0.80 (JaCoCo, la misma regla del resto).

**Parsers (unitarios, sin Spring)**

- MTGO: zonas `COMMANDER`/`DECK`/`SIDEBOARD`, líneas con set y collector number,
  marca de foil, precio, comentarios `//`, líneas en blanco, commanders duplicados,
  fichero vacío → `DeckListParseException`, líneas que no son cartas → se ignoran.
- JSON: las tres formas, `isCommander`, clave `commander` como string y como objeto,
  campos ausentes, array vacío → excepción, JSON malformado → excepción.
- CSV: `,` y `;`, cabeceras en cualquier orden, campos entrecomillados con comas y
  comillas dobles escapadas, cabecera obligatoria ausente → excepción.
- `DeckNameNormalizer`: diacríticos, mayúsculas, espacios repetidos, puntuación.

**Worker (unitario, todo mockeado)**

- Un mazo con 3 cartas resuelve 2 y deja 1 en `unresolved` → `COMPLETED` y un **único**
  `deckRepository.save()` con 2 cartas.
- `REPLACE` ignora las cartas que ya había; `MERGE` suma cantidades.
- Tierras básicas: no se llama a Scryfall y su `DeckCard` trae `typeLine` "Basic Land".
- Un `RestClientResponseException` de Scryfall → job `FAILED`, `unresolved` con
  `UPSTREAM_ERROR` y **cero llamadas** a `deckRepository.save()`.
- El comandante resuelto rellena `commanderColors`.
- El job se actualiza en caché en cada avance (progreso observable).
- El `userId` se usa tal cual llega, sin tocar el `SecurityContext`.

**Service (unitario)**

- Archivo vacío / demasiadas líneas / formato inconsistente → excepción, **sin** job creado.
- Job creado en `PENDING` con `format` y `mode` correctos, y `statusUrl` bien formado.
- `findJob` de un id desconocido → `DeckImportNotFoundException`.
- `findJob` de un mazo ajeno → `ForbiddenException`.

**Controller (`@SpringBootTest` + MockMvc, como `DeckControllerTest`)**

- `POST` multipart → `202`, header `Location` y body con `jobId`.
- `POST` `text/plain` → `202`.
- `POST` con archivo vacío → `400`.
- `GET` de un job `COMPLETED` → `200` con `deck`, `validation`, `unresolved` y `progress`.
- `GET` de un job inexistente → `404`.
- `GET` sobre un mazo ajeno → `403`.

**Regresión**

- `DeckServiceTest` sigue verde tras el refactor de `DeckCardFactory`.
- `MagicCardPersistenceAdapterTest` cubre la proyección de nombres.

## 13. Fuera de alcance

- Import por URL externa (Moxfield, Deckbox, Scryfall lists).
- SSE o WebSocket en vez de poll.
- Jobs persistidos en Mongo, cola de trabajos, reintento automático, cancelación.
- Progreso incremental visible en el mazo mientras se importa (solo en el job).
- Importar.sideboard, tokens de Commander aparte del mazo principal, o mazos de
  formato distinto de Commander (el validador solo conoce las reglas de Commander).
- La UI: es la issue #462 del frontend.

## 14. Riesgos

| Riesgo | Mitigación |
| --- | --- |
| Un import de 100 cartas dura 10-60 s y se pierde al reiniciar | Es la decisión tomada (estado en memoria); el `404` lo dice claramente y el import es idempotente en modo `REPLACE`, así que reenviar es seguro |
| 80 llamadas a Scryfall por import | Deduplicación por nombre, saltando básicas, caché `magicSearch` y rate limiter existente |
| Un `.txt` con 300 cartas distintas dispara el rate limit | El rate limiter ya espera 100 ms entre llamadas; los reintentos por 5xx ya existen |
| Los nombres en español no resuelven | Van a `unresolved` con sugerencias; el frontend decide |
| El mazo se pisa si alguien edita el mazo durante el import | `REPLACE` es explícito en la petición; el import no bloquea el mazo ni el `PUT` |
| `DeckCard` con datos incompletos de `MagicCardSearchResult` | `MagicCardSearchResult` tiene `name`, `manaCost`, `type`, `colorIdentity`, `imageUrl` y `scryfallId`: todo lo que `DeckCard` necesita |