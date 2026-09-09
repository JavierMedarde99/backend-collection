package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ExtendWith(MockitoExtension.class)
class BoardGameStatusMigrationTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Test
    void migraLosEstadosLegacyAWishlist() {
        BoardGameStatusMigration migration = new BoardGameStatusMigration(mongoTemplate);

        migration.run();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateMulti(queryCaptor.capture(), updateCaptor.capture(), org.mockito.ArgumentMatchers.eq(BoardGameEntity.class));

        Document query = queryCaptor.getValue().getQueryObject();
        assertThat(query).isEqualTo(new Document("status", new Document("$in", java.util.List.of("PREVIOUSLY_OWNED", "FOR_TRADE"))));

        Document update = updateCaptor.getValue().getUpdateObject();
        assertThat(update).isEqualTo(new Document("$set", new Document("status", "WISHLIST")));
    }
}