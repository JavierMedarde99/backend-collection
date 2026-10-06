# Importación asíncrona de mazos Commander — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Endpoint que importa un mazo Commander completo desde un `.txt` (MTGO/Arena), JSON o CSV, devolviendo `202` + `jobId` y resolviendo los nombres contra Scryfall en segundo plano hasta dejar el mazo guardado con su informe de validación.

**Architecture:** Un `POST` valida lo barato y crea un job inmutable en un registro Caffeine; un worker en otro bean con `@Async` parsea, resuelve cada nombre único contra Scryfall, y hace **una sola** escritura del mazo al final. El cliente hace poll de `GET /imports/{jobId}`, que compone el estado del job con el mazo recién guardado (Mongo es la única fuente de verdad del mazo).

**Tech Stack:** Java 25, Spring Boot 4.1.1, Spring Data MongoDB, Caffeine (ya dependencia), Lombok, RestClient, MockMvc + `@MockitoBean`, MockWebServer, JUnit 5 + AssertJ.

**Spec:** `docs/superpowers/specs/2026-10-05-deck-import-design.md` — léelo antes de empezar; este plan argue desde él.

## Global Constraints

- Java 25 + Lombok: el `maven-compiler-plugin` ya fuerza Lombok por `annotationProcessorPaths`. No tocar esa config.
- `mvn verify` es la puerta: tests + JaCoCo con **línea ≥ 0.80**. `mvn test` no basta.
- Ejecutar Maven **sin red**: `mvn -o ...`. No existe `mvnw`.
- Clave de Mongo: `spring.mongodb.uri`. En tests: `spring.data.mongodb.auto-index-creation=false` y `app.boardgame-status-migration.enabled=false`.
- Imports de Boot 4 en tests: `@AutoConfigureMockMvc` → `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`; `@MockitoBean` → `org.springframework.test.context.bean.override.mockito.MockitoBean`.
- tests de controller: `@SpringBootTest` + `@AutoConfigureMockMvc`, y mockear `OwnerResolver` (si no, el contexto revienta), `DeckRepository`, `MagicCardRepository` y `ScryfallClient`.
- Limpiar cachés entre tests de controller: `CacheTestSupport.clearAll(cacheManager)` en `@BeforeEach`, como hace `DeckControllerTest`.
- Solo hay **un** `@EnableMongoAuditing` (en `MongoAuditConfig`). No añadir otro.
- Hexagonal: `domain/` no importa `infrastructure/`. `application/` solo importa `infrastructure/` en `MovieShowService`, y es excepción, no norma.
- Textos de cara al usuario en español. Comentarios de código en español, como el resto del repo.
- Los records de Scryfall (`MagicCardMapper`) llevan `@JsonIgnoreProperties(ignoreUnknown = true)` **todos** y **no deben tener un segundo constructor**.
- Nada de secretos en `application.properties`.

## Review Focus

Cinco entradas que la spec sugiere pero que ningún test de la spec fija. Cada una tiene su test en la tarea indicada.

1. **Un nombre repetido en dos líneas del archivo** (`1 Sol Ring` en DECK y `1 Sol Ring` en otra zona) debe **sumar** cantidades en un único `DeckCard`, no crear dos entradas: el `DeckValidator` marca "no respeta singleton" ante duplicados. → Task 7, `aggregatesDuplicateNamesBySummingQuantities`.
2. **El comandante repetido también en la zona DECK** no debe contar como carta del mazo: el mazo se queda en 99, no en 100, y así puede alcanzar `COMPLETE`. → Task 7, `commanderListedInMainZoneIsNotCountedAsDeckCard`.
3. **Un nombre en español** ("Llanuras") contra Scryfall, que indexa en inglés: `NOT_FOUND` con sugerencias y job `COMPLETED`, nunca una excepción que tumba la importación. → Task 7, `spanishNameLandsInNotFoundWithSuggestions`.
4. **`GET /decks/{otroDeckId}/imports/{jobId}`**: el `jobId` existe pero pertenece a otro mazo → `404`, no `403`. Un `403` confirmaría que ese id existe y filtra la existencia de importaciones ajenas. → Task 9, `getJob_withJobIdOfAnotherDeck_returns404`.
5. **Reimportar el mismo mazo** con `mode=REPLACE` tras un job `COMPLETED` deja exactamente la lista nueva: reenviar es seguro porque el job se puede perder al reiniciar. → Task 7, `replaceModeOverwritesPreviousCardsOnReimport`.

---

### Task 1: Parser de texto plano MTGO

Domina el formato que exportan Arena, Moxfield y Deckbox, más el modelo mínimo y el puerto que comparten los tres parsers. Sin esto no hay nada que probar.

**Files:**
- Create: `src/main/java/com/wikicollection/domain/model/DeckListEntry.java`
- Create: `src/main/java/com/wikicollection/domain/model/ParsedDeckList.java`
- Create: `src/main/java/com/wikicollection/domain/model/DeckImportFormat.java`
- Create: `src/main/java/com/wikicollection/domain/port/out/DeckListParser.java`
- Create: `src/main/java/com/wikicollection/application/exception/DeckListParseException.java`
- Create: `src/main/java/com/wikicollection/infrastructure/adapter/in/decklist/MtgoTextDeckListParser.java`
- Test: `src/test/java/com/wikicollection/infrastructure/adapter/in/decklist/MtgoTextDeckListParserTest.java`

**Interfaces:**
- Consumes: nada.
- Produces:
  ```java
  public record DeckListEntry(int line, int quantity, String name,
                              String setCode, String collectorNumber, boolean commander) {}
  public record ParsedDeckList(List<DeckListEntry> entries, int sideboardIgnored) {}
  public enum DeckImportFormat { TXT, JSON, CSV }
  public interface DeckListParser {
      DeckImportFormat format();
      ParsedDeckList parse(String content);   // lanza DeckListParseException
  }
  ```

- [ ] **Step 1: Escribe el test que falla**

`MtgoTextDeckListParserTest`, JUnit 5 puro con `assertThat` de AssertJ. Casos, con este contenido de ejemplo:

```
// deck comment
COMMANDER
1 Atraxa, Grand Unifier (ONE) 32

DECK
4 Sol Ring (NEO) 269
2 Plains
1 Not A Real Card (XXX) 7 *F*  $1.23

SIDEBOARD
2 Negate (M21) 68
```

- `parsesCommanderAndDeckZones()`: 4 entradas; la de "Atraxa, Grand Unifier" tiene `commander() == true` y `line() == 3`; "Sol Ring" tiene `quantity() == 4`, `setCode() == "NEO"`, `collectorNumber() == "269"`; "Not A Real Card" conserva `name() == "Not A Real Card"` sin el `(XXX) 7 *F*  $1.23`; `sideboardIgnored() == 1`.
- `ignoresCommentLinesAndBlankLines()`: sin línea `// deck comment` en las entradas.
- `parsesEntryWithoutSetOrNumber()`: `"4 Lightning Bolt"` → `quantity() == 4`, `setCode() == null`, `collectorNumber() == null`.
- `lowercasedZoneHeadersAreRecognized()`: `"commander\n1 Atraxa, Grand Unifier\n\ndeck\n4 Sol Ring"` → 1 comandante, 1 carta, `sideboardIgnored() == 0`.
- `emptyContent_throws()`: `assertThatThrownBy(() -> parser.parse("   \n\n  ")).isInstanceOf(DeckListParseException.class)`.
- `contentWithoutAnyCardLine_throws()`: solo `"COMMANDER\n"` → `DeckListParseException`.
- `throwsWhenQuantityIsNotPositive()`: `"DECK\n0 Sol Ring"` → `DeckListParseException`.

- [ ] **Step 2: Ejecuta el test y comprueba que falla**

Run: `mvn -o -Dtest=MtgoTextDeckListParserTest test`
Expected: FAIL con `cannot find symbol: class MtgoTextDeckListParser`.

- [ ] **Step 3: Implementa lo mínimo**

`DeckImportFormat`, `DeckListEntry` y `ParsedDeckList` como records/enum exactos del bloque Interfaces. `DeckListParseException extends RuntimeException` en `application/exception/` (lo traduce `GlobalExceptionHandler` a 400 en la Task 9).

`MtgoTextDeckListParser` con `@Component` y `format()` devolviendo `DeckImportFormat.TXT`. Constante pública `static final Pattern CARD_LINE`:

```java
Pattern.compile("^(?<qty>\\d+)\\s+(?<name>.+?)(?:\\s+\\((?<set>[A-Z0-9]{2,6})\\))?"
        + "(?:\\s+(?<num>\\d+))?(?:\\s+\\*(?:F|NF)\\*)?(?:\\s+\\$[\\d.,]+)?$")
```

Lógica, línea a línea con su número 1-based: vacía o `//` → se ignora; `COMMANDER`/`DECK`/`SIDEBOARD` en línea propia → cambia la zona actual; en zona `SIDEBOARD` → `sideboardIgnored++` y se ignora; si no casa con `CARD_LINE` → se ignora (una línea que no es carta no es un error del archivo); si casa, `qty < 1` → `DeckListParseException`; si no, `DeckListEntry(line, qty, name.trim(), set, num, zonaActual == COMMANDER)`. El nombre se limpia con `replaceAll("\\s+", " ")`. Si al final `entries.isEmpty()` → `DeckListParseException`.

- [ ] **Step 4: Ejecuta el test y comprueba que pasa**

Run: `mvn -o -Dtest=MtgoTextDeckListParserTest test`
Expected: PASS, 7 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wikicollection/domain/model/DeckListEntry.java \
        src/main/java/com/wikicollection/domain/model/ParsedDeckList.java \
        src/main/java/com/wikicollection/domain/model/DeckImportFormat.java \
        src/main/java/com/wikicollection/domain/port/out/DeckListParser.java \
        src/main/java/com/wikicollection/application/exception/DeckListParseException.java \
        src/main/java/com/wikicollection/infrastructure/adapter/in/decklist/MtgoTextDeckListParser.java \
        src/test/java/com/wikicollection/infrastructure/adapter/in/decklist/MtgoTextDeckListParserTest.java
git commit -m "feat(decks): parser de listas de mazo en texto plano (MTGO)"
```

---

### Task 2: Parsers de JSON y CSV

Mismo puerto, dos formatos más. La detección de separador y el parseo de campos entrecomillados son el trabajo real aquí.

**Files:**
- Create: `src/main/java/com/wikicollection/infrastructure/adapter/in/decklist/JsonDeckListParser.java`
- Create: `src/main/java/com/wikicollection/infrastructure/adapter/in/decklist/CsvDeckListParser.java`
- Create: `src/main/java/com/wikicollection/infrastructure/adapter/in/decklist/DeckListParserRegistry.java`
- Test: `src/test/java/com/wikicollection/infrastructure/adapter/in/decklist/JsonDeckListParserTest.java`
- Test: `src/test/java/com/wikicollection/infrastructure/adapter/in/decklist/CsvDeckListParserTest.java`

**Interfaces:**
- Consumes: `DeckListParser`, `DeckListEntry`, `ParsedDeckList`, `DeckImportFormat`, `DeckListParseException` (Task 1).
- Produces:
  ```java
  @Component
  public class DeckListParserRegistry {
      public DeckListParserRegistry(List<DeckListParser> parsers);   // Spring inyecta los 3
      public DeckListParser forFormat(DeckImportFormat format);       // IllegalArgumentException si no existe
  }
  ```

- [ ] **Step 1: Escribe el test que falla**

`JsonDeckListParserTest`:
- `parsesArrayOfObjects()`: `[{"name":"Sol Ring","quantity":4},{"name":"Plains","quantity":20}]` → 2 entradas con esas cantidades.
- `parsesObjectWithCardsAndCommander()`: `{"cards":[{"name":"Sol Ring","quantity":4}],"commander":"Atraxa, Grand Unifier"}` → 1 carta normal + 1 entrada con `commander() == true` y `name() == "Atraxa, Grand Unifier"`.
- `parsesObjectWithDeckKeyAndMetadata()`: `{"deck":[{"name":"Sol Ring","quantity":4,"set":"NEO","number":"269"}]}` → `setCode() == "NEO"`, `collectorNumber() == "269"`.
- `honoursIsCommanderFlag()`: `[{"name":"Atraxa, Grand Unifier","isCommander":true}]` → `commander() == true`.
- `defaultsMissingQuantityToOne()`: `[{"name":"Sol Ring"}]` → `quantity() == 1`.
- `ignoresUnknownKeys()`: `[{"name":"Sol Ring","quantity":4,"foil":true,"currency":"USD"}]` → 2 elementos, sin excepción.
- `malformedJson_throws()`, `emptyArray_throws()`, `entryWithoutName_throws()`: las tres con `DeckListParseException`.

`CsvDeckListParserTest`:
- `parsesCommaSeparatedWithHeaderInAnyOrder()`: cabecera `name,quantity,commander` y filas `Sol Ring,4,false` y `"Atraxa, Grand Unifier",1,true` → 2 entradas; la segunda con `commander() == true` porque su columna vale `true`. El nombre va **entrecomillado**: sin comillas serían 4 campos para 3 columnas, y la spec (§6.3) también lo entrecomilla. El orden de las columnas es libre, no tiene por qué ser el del ejemplo de la spec.
- `parsesSetAndNumberColumns()`: cabecera `quantity,name,set,number` con la fila `4,\"Sol Ring\",NEO,269` → `setCode() == \"NEO\"` y `collectorNumber() == \"269\"`.
- `parsesSemicolonSeparated()`: `"1;Sol Ring;NEO"` con cabecera `quantity;name;set`.
- `parsesQuotedNameContainingComma()`: `"1,\"Sword of Fire and Ice\""` → `name() == "Sword of Fire and Ice"` (la coma va dentro de las comillas, no separa campos).
- `parsesEscapedDoubleQuotes()`: `"1,\"Atraxa, \"\"Grand Unifier\"\"\""` → `name() == "Atraxa, \"Grand Unifier\""`.
- `missingHeader_throws()`, `headerWithoutNameColumn_throws()`, `emptyContent_throws()`.

- [ ] **Step 2: Ejecuta y comprueba que falla**

Run: `mvn -o -Dtest=JsonDeckListParserTest,CsvDeckListParserTest test`
Expected: FAIL, `cannot find symbol: class JsonDeckListParser`.

- [ ] **Step 3: Implementa lo mínimo**

`JsonDeckListParser` con `@Component`, `format()` → `JSON`, usando `ObjectMapper` inyectado (`com.fasterxml.jackson.databind.ObjectMapper`). Acepta las tres formas de la spec: array, objeto con `cards`, objeto con `deck`. Claves por carta: `name` (obligatoria), `quantity` (por defecto 1), `set` o `setCode`, `number` o `collectorNumber`, `isCommander`. `commander` del objeto como string se añade como entrada con `commander() == true`. Claves desconocidas se ignoran. Cualquier error de parseo o lista vacía → `DeckListParseException`.

`CsvDeckListParser` con `@Component`, `format()` → `CSV`. Sin dependencias nuevas: separa líneas, detecta el separador contando los que están fuera de comillas (el que más aparezca en la primera línea no vacía), parsea campos con comillas dobles y `""` escapado, y mapea columnas por cabecera en minúsculas (`quantity`, `name`, `set`/`setcode`, `number`/`collectornumber`, `commander`/`iscommander`). `DeckListParseException` si falta la cabecera, falta la columna `name`, no hay filas o no hay ninguna carta.

`DeckListParserRegistry` con `@Component`, guarda `Map<DeckImportFormat, DeckListParser>` construido desde la `List<DeckListParser>` inyectada.

- [ ] **Step 4: Ejecuta y comprueba que pasa**

Run: `mvn -o -Dtest=JsonDeckListParserTest,CsvDeckListParserTest test`
Expected: PASS, 16 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wikicollection/infrastructure/adapter/in/decklist/ \
        src/test/java/com/wikicollection/infrastructure/adapter/in/decklist/
