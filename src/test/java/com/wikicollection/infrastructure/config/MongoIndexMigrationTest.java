package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import org.mockito.ArgumentCaptor;

class MongoIndexMigrationTest {

    /**
     * Colecciones cuyo servicio valida la unicidad de {@code externalId} y lanza 409 en
     * duplicado: son las que necesitan el índice único parcial como respaldo (#478).
     */
    private static final List<String> EXTERNAL_ID_COLLECTIONS =
            List.of("books", "movie_shows", "amiibos", "funkos");

    private MongoTemplate template(IndexOperations ops) {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        when(mongoTemplate.indexOps(any(String.class))).thenReturn(ops);
        return mongoTemplate;
    }

    private MongoTemplate templatePerCollection(Map<String, IndexOperations> opsByCollection) {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        // Las colecciones no relevantes para el test reciben un mock descartable: sin él,
        // indexOps devolvería null y la NPE abortaría la migración entera por el catch.
        IndexOperations fallback = mock(IndexOperations.class);
        when(mongoTemplate.indexOps(any(String.class)))
                .thenAnswer(invocation -> opsByCollection.getOrDefault(
                        invocation.getArgument(0, String.class), fallback));
        return mongoTemplate;
    }

    @Test
    void run_createsUniqueIndexesOnUsers() {
        IndexOperations ops = mock(IndexOperations.class);
        ArgumentCaptor<Index> captor = ArgumentCaptor.forClass(Index.class);

        new MongoIndexMigration(template(ops), true).run(null);

        verify(ops, atLeastOnce()).ensureIndex(captor.capture());
        assertThat(captor.getAllValues())
                .filteredOn(index -> Boolean.TRUE.equals(index.getIndexOptions().get("unique"))
                        && !index.getIndexKeys().containsKey("externalId"))
                .as("username, email e userId deben ser únicos (los de externalId se comprueban aparte)")
                .hasSize(3);
    }

    @Test
    void run_createsOwnerIndexOnEachCollection() {
        IndexOperations ops = mock(IndexOperations.class);

        new MongoIndexMigration(template(ops), true).run(null);

        verify(ops, atLeastOnce()).ensureIndex(any(Index.class));
    }

    @Test
    void run_createsParentIndexOnBoardGames() {
        IndexOperations otherOps = mock(IndexOperations.class);
        IndexOperations boardGamesOps = mock(IndexOperations.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        when(mongoTemplate.indexOps(any(String.class))).thenReturn(otherOps);
        when(mongoTemplate.indexOps("board_games")).thenReturn(boardGamesOps);
        ArgumentCaptor<Index> captor = ArgumentCaptor.forClass(Index.class);

        new MongoIndexMigration(mongoTemplate, true).run(null);

        verify(boardGamesOps, atLeastOnce()).ensureIndex(captor.capture());
        assertThat(captor.getAllValues())
                .filteredOn(index -> index.getIndexKeys().containsKey("parentId"))
                .singleElement()
                .satisfies(index -> {
                    assertThat(index.getIndexKeys().keySet()).containsExactly("ownerId", "parentId");
                    assertThat(index.getIndexKeys().get("ownerId")).isEqualTo(1);
                    assertThat(index.getIndexKeys().get("parentId")).isEqualTo(1);
                });
    }

    @Test
    void run_createsParentIndexOnGames() {
        IndexOperations otherOps = mock(IndexOperations.class);
        IndexOperations gamesOps = mock(IndexOperations.class);
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        when(mongoTemplate.indexOps(any(String.class))).thenReturn(otherOps);
        when(mongoTemplate.indexOps("games")).thenReturn(gamesOps);
        ArgumentCaptor<Index> captor = ArgumentCaptor.forClass(Index.class);

        new MongoIndexMigration(mongoTemplate, true).run(null);

        verify(gamesOps, atLeastOnce()).ensureIndex(captor.capture());
        assertThat(captor.getAllValues())
                .filteredOn(index -> index.getIndexKeys().containsKey("parentId"))
                .singleElement()
                .satisfies(index -> {
                    assertThat(index.getIndexKeys().keySet()).containsExactly("ownerId", "parentId");
                    assertThat(index.getIndexKeys().get("ownerId")).isEqualTo(1);
                    assertThat(index.getIndexKeys().get("parentId")).isEqualTo(1);
                });
    }

    @Test
    void run_doesNothing_whenDisabled() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);

        new MongoIndexMigration(mongoTemplate, false).run(null);

        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void indexOptions_uniqueFlag() {
        Index unique = new Index().on("username", org.springframework.data.domain.Sort.Direction.ASC).unique();
        Document options = unique.getIndexOptions();
        assertThat(options.get("unique")).isEqualTo(true);
    }

    @Test
    void run_createsPartialUniqueExternalIdIndex_onCollectionsThatEnforceUniqueness() {
        Map<String, IndexOperations> opsByCollection = new HashMap<>();
        for (String collection : EXTERNAL_ID_COLLECTIONS) {
            opsByCollection.put(collection, mock(IndexOperations.class));
        }

        new MongoIndexMigration(templatePerCollection(opsByCollection), true).run(null);

        for (String collection : EXTERNAL_ID_COLLECTIONS) {
            Index externalIdIndex = uniqueExternalIdIndex(opsByCollection.get(collection), collection);

            assertThat(externalIdIndex.getIndexOptions().get("unique"))
                    .as("%s: el índice de externalId debe ser único", collection)
                    .isEqualTo(true);
            assertThat(partialFilter(externalIdIndex, collection))
                    .as("%s: el índice debe ser parcial para que los documentos sin externalId "
                            + "no colisionen entre sí en el valor null", collection)
                    .isEqualTo(new Document("externalId", new Document("$type", "string").append("$gt", "")));
        }
    }

    @Test
    void run_skipsExternalIdIndex_onCollectionsWhoseServiceDoesNotEnforceUniqueness() {
        Map<String, IndexOperations> opsByCollection = new HashMap<>();
        opsByCollection.put("games", mock(IndexOperations.class));
        opsByCollection.put("board_games", mock(IndexOperations.class));

        new MongoIndexMigration(templatePerCollection(opsByCollection), true).run(null);

        for (Map.Entry<String, IndexOperations> entry : opsByCollection.entrySet()) {
            ArgumentCaptor<Index> captor = ArgumentCaptor.forClass(Index.class);
            verify(entry.getValue(), atLeastOnce()).ensureIndex(captor.capture());

            assertThat(captor.getAllValues())
                    .as("%s no valida unicidad de externalId: no debe tener índice único sobre ese campo",
                            entry.getKey())
                    .noneMatch(index -> index.getIndexOptions().containsKey("partialFilterExpression"));
        }
    }

    private static Index uniqueExternalIdIndex(IndexOperations ops, String collection) {
        ArgumentCaptor<Index> captor = ArgumentCaptor.forClass(Index.class);
        verify(ops, atLeastOnce()).ensureIndex(captor.capture());

        return captor.getAllValues().stream()
                .filter(index -> index.getIndexOptions().containsKey("partialFilterExpression"))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        collection + ": no se creó ningún índice parcial (falta el de externalId)"));
    }

    private static Document partialFilter(Index index, String collection) {
        Object filter = index.getIndexOptions().get("partialFilterExpression");
        assertThat(filter)
                .as("%s: partialFilterExpression debe ser un Document", collection)
                .isInstanceOf(Document.class);
        return (Document) filter;
    }
}
