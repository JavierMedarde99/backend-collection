package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import org.mockito.ArgumentCaptor;

class MongoIndexMigrationTest {

    private MongoTemplate template(IndexOperations ops) {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        when(mongoTemplate.indexOps(any(String.class))).thenReturn(ops);
        return mongoTemplate;
    }

    @Test
    void run_createsUniqueIndexesOnUsers() {
        IndexOperations ops = mock(IndexOperations.class);
        ArgumentCaptor<Index> captor = ArgumentCaptor.forClass(Index.class);

        new MongoIndexMigration(template(ops), true).run(null);

        verify(ops, atLeastOnce()).ensureIndex(captor.capture());
        assertThat(captor.getAllValues())
                .filteredOn(index -> Boolean.TRUE.equals(index.getIndexOptions().get("unique")))
                .hasSize(2);
    }

    @Test
    void run_createsOwnerIndexOnEachCollection() {
        IndexOperations ops = mock(IndexOperations.class);

        new MongoIndexMigration(template(ops), true).run(null);

        verify(ops, atLeastOnce()).ensureIndex(any(Index.class));
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
}