git commit -m "feat(decks): parsers de listas JSON y CSV"
```

---

### Task 3: Búsquedas de Scryfall por nombre con `unique=oracle`

Sin esto el import no puede resolver nada, y sin `unique=oracle` cada línea del mazo parecería ambigua porque Scryfall devuelve las 20 reimpresiones de "Sol Ring".

**Files:**
- Modify: `src/main/java/com/wikicollection/domain/port/out/ExternalMagicCardCatalogClient.java`
- Modify: `src/main/java/com/wikicollection/infrastructure/adapter/out/scryfall/ScryfallClient.java`
- Test: `src/test/java/com/wikicollection/infrastructure/adapter/out/scryfall/ScryfallClientNameSearchTest.java`

**Interfaces:**
- Consumes: `MagicCardSearchResult` (existe), `MagicCardMapper.mapResponse` (existe).
- Produces, dos métodos nuevos en `ExternalMagicCardCatalogClient`:
  ```java
  List<MagicCardSearchResult> searchByNameExact(String name);   // q=name:"X" + unique=oracle
  List<MagicCardSearchResult> searchSuggestions(String name);  // q=X        + unique=oracle, máx. 5
  ```

- [ ] **Step 1: Escribe el test que falla**

`ScryfallClientNameSearchTest`, con el mismo arranque que `ScryfallClientTest` (MockWebServer en `setUp`, `new ScryfallClient(RestClient.builder().baseUrl(server.url("").toString()).build(), server.url("").toString(), 0, 0, new MagicCardMapper())`, `server.shutdown()` en `@AfterEach`):
- `searchByNameExact_sendsNameQueryWithUniqueOracle()`: encola un `ScryfallListResponse` JSON real (reutiliza el shape de `searchFixture()` de `ScryfallClientTest`, no un objeto plano); llama `searchByNameExact("Sol Ring")`; assert `results` con 1 elemento y `RecordedRequest.getPath()` contiene `q=name:%22Sol%20Ring%22` y `unique=oracle`.
- `searchSuggestions_sendsBareQueryWithoutPageSizeAndCapsAtFive()`: encola 8 resultados; assert que la ruta contiene `q=Sol+Ring` (sin comillas) y `unique=oracle`, que **no** contiene `page_size`, `per_page` ni `limit`, y que el método devuelve 5 elementos. Assert también que la ruta **no** contiene `name:%22` (o sea, no es la búsqueda exacta).
- `searchByNameExact_onUpstreamError_returnsEmptyList()`: encola 503 → lista vacía, sin excepción (mismo criterio que `search`, a diferencia de `findPrintings`).

- [ ] **Step 2: Ejecuta y comprueba que falla**

Run: `mvn -o -Dtest=ScryfallClientNameSearchTest test`
Expected: FAIL, `cannot find symbol: method searchByNameExact`.

- [ ] **Step 3: Implementa lo mínimo**

En `ExternalMagicCardCatalogClient`, los dos métodos con el javadoc que explica por qué `unique=oracle` es obligatorio. En `ScryfallClient`, un helper privado que construya la URI:

```java
private String searchUri(String query) {
    return UriComponentsBuilder.fromUriString(baseUrl)
            .path(SEARCH_PATH)
            .queryParam("q", query)
            .queryParam("unique", "oracle")
            .build()
            .toUriString();
}
```

`searchByNameExact` → `searchUri("name:\"" + name + "\"")`; `searchSuggestions` → `searchUri(name)`, y recorta el resultado a los 5 primeros en el cliente con `stream().limit(5).toList()`. **No** mandes `page_size`, `per_page` ni `limit`: AGENTS.md documenta que Scryfall los ignora en silencio, así que no filtran nada y, además, un parámetro mal escrito devuelve 200 con 0 resultados en vez de un error. Ambos con `pace()` y `executeWithRetry(...)`, y con el mismo `catch (RestClientResponseException | ResourceAccessException)` que `search`, devolviendo `List.of()` y haciendo `log.warn`. Reutiliza `mapper.mapResponse(response)`.

- [ ] **Step 4: Ejecuta y comprueba que pasa**

Run: `mvn -o -Dtest=ScryfallClientNameSearchTest test`
Expected: PASS, 3 tests.

- [ ] **Step 5: Ejecuta la regresión de Scryfall**

Run: `mvn -o -Dtest='Scryfall*Test,MagicCardMapper*Test' test`
Expected: PASS. Si `ScryfallClientTest` falla por un record nuevo en `MagicCardMapper`, no es esta tarea: revisa que no hayas tocado el mapper.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wikicollection/domain/port/out/ExternalMagicCardCatalogClient.java \
        src/main/java/com/wikicollection/infrastructure/adapter/out/scryfall/ScryfallClient.java \
        src/test/java/com/wikicollection/infrastructure/adapter/out/scryfall/ScryfallClientNameSearchTest.java
git commit -m "feat(magic): busqueda por nombre con unique=oracle para importar mazos"
```

---

### Task 4: Nombres de la colección del dueño en una consulta

`DeckService.addCard` decide `inCollection` con `MagicCardSearchCriteria(name)`, que compila un regex sobre `name`: `name` no está indexado (`auto-index-creation=false` y `MongoIndexMigration` solo crea `ownerId`), así que 80 de esas búsquedas en un import son 80 escaneos. Aquí sale la proyección de una sola consulta.

**Files:**
- Modify: `src/main/java/com/wikicollection/domain/port/out/MagicCardRepository.java`
- Modify: `src/main/java/com/wikicollection/infrastructure/adapter/out/persistence/MagicCardPersistenceAdapter.java`
- Test: `src/test/java/com/wikicollection/infrastructure/adapter/out/persistence/MagicCardPersistenceAdapterNamesTest.java`

**Interfaces:**
- Consumes: `MagicCardEntity` (`name`), `MongoTemplate`.
- Produces, método nuevo en `MagicCardRepository`:
  ```java
  List<String> findNamesByOwnerId(String ownerId);   // nombres distintos del dueño, sin duplicados
  ```

- [ ] **Step 1: Escribe el test que falla**

`MagicCardPersistenceAdapterNamesTest` con `@ExtendWith(MockitoExtension.class)`, `@Mock MongoTemplate`, `@Mock MagicCardEntityMapper`, `@InjectMocks MagicCardPersistenceAdapter`, siguiendo `MagicCardPersistenceAdapterTest`:
- `findNamesByOwnerId_returnsDistinctNames()`: `when(mongoTemplate.find(any(Query.class), eq(MagicCardEntity.class)))` devuelve `List.of(entity("Sol Ring"), entity("Sol Ring"), entity("Plains"))`; assert que devuelve `List.of("Sol Ring", "Plains")` sin duplicados.
- `findNamesByOwnerId_filtersByOwner()`: captura el `Query` con `ArgumentCaptor` y assert que su `queryObject` contiene `ownerId` = `"user-1"`.
- `findNamesByOwnerId_projectsOnlyTheNameField()`: assert que el `Query` capturado tiene `fields().getIncludedFields()` con la clave `name` y ninguna más.

- [ ] **Step 2: Ejecuta y comprueba que falla**

Run: `mvn -o -Dtest=MagicCardPersistenceAdapterNamesTest test`
Expected: FAIL, `cannot find symbol: method findNamesByOwnerId`.

- [ ] **Step 3: Implementa lo mínimo**

En el puerto, el método con javadoc: una sola consulta con índice sobre `ownerId` en vez de una por nombre.

En el adapter:

```java
@Override
public List<String> findNamesByOwnerId(String ownerId) {
    Query query = Query.query(Criteria.where("ownerId").is(ownerId))
            .fields().include("name")
            .limit(5000);
    return mongoTemplate.find(query, MagicCardEntity.class).stream()
            .map(MagicCardEntity::getName)
            .filter(name -> name != null && !name.isBlank())
            .distinct()
            .toList();
}
```

- [ ] **Step 4: Ejecuta y comprueba que pasa**

Run: `mvn -o -Dtest=MagicCardPersistenceAdapterNamesTest,MagicCardPersistenceAdapterTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wikicollection/domain/port/out/MagicCardRepository.java \
        src/main/java/com/wikicollection/infrastructure/adapter/out/persistence/MagicCardPersistenceAdapter.java \
        src/test/java/com/wikicollection/infrastructure/adapter/out/persistence/MagicCardPersistenceAdapterNamesTest.java
git commit -m "feat(magic): nombres de la coleccion del dueno en una sola consulta"
```

---

### Task 5: `DeckCardFactory` compartido

Hoy `DeckService.addCard` construye el `DeckCard` inline. La importación necesita la misma forma de `DeckCard` pero desde un `MagicCardSearchResult`, así que el mapeo sale a un sitio compartido. Sin cambio de comportamiento.

**Files:**
- Create: `src/main/java/com/wikicollection/application/service/DeckCardFactory.java`
- Modify: `src/main/java/com/wikicollection/application/service/DeckService.java` (método `addCard`)
- Test: `src/test/java/com/wikicollection/application/service/DeckCardFactoryTest.java`

**Interfaces:**
- Consumes: `MagicCard`, `MagicCardSearchResult`, `DeckCard`.
- Produces:
  ```java
  @Component
  public class DeckCardFactory {
      public DeckCard fromMagicCard(MagicCard card, int quantity, boolean inCollection);
      public DeckCard fromSearchResult(MagicCardSearchResult result, int quantity, boolean inCollection);
  }
  ```

- [ ] **Step 1: Escribe el test que falla**

`DeckCardFactoryTest`:
- `fromMagicCard_mapsEveryField()`: con un `MagicCard` construido por `MagicCard.builder()` (name "Sol Ring", manaCost "{1}", type "Artifact", colorIdentity `List.of("G")`, imageUrl "https://img", scryfallId "abc"); con `quantity 4` e `inCollection true` → el `DeckCard` tiene esos mismos valores, `inCollection() == true` e `isProxy() == false`.
- `fromMagicCard_notInCollection_marksProxy()`: `inCollection false` → `isProxy() == true`.
- `fromSearchResult_mapsEveryField()`: con `new MagicCardSearchResult("abc", "Sol Ring", "{1}", "Artifact", "rare", "neo", "Neon Dynasty", "https://img", "1.25", List.of(), List.of("G"), "text")` y `inCollection false` → `cardName() == "Sol Ring"`, `typeLine() == "Artifact"`, `colorIdentity() == List.of("G")`, `scryfallId() == "abc"`, `manaCost() == "{1}"`, `imageUrl() == "https://img"`, `isProxy() == true`.

- [ ] **Step 2: Ejecuta y comprueba que falla**

Run: `mvn -o -Dtest=DeckCardFactoryTest test`
Expected: FAIL, `cannot find symbol: class DeckCardFactory`.

- [ ] **Step 3: Implementa y haz que `DeckService` lo use**

`DeckCardFactory` con `@Component` y los dos métodos delegando en `DeckCard.builder()`. `isProxy` es siempre `!inCollection`.

En `DeckService.addCard`, sustituye el `DeckCard.builder()...build()` inline por `cardFactory.fromMagicCard(fetched, quantity, owned)`, añade `DeckCardFactory` al constructor (como primer parámetro Collaborator nuevo, y actualiza el `@Autowired`/constructor), y **no cambies nada más**: ni la búsqueda de `owned`, ni el `ifPresentOrElse`, ni los `@CacheEvict`.

- [ ] **Step 4: Ejecuta y comprueba que pasa**

Run: `mvn -o -Dtest=DeckCardFactoryTest,DeckServiceTest,DeckServiceOwnerFilterTest,DeckValidationTest test`
Expected: PASS. `DeckServiceTest` verde es la prueba de que el refactor no cambió comportamiento.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wikicollection/application/service/DeckCardFactory.java \
        src/main/java/com/wikicollection/application/service/DeckService.java \
        src/test/java/com/wikicollection/application/service/DeckCardFactoryTest.java
git commit -m "refactor(decks): extrae el mapeo a DeckCard a DeckCardFactory"
```

---

### Task 6: Ejecución asíncrona y registro de jobs

La infraestructura que el worker necesita: `@EnableAsync`, un executor con nombre y el mapa de jobs. Ninguno de los dos es testeable por sí solo, así que se prueba el store y se comprueba el wiring del executor.

**Files:**
- Create: `src/main/java/com/wikicollection/domain/model/DeckImportStatus.java`
- Create: `src/main/java/com/wikicollection/domain/model/DeckImportPhase.java`
- Create: `src/main/java/com/wikicollection/domain/model/UnresolvedReason.java`
- Create: `src/main/java/com/wikicollection/domain/model/UnresolvedCardEntry.java`
- Create: `src/main/java/com/wikicollection/domain/model/DeckImportJob.java`
- Create: `src/main/java/com/wikicollection/domain/model/DeckImportProgress.java`
- Create: `src/main/java/com/wikicollection/application/service/DeckImportJobStore.java`
- Create: `src/main/java/com/wikicollection/infrastructure/config/AsyncConfig.java`
- Test: `src/test/java/com/wikicollection/application/service/DeckImportJobStoreTest.java`
- Test: `src/test/java/com/wikicollection/infrastructure/config/AsyncConfigTest.java`

**Interfaces:**
- Consumes: `DeckImportFormat`, `DeckImportMode`, `DeckStatusReport`, `MagicCardSearchResult`.
- Produces:
  ```java
  public enum DeckImportStatus { PENDING, RUNNING, COMPLETED, FAILED }
  public enum DeckImportPhase { PARSING, RESOLVING, SAVING, DONE }
  public enum UnresolvedReason { NOT_FOUND, AMBIGUOUS, UPSTREAM_ERROR }
  public record UnresolvedCardEntry(int line, String raw, int quantity, String name,
                                    UnresolvedReason reason,
                                    List<MagicCardSearchResult> candidates) {}
  public record DeckImportProgress(int total, int processed, int resolved, int sideboardIgnored) {}

  public record DeckImportJob(String jobId, String deckId, String ownerId,
      DeckImportFormat format, DeckImportMode mode, DeckImportStatus status, DeckImportPhase phase,
      DeckImportProgress progress, String commanderName, List<String> commanderColors,
      List<UnresolvedCardEntry> unresolved, DeckStatusReport validation, String error,
      Instant createdAt, Instant updatedAt, Instant completedAt) {
      public DeckImportJob progress(DeckImportStatus status, DeckImportPhase phase,
                                    DeckImportProgress progress, List<UnresolvedCardEntry> unresolved);
      public DeckImportJob completed(DeckStatusReport validation, String commanderName,
                                     List<String> commanderColors);
      public DeckImportJob failed(String error);
      public static DeckImportJob pending(String jobId, String deckId, String ownerId,
                                          DeckImportFormat format, DeckImportMode mode);
  }

  @Component
  public class DeckImportJobStore {
      public DeckImportJobStore(@Qualifier("deckImportJobs") Cache<String, DeckImportJob> cache);
      public void save(DeckImportJob job);
      public Optional<DeckImportJob> find(String jobId);
      public void evict(String jobId);
  }
  ```

- [ ] **Step 1: Escribe el test que falla**

`DeckImportJobStoreTest`, JUnit puro con una `Cache` de Caffeine real construida en el test:
- `saveThenFind_returnsTheJob()`.
- `find_unknownJob_returnsEmpty()`.
- `savingReplacesThePreviousSnapshot()`: guarda `job.progress(RUNNING, RESOLVING, new DeckImportProgress(3,1,1,0), List.of())` sobre un job `PENDING`; el `find` devuelve la versión con `status() == RUNNING` y conserva `jobId`, `deckId`, `ownerId`, `format`, `mode` y `createdAt` intactos.
- `evict_removesTheJob()`.
- `jobCopiesCarryForwardTheImmutableFields()`: sobre un job completo, `failed("boom")` conserva `jobId`/`deckId`/`ownerId`/`format`/`mode`/`createdAt`, pone `status() == FAILED`, `error() == "boom"` y `completedAt() != null`.

`AsyncConfigTest`, `@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})` con **`@MockitoBean OwnerResolver`** (sin él el contexto no levanta: Mongo no está disponible y `OwnerResolver` lo necesita). Comprueba los beans:
- `executorBeanExistsWithExpectedPool()`: `@Autowired @Qualifier("deckImportExecutor") ThreadPoolTaskExecutor` → `getCorePoolSize() == 2`, `getMaxPoolSize() == 4`.
- `deckImportJobsCacheIsNotSpringCacheManaged()`: `@Autowired @Qualifier("deckImportJobs") Cache<String, DeckImportJob>` no es nulo, y `CacheConfigCacheNamesTest` sigue verde (el registro de jobs **no** va en `CacheConfig.CACHE_NAMES`).

- [ ] **Step 2: Ejecuta y comprueba que falla**

Run: `mvn -o -Dtest=DeckImportJobStoreTest,AsyncConfigTest test`
Expected: FAIL, `cannot find symbol: class DeckImportJobStore`.

- [ ] **Step 3: Implementa lo mínimo**

Los tres enums y los cuatro records con los campos exactos del bloque Interfaces. Los tres métodos de copia de `DeckImportJob` reconstruyen el record pasando a `Instant.now()` en `updatedAt` y en `completedAt` (salvo en `progress`, que solo toca `updatedAt`).

`DeckImportJobStore` con `@Component`; `find` devuelve `Optional.ofNullable(cache.getIfPresent(jobId))`.

`AsyncConfig`:

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean("deckImportExecutor")
    ThreadPoolTaskExecutor deckImportExecutor(
            @Value("${deck.import.executor.core-size:2}") int core,
            @Value("${deck.import.executor.max-size:4}") int max,
            @Value("${deck.import.executor.queue-capacity:20}") int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setThreadNamePrefix("deck-import-");
        executor.initialize();
        return executor;
    }

    @Bean("deckImportJobs")
    Cache<String, DeckImportJob> deckImportJobs(
            @Value("${deck.import.job.ttl:30m}") Duration ttl,
            @Value("${deck.import.job.max-size:200}") long maxSize) {
        return Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maxSize).build();
    }
}
```

El bean va fuera de `CacheConfig` a propósito: `CacheConfig` construye cachés del `CacheManager` de Spring Cache, y aquí lo que se necesita es un `Cache` de Caffeine consultado directamente por el store.

- [ ] **Step 4: Ejecuta y comprueba que pasa**

Run: `mvn -o -Dtest=DeckImportJobStoreTest,AsyncConfigTest,CacheConfigCacheNamesTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wikicollection/domain/model/ \
        src/main/java/com/wikicollection/application/service/DeckImportJobStore.java \
        src/main/java/com/wikicollection/infrastructure/config/AsyncConfig.java \
        src/test/java/com/wikicollection/application/service/DeckImportJobStoreTest.java \
        src/test/java/com/wikicollection/infrastructure/config/AsyncConfigTest.java
git commit -m "feat(decks): registro de jobs en memoria y executor de importacion"
```

---

### Task 7: El worker de importación

El corazón: parsea, resuelve nombre a nombre, y guarda el mazo una sola vez. Toda la lógica de la spec vive aquí, y los tests fijan los cinco Review Focus.

**Files:**
- Create: `src/main/java/com/wikicollection/application/service/DeckNameNormalizer.java`
- Create: `src/main/java/com/wikicollection/application/service/DeckImportWorker.java`
- Create: `src/main/java/com/wikicollection/application/service/DeckCacheInvalidator.java`
- Test: `src/test/java/com/wikicollection/application/service/DeckImportWorkerTest.java`
- Test: `src/test/java/com/wikicollection/application/service/DeckCacheInvalidatorTest.java`

**Interfaces:**
- Consumes: `DeckImportJobStore` y sus records (Task 6), `DeckListParserRegistry`/`DeckListParser`/`ParsedDeckList`/`DeckListEntry` (Tasks 1-2), `ExternalMagicCardCatalogClient.searchByNameExact`/`searchSuggestions` (Task 3), `MagicCardRepository.findNamesByOwnerId` (Task 4), `DeckCardFactory` (Task 5), `DeckRepository`, `MagicCardRepository`, `DeckValidator`, `OwnershipValidator`, `UserOwned`/`Deck`/`DeckCard` del dominio.
- Produces:
  ```java
  @Component
  public class DeckNameNormalizer {
      public String normalize(String raw);   // NFD, quita diacríticos, minúsculas, colapsa espacios, quita puntuación final
  }

  @Component
  public class DeckCacheInvalidator {
      @CacheEvict(cacheNames = {"deckDetail", "deckList"}, allEntries = true)
      public void afterImport();
  }

  @Component
  public class DeckImportWorker {
      public DeckImportWorker(DeckImportJobStore store, DeckListParserRegistry parsers,
          ExternalMagicCardCatalogClient catalog, DeckRepository deckRepository,
          MagicCardRepository magicCardRepository, DeckCardFactory cardFactory,
          DeckValidator validator, OwnershipValidator ownershipValidator,
          DeckNameNormalizer normalizer, DeckCacheInvalidator cacheInvalidator);
      @Async("deckImportExecutor")
      public void run(String jobId, String deckId, String ownerId, String content,
                      DeckImportFormat format, DeckImportMode mode);
  }
  ```

- [ ] **Step 1: Escribe el test que falla**

`DeckImportWorkerTest` con `@ExtendWith(MockitoExtension.class)`, `@Mock` de `DeckImportJobStore`, `DeckListParser`, `ExternalMagicCardCatalogClient`, `DeckRepository`, `MagicCardRepository`, `DeckValidator`, `OwnershipValidator`, y `@Spy`/`new` de `DeckCardFactory` y `DeckNameNormalizer`. Captura el job final con `ArgumentCaptor<DeckImportJob>` sobre `store.save(...)`. Para asercionar sobre el mazo guardado, `ArgumentCaptor<Deck>` sobre `deckRepository.save(...)`.

Fixture: contenido MTGO de 3 cartas (comandante "Atraxa, Grand Unifier", más "Sol Ring" y "Plains"); `when(parserRegistry.forFormat(TXT)).thenReturn(parser)` con `when(parser.parse(anyString()))` devolviendo `new ParsedDeckList(List.of(entry(1,1,"Atraxa, Grand Unifier",true), entry(3,4,"Sol Ring",false), entry(4,20,"Plains",false)), 2)`. `when(deckRepository.findById("deck-1"))` → `Deck.builder().id("deck-1").ownerId("user-1").cards(new ArrayList<>()).build()`.

Tests, con los Review Focus marcados:
- `savesTheDeckOnceWithEveryResolvedCard()` — *RF1*: `verify(deckRepository, times(1)).save(any(Deck.class))`; el mazo guardado tiene 2 `DeckCard` (comandante y Sol Ring) y las 20 quantity de Plains en uno; el job final es `COMPLETED` con `progress().total() == 3`, `processed() == 3`, `resolved() == 3`, `sideboardIgnored() == 2`.
- `aggregatesDuplicateNamesBySummingQuantities()` — *RF1*: dos `DeckListEntry` con nombre `"Sol Ring"` normalizado igual y cantidades 1 y 3 → un solo `DeckCard` con `quantity() == 4`; `total() == 2` (dos entradas, no tres cartas).
- `commanderListedInMainZoneIsNotCountedAsDeckCard()` — *RF2*: una entrada con `commander() == false` y nombre igual al del comandante → **no** aparece en `deck.getCards()`, y `verify(catalog, never()).searchByNameExact("Atraxa, Grand Unifier")` para ese nombre (ya se resolvió como comandante).
- `spanishNameLandsInNotFoundWithSuggestions()` — *RF3*: entrada `"Llanuras"`; `searchByNameExact("Llanuras")` → `List.of()`; `searchSuggestions("Llanuras")` → una carta "Plains" → `unresolved` con `reason() == NOT_FOUND` y un candidato; el job es `COMPLETED` (no `FAILED`) y el mazo se guarda con lo demás.
- `replaceModeOverwritesPreviousCardsOnReimport()` — *RF5*: el mazo existente trae 5 cartas previas → tras `REPLACE`, `deck.getCards()` no contiene ninguna de ellas.
- `mergeModeSumsQuantitiesWithExistingCards()`: con `MERGE` y un mazo que ya tiene `Sol Ring` cantidad 4 → la guardada tiene `Sol Ring` cantidad 8.
- `basicLandsAreResolvedWithoutCallingScryfall()`: `"Plains"` → `verify(catalog, never()).searchByNameExact("Plains")`; el `DeckCard` tiene `typeLine()` con `"Basic Land"` (sin eso el `DeckValidator` lo marca como singleton inválido).
- `resolvedCommanderGetsItsColorIdentity()`: el candidato de "Atraxa, Grand Unifier" tiene `colorIdentity() == List.of("G","W")` → `deck.getCommander() == "Atraxa, Grand Unifier"` y `commanderColors() == List.of("G","W")`; ese `DeckCard` **no** entra en `deck.getCards()`.
- `upstreamErrorFailsTheJobAndLeavesTheDeckUntouched()`: `searchByNameExact("Sol Ring")` lanza `RestClientResponseException` → job `FAILED`, `unresolved` con `UPSTREAM_ERROR`, y **`verify(deckRepository, never()).save(any())`**.
- `ambiguousCandidatesAreReportedNotResolved()`: `searchByNameExact` devuelve 2 cartas con nombres distintos → `AMBIGUOUS` con 2 candidatos, no guardada.
- `singleExactCandidateResolvesAndSuggestionsAreNotCalled()`: 1 candidato con nombre normalizado igual → `verify(catalog, never()).searchSuggestions(anyString())`.
- `fuzzySingleMatchResolvesAccentDifferences()`: exacto → 0 resultados; sugerencias → 1 candidato "Kongou, Keeper of the Deep"; petición `"Kóngou, Keeper of the Deep"` → resuelta.
- `parseErrorFailsTheJob()`: `parser.parse` lanza `DeckListParseException` → `FAILED` con el mensaje y `never()` en `deckRepository.save`.
- `deckNotFoundFailsTheJob()`: `findById` devuelve `Optional.empty()` → `FAILED`, sin `save`.
- `foreignDeckFailsTheJob()`: `ownershipValidator.validateOwner("other", "user-1")` lanza `ForbiddenException` → `FAILED`, sin `save`.
- `validationReportIsStoredInTheJob()`: `validator.validate(any(Deck.class))` → `List.of("El mazo no llega a 99 cartas")`; `validator.evaluate(...)` → `DRAFT`; el job final tiene `validation().reasons()` con ese motivo.
- `progressIsPublishedWhileResolving()`: el test verifica con `InOrder` que `store.save` se llama al menos 3 veces y que una llamada intermedia tiene `phase() == RESOLVING` con `processed() > 0`.
- `inCollectionComesFromASingleQueryForOwner()`: `when(magicCardRepository.findNamesByOwnerId("user-1")).thenReturn(List.of("sol ring"))` → el `DeckCard` de Sol Ring tiene `inCollection() == true` e `isProxy() == false`, y Sol Ring **no** se cuenta en `resolved()`... no: cuenta igual; assert solo los flags. `verify(magicCardRepository, times(1)).findNamesByOwnerId("user-1")`.
- `workerNeverReadsTheSecurityContext()`: sin `SecurityContextHolder` montado, el job se completa igual y con `ownerId` intacto.

- [ ] **Step 2: Ejecuta y comprueba que falla**

Run: `mvn -o -Dtest=DeckImportWorkerTest test`
Expected: FAIL, `cannot find symbol: class DeckImportWorker`.

- [ ] **Step 3: Implementa lo mínimo**

`DeckNameNormalizer`: `@Component`, `normalize` con `java.text.Normalizer.normalize(raw, Form.NFD)`, quita `\\p{M}`, `toLowerCase(Locale.ROOT)`, colapsa `\\s+` a un espacio y quita el punto final.

`DeckImportWorker.run(...)`, en orden:

1. `store.save(job.progress(RUNNING, PARSING, new DeckImportProgress(0,0,0,0), List.of()))`.
2. `Deck deck = deckRepository.findById(deckId).orElseThrow(() -> new DeckNotFoundException(...))` y `ownershipValidator.validateOwner(deck.getOwnerId(), ownerId)` dentro del `try` general; cualquier excepción aquí → `store.save(job.failed(msg))` y `return`.
3. Parsear con `parsers.forFormat(format).parse(content)`.
4. Commander primero: si hay entradas `commander() == true`, resolver su nombre con el mismo algoritmo de dos fases; guardar `commanderName` y `commanderColors` (el `colorIdentity` del candidato). Si ninguna resolvió, nombres sin resolver.
5. Para cada entrada no comandante, **deduplicar por nombre normalizado** en un `LinkedHashMap<String, DeckListEntry>` **sumando cantidades** (RF1). Saltar las entradas cuyo nombre normalizado sea tierra básica de las 19 (*Plains, Island, Swamp, Mountain, Forest, Wasteland, Dryad Arbor, Llanowar Elves, Bayou, Arctic Plains, Scrubland, Tropical Island, Jungle, Badlands, Volcanic Island, Tundra, Underground Sea, Coastal Plains, Ancient Tomb*): resueltas en local con `DeckCard` de `cardFactory.fromSearchResult(new MagicCardSearchResult(null, nombreOriginal, null, "Basic Land", null, null, null, null, null, List.of(), List.of(), null), cantidad, owned)`.
6. Por cada nombre único restante, dos fases: `searchByNameExact(nombreOriginal)`; si hay exactamente un candidato cuyo `normalize(candidate.name()).equals(normalize(nombreOriginal))` → resuelta; si hay varios → `AMBIGUOUS` con todos; si hay cero → `searchSuggestions(nombreOriginal)` y según el resultado lo de arriba (RF3). En cada rama, `store.save(job.progress(RUNNING, RESOLVING, progresoActualizado, unresolved))` para que el poll vea el avance.
7. `catch (RestClientResponseException | ResourceAccessException e)` por carta → `UPSTREAM_ERROR` en `unresolved`, `store.save(job.failed("Scryfall falló: " + e.getMessage()))` y `return`, **sin** tocar el mazo.
8. `store.save(job.progress(RUNNING, SAVING, ...))`.
9. `Set<String> ownedNames = normalizer-normalizados de magicCardRepository.findNamesByOwnerId(ownerId)`.
10. Construir `List<DeckCard>`; con `MERGE`, empezar por las cartas actuales y sumar por `scryfallId` como `DeckService.addCard`.
11. `deck.setCards(lista)`, `deck.setCommander(commanderName)`, `deck.setCommanderColors(commanderColors)`, `deck.setUpdatedAt(LocalDateTime.now())`, `deckRepository.save(deck)`, y **`deckCacheInvalidator.afterImport()`** inmediatamente después (paso 13).
12. `DeckStatusReport report = new DeckStatusReport(validator.evaluate(deck), validator.validate(deck))`; `store.save(job.completed(report, commanderName, commanderColors))`.
13. La evictación de `deckDetail` y `deckList` la hace `deckCacheInvalidator.afterImport()`, **no** un `@CacheEvict` sobre `run`: al estar `run` anotado con `@Async`, el advisor de caché se ejecutaría en el hilo que encola, antes de que el mazo se guarde, y una lectura concurrente volvería a cachear el mazo viejo. `DeckCacheInvalidator` es un bean aparte que el worker llama después del `save`.

- [ ] **Step 4: Ejecuta y comprueba que pasa**

Run: `mvn -o -Dtest=DeckImportWorkerTest test`
Expected: PASS, 20 tests.

- [ ] **Step 5: Testea la evictación de caché por su cuenta**

`DeckCacheInvalidatorTest`, al estilo de `application/service/CacheInvalidationServiceTest`
(que ya verifica las `@CacheEvict` de los otros servicios): un test de Spring que mete
una entrada a mano en `cacheManager.getCache("deckDetail")` y en `deckList`, invoca
`afterImport()` **a través del proxy** (inyectado, no `new`), y assert que ambas cachés
quedan vacías. Sin este test, el orden `@Async`/`@CacheEvict` del punto 13 quedaría
como una suposición.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wikicollection/application/service/DeckNameNormalizer.java \
        src/main/java/com/wikicollection/application/service/DeckImportWorker.java \
        src/main/java/com/wikicollection/application/service/DeckCacheInvalidator.java \
        src/test/java/com/wikicollection/application/service/DeckImportWorkerTest.java \
        src/test/java/com/wikicollection/application/service/DeckCacheInvalidatorTest.java
git commit -m "feat(decks): worker de importacion de mazos en segundo plano"
```

---

### Task 8: `DeckImportService`, validaciones síncronas y disparo del worker

El borde que decide qué entra al job y qué se rechaza con 400 antes de crearlo.

**Files:**
- Create: `src/main/java/com/wikicollection/domain/port/in/DeckImportUseCase.java`
- Create: `src/main/java/com/wikicollection/application/exception/DeckImportNotFoundException.java`
- Create: `src/main/java/com/wikicollection/infrastructure/adapter/in/decklist/DeckImportFormatDetector.java`
- Create: `src/main/java/com/wikicollection/application/service/DeckImportService.java`
- Test: `src/test/java/com/wikicollection/application/service/DeckImportServiceTest.java`

**Interfaces:**
- Consumes: `DeckImportWorker.run`, `DeckImportJobStore`, `DeckListParserRegistry`, `DeckRepository`, `OwnershipValidator`, `DeckImportFormatDetector`.
- Produces:
  ```java
  public interface DeckImportUseCase {
      DeckImportJob startImport(String deckId, String content, DeckImportFormat format,
                                DeckImportMode mode, String userId);
      DeckImportJob findJob(String deckId, String jobId, String userId);
  }

  @Component
  public class DeckImportFormatDetector {
      public DeckImportFormat detect(String content, String filename);  // null si no puede deducir
      public void requireConsistent(DeckImportFormat declared, String content, String filename);
  }

  @Service
  public class DeckImportService implements DeckImportUseCase { /* constructores explícitos */ }
  ```

- [ ] **Step 1: Escribe el test que falla**

`DeckImportServiceTest` con `@ExtendWith(MockitoExtension.class)`, `@Mock` de `DeckImportWorker`, `DeckImportJobStore`, `DeckListParserRegistry`, `DeckRepository`, `OwnershipValidator`, y `@Spy` de `DeckImportFormatDetector`:
- `startImport_createsPendingJobAndDelegatesToWorker()`: assert `store.save` guarda un job `PENDING` con el `deckId`, `ownerId`, `format`, `mode` y `progress().total() == 0`; `verify(worker).run(anyString(), eq("deck-1"), eq("user-1"), anyString(), eq(TXT), eq(REPLACE))`.
- `startImport_validatesOwnershipBeforeCreatingAnything()`: `ownershipValidator.validateOwner("other", "user-1")` lanza `ForbiddenException` → `verify(store, never()).save(any())` y `verify(worker, never()).run(any(), any(), any(), any(), any(), any())`.
- `startImport_unknownDeck_throws()`: `findById` → `Optional.empty()` → `DeckNotFoundException`, sin job creado.
- `startImport_blankContent_throws()`: `"   "` → `IllegalArgumentException`.
- `startImport_tooManyEntries_throws()`: 501 líneas `1 Sol Ring` → `IllegalArgumentException` con el mensaje que mencione el límite.
- `startImport_oversizedContent_throws()`: contenido de 5 MB + 1 con `deck.import.max-file-size=5242880` → `IllegalArgumentException`.
- `startImport_declaredFormatInconsistentWithContent_throws()`: `declared = JSON` con contenido `1 Sol Ring (NEO)` → `IllegalArgumentException`.
- `startImport_detectsFormatFromContentWhenNotDeclared()`: contenido JSON válido con `filename == null` → el job sale con `format() == JSON`.
- `startImport_detectsFormatFromFilename()`: `"deck.csv"` con contenido CSV → `format() == CSV`.
- `findJob_returnsTheJobForTheOwner()`.
- `findJob_unknownJob_throws()` → `DeckImportNotFoundException`.
- `findJob_jobOfAnotherDeck_throws()` — *RF4*: el store devuelve un job con `deckId "deck-2"` y se pide `"deck-1"` → `DeckImportNotFoundException` (no `ForbiddenException`).
- `findJob_deckOwnedByAnotherUser_throws()` → `ForbiddenException`.

- [ ] **Step 2: Ejecuta y comprueba que falla**

Run: `mvn -o -Dtest=DeckImportServiceTest test`
Expected: FAIL, `cannot find symbol: class DeckImportService`.

- [ ] **Step 3: Implementa lo mínimo**

`DeckImportNotFoundException extends RuntimeException` en `application/exception/`.

`DeckImportFormatDetector` con `@Component`: si hay `filename` con extensión `.txt`/`.json`/`.csv`, esa gana; si no, deduce del contenido: empieza por `{` o `[` tras quitar blancos → `JSON`; la primera línea no vacía contiene `,` o `;` y tiene cabecera con `name` → `CSV`; si no → `TXT`. `requireConsistent` compara lo declarado con lo deducido y lanza `IllegalArgumentException` si difieren.

`DeckImportService` con `@Service`, implementando `DeckImportUseCase`, con los checks en este orden: `deckRepository.findById` → `ownershipValidator.validateOwner` → contenido en blanco → número de líneas no vacías mayor que `deck.import.max-entries` (500) → tamaño mayor que `deck.import.max-file-size` (5 MB) → `requireConsistent`. Luego `UUID.randomUUID().toString()`, `store.save(DeckImportJob.pending(jobId, deckId, userId, format, mode))` (usa la fabrica de la Task 6, no el constructor de 15 componentes) y `worker.run(jobId, deckId, userId, content, format, mode)`.

`findJob(deckId, jobId, userId)`: `store.find(jobId)` vacío o con `deckId` distinta → `DeckImportNotFoundException` (RF4); `deckRepository.findById(deckId)` y `ownershipValidator.validateOwner` → `ForbiddenException`; si el job existe, devuélvelo.

- [ ] **Step 4: Ejecuta y comprueba que pasa**

Run: `mvn -o -Dtest=DeckImportServiceTest test`
Expected: PASS, 13 tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/wikicollection/domain/port/in/DeckImportUseCase.java \
        src/main/java/com/wikicollection/application/exception/DeckImportNotFoundException.java \
        src/main/java/com/wikicollection/infrastructure/adapter/in/decklist/DeckImportFormatDetector.java \
        src/main/java/com/wikicollection/application/service/DeckImportService.java \
        src/test/java/com/wikicollection/application/service/DeckImportServiceTest.java
git commit -m "feat(decks): servicio de importacion con validaciones sincronicas"
```

---

### Task 9: Los tres endpoints y sus DTOs

La superficie HTTP. Es la tarea donde se ve si el resto encaja de verdad.

**Files:**
- Create: `src/main/java/com/wikicollection/infrastructure/adapter/in/web/dto/DeckImportAcceptedResponse.java`
- Create: `src/main/java/com/wikicollection/infrastructure/adapter/in/web/dto/DeckImportJobResponse.java`
- Create: `src/main/java/com/wikicollection/infrastructure/adapter/in/web/dto/DeckImportProgressResponse.java`
- Create: `src/main/java/com/wikicollection/infrastructure/adapter/in/web/dto/DeckImportUnresolvedResponse.java`
- Create: `src/main/java/com/wikicollection/infrastructure/adapter/in/web/dto/DeckImportCandidateResponse.java`
- Create: `src/main/java/com/wikicollection/infrastructure/adapter/in/web/dto/DeckImportValidationResponse.java`
- Create: `src/main/java/com/wikicollection/infrastructure/adapter/in/web/dto/DeckImportDtoMapper.java`
- Modify: `src/main/java/com/wikicollection/infrastructure/adapter/in/web/DeckController.java`
- Modify: `src/main/java/com/wikicollection/infrastructure/adapter/in/web/GlobalExceptionHandler.java`
- Test: `src/test/java/com/wikicollection/infrastructure/adapter/in/web/DeckImportControllerTest.java`

**Interfaces:**
- Consumes: `DeckImportUseCase`, `DeckUseCase` (para el `deck` de la respuesta), `DeckDtoMapper`, `DeckImportNotFoundException`, `DeckListParseException`.
- No se reutiliza el `DeckStatusResponse` existente: es `(status, message)` y su `message`
  es `null` salvo en `INVALID`, mientras que el import necesita la **lista** de motivos
  para que el frontend los pinte uno a uno (spec §3.3). De ahí
  `DeckImportValidationResponse(DeckStatus status, List<String> reasons)`.
- Produces, en `DeckController`:
  ```java
  @PostMapping(value = "/{id}/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<DeckImportAcceptedResponse> importDeck(
          @PathVariable String id,
          @RequestParam("file") MultipartFile file,
          @RequestParam(required = false) String format,
          @RequestParam(defaultValue = "replace") String mode,
          @CurrentUser String currentUserId,
          UriComponentsBuilder ucb);

  @PostMapping(value = "/{id}/imports/text", consumes = MediaType.TEXT_PLAIN_VALUE)
  ResponseEntity<DeckImportAcceptedResponse> importDeckText(
          @PathVariable String id, @RequestBody String content,
          @RequestParam(required = false) String format,
          @RequestParam(defaultValue = "replace") String mode,
          @CurrentUser String currentUserId, UriComponentsBuilder ucb);

  @GetMapping("/{id}/imports/{jobId}")
  DeckImportJobResponse getImport(@PathVariable String id, @PathVariable String jobId,
                                  @CurrentUser String currentUserId);
  ```

- [ ] **Step 1: Escribe el test que falla**

`DeckImportControllerTest` como `DeckControllerTest`: `@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})`, `@AutoConfigureMockMvc`, `@MockitoBean` de `OwnerResolver`, `DeckRepository`, `MagicCardRepository`, `ScryfallClient`, **`DeckImportUseCase`**, `@Autowired MockMvc`, `@Autowired CacheManager` y `CacheTestSupport.clearAll` en `@BeforeEach`.

Tests:
- `importDeck_withFile_returns202WithLocation()`: `mockMvc.perform(multipart("/api/v1/decks/deck-1/imports").file(new MockMultipartFile("file", "deck.txt", "text/plain", bytes)).with(user("user-1")))` → `status().isAccepted()`, `header().string("Location", "/api/v1/decks/deck-1/imports/job-1")`, `jsonPath("$.jobId").value("job-1")`, `jsonPath("$.status").value("PENDING")`, `jsonPath("$.statusUrl").value("/api/v1/decks/deck-1/imports/job-1")`.
- `importDeckText_withPlainText_returns202()`: `post("/api/v1/decks/deck-1/imports/text").contentType(TEXT_PLAIN).content("1 Sol Ring")` → `202`.
- `importDeck_emptyFile_returns400()` → `400` con `$.status` 400.
- `importDeck_unknownDeck_returns404()`: `startImport` lanza `DeckNotFoundException` → `404`.
- `importDeck_foreignDeck_returns403()`: `ForbiddenException` → `403`.
- `importDeck_declaredFormatMismatch_returns400()`: `IllegalArgumentException` → `400`.
- `importDeck_withoutAuthentication_returns401()`: sin `with(user(...))` → `401`.
- `getImport_completed_returnsDeckAndReport()`: el use case devuelve un job `COMPLETED` con 2 entradas resueltas, 1 `NOT_FOUND` con 1 candidato y un `DeckStatusReport(DRAFT, List.of("El mazo no llega a 99 cartas"))`; `deckUseCase.findById("deck-1")` devuelve un `Deck` → `200` con `$.status` = `COMPLETED`, `$.phase` = `DONE`, `$.deck.name`, `$.validation.status` = `DRAFT`, `$.validation.reasons[0]`, `$.unresolved[0].reason` = `NOT_FOUND`, `$.unresolved[0].candidates[0].name`, `$.progress.total` = 3.
- `getImport_running_returnsProgressWithoutDeck()`: job `RUNNING` en fase `RESOLVING` → `200`, `$.phase` = `RESOLVING`, `$.progress.processed` = 12, y `$.deck` no existe (`jsonPath("$.deck").doesNotExist()`).
- `getImport_failed_returnsError()`: job `FAILED` con `error "Scryfall falló: 503"` → `$.error`.
- `getImport_unknownJob_returns404()` → `404`.
- `getImport_jobOfAnotherDeck_returns404()` — *RF4*: `404`, no `403`.
- `getImport_deckOwnedByAnotherUser_returns403()` → `403`.
- `getImport_withoutAuthentication_returns401()`.

- [ ] **Step 2: Ejecuta y comprueba que falla**

Run: `mvn -o -Dtest=DeckImportControllerTest test`
Expected: FAIL, `404` en los tres endpoints nuevos.

- [ ] **Step 3: Implementa lo mínimo**

Los siete DTOs con records cuyos campos son los del JSON de la spec (§3.3), más
`DeckImportValidationResponse(status, reasons)`. `DeckImportDtoMapper` con `@Component` y:
- `DeckImportAcceptedResponse toAccepted(DeckImportJob job, String statusUrl)`
- `DeckImportJobResponse toJobResponse(DeckImportJob job, Deck deck)` — devuelve `deck` a `null` si el job no ha terminado; los enums como `name()`; `Instant` en ISO-8601; `commander` a `null` si `commanderName() == null`.

En `DeckController`: inyecta `DeckImportUseCase` en el constructor (el que ya recibe `DeckUseCase` y `DeckDtoMapper`), añade los tres métodos, convierte `String format`/`String mode` a enums con un parseo que lance `IllegalArgumentException` en valor desconocido (el `GlobalExceptionHandler` ya la traduce a 400), y construye el `Location` con `UriComponentsBuilder` como hace `create`.

En `GlobalExceptionHandler`, añade los handlers de `DeckListParseException` → `400`, `DeckImportNotFoundException` → `404` y `org.springframework.core.task.TaskRejectedException` → `429` (executor saturado; sin él, un pico de importaciones sube un `500` al cliente).

- [ ] **Step 4: Ejecuta y comprueba que pasa**

Run: `mvn -o -Dtest=DeckImportControllerTest,DeckControllerTest,GlobalExceptionHandlerTest,SecurityMatrixTest test`
Expected: PASS. `SecurityMatrixTest` y `DeckControllerTest` verdes son la prueba de que no se rompió nada.

Y después, porque los beans nuevos (`DeckImportService`, el worker, los tres parsers) se
instancian en el contexto de **todos** los `@SpringBootTest` del repo, no solo en los de mazos:

Run: `mvn -o -Dtest='*ControllerTest,ResponseVisibilityTest' test`
Expected: PASS. Si alguno falla al cargar el contexto, el problema es un bean nuevo que falta
por mockear en ese test, no este código.

- [ ] **Step 5: Añade la anotación OpenAPI** en los tres métodos: `@Operation` + `@ApiResponses` con 202/400/404/403 en los POST y 200/404/403 en el GET, en el estilo del resto del controller. Sin esto el contrato nuevo no aparece en `/v3/api-docs`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/wikicollection/infrastructure/adapter/in/web/ \
        src/test/java/com/wikicollection/infrastructure/adapter/in/web/DeckImportControllerTest.java
git commit -m "feat(decks): endpoints 202+poll para importar mazos (#353)"
```

---

### Task 10: Propiedades, documentación y puerta de cobertura

**Files:**
- Modify: `src/main/resources/application.properties`
- Modify: `AGENTS.md`
- Test: ninguno nuevo; aquí se ejecuta la puerta.

- [ ] **Step 1: Añade las propiedades**

Bloque nuevo al final de `application.properties`:

```properties
# Importacion de mazos (#353)
deck.import.executor.core-size=2
deck.import.executor.max-size=4
deck.import.executor.queue-capacity=20
deck.import.job.ttl=30m
deck.import.job.max-size=200
deck.import.max-entries=500
deck.import.max-file-size=5242880
```

Los tres últimos coinciden con los valores por defecto que ya usan el código y con
`spring.servlet.multipart.max-file-size=5MB`, para que subir un `.txt` de 6 MB dé el
mismo error por las dos vías.

- [ ] **Step 2: Ejecuta la puerta completa**

Run: `mvn -o verify`
Expected: **BUILD SUCCESS** y la regla JaCoCo satisfied (`LINE` ≥ 0.80). Si falla por cobertura, mira `target/site/jacoco/index.html`: losasternos recién añadidos no usados son lo más probable.

- [ ] **Step 3: Documenta en `AGENTS.md`**

Sección nueva "Importación de mazos (issue #353)" con lo que un próximo tiene que saber sin abrir los ficheros:

- El import es **asíncrono**: `POST` devuelve `202` + `jobId` y el cliente hace poll de `GET /api/v1/decks/{id}/imports/{jobId}`.
- El estado de los jobs vive **en memoria** (Caffeine, TTL 30 min): un reinicio los borra y el `GET` responde `404`. Reenviar el archivo es seguro con `mode=REPLACE`.
- **Nunca llames a `DeckImportWorker.run()` desde dentro del mismo bean**: `@Async` se ignora en silencio por auto-invocación.
- El worker **no lee el `SecurityContext`**: el `userId` viaja como parámetro.
- El mazo se guarda **una sola vez**, al final. Si el job falla, el mazo queda intacto.
- Un `UPSTREAM_ERROR` (Scryfall 5xx/timeout tras los 4 intentos) tumba el job y **no** se confunde con una carta que no existe. Es la misma lección que las impressions de #451, al revés.
- Las tierras básicas se resuelven **en local** con `typeLine` "Basic Land": sin eso el `DeckValidator` las marca como singleton inválido.
- `unique=oracle` es obligatorio en las búsquedas por nombre: sin él Scryfall devuelve todas las reimpresiones y cada línea del mazo parecería ambigua.
- `DeckControllerTest` limpia cachés con `CacheTestSupport.clearAll` porque los mazos están cacheados.
- El registro de jobs **no** va en `CacheConfig.CACHE_NAMES`: es un `Cache` de Caffeine consultado directamente por `DeckImportJobStore`.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/application.properties AGENTS.md
git commit -m "docs(decks): propiedades y AGENTS.md de la importacion de mazos"
```

- [ ] **Step 5: Abre el PR**

```bash
git push -u origin feat/353-deck-import
gh pr create --title "feat(decks): importación asíncrona de mazos Commander (#353)" --body "Closes #353"
```

El cuerpo debe mencionar además: la spec y el plan (`docs/superpowers/...`), que el estado de los jobs es en memoria y se pierde al reiniciar, el cambio de semántica de `inCollection` en el import, y que un `UPSTREAM_ERROR` tumba el job sin tocar el mazo.

## Fuera de alcance

Los cuatro puntos 4-6 del Global Constraints y el "Fuera de alcance" de la spec: import por URL externa, SSE, jobs persistidos en Mongo, cancelar jobs, reimportar progreso incremental en el mazo, y el sideboard (se ignora y se cuenta en `sideboardIgnored`). La UI es la issue #462 del frontend.