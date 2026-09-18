package com.wikicollection.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.application.service.OwnerResolver;
import com.wikicollection.domain.model.UserOwned;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ExtendWith(MockitoExtension.class)
class UserOwnedBackfillMigrationTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private OwnerResolver ownerResolver;

    private UserOwnedBackfillMigration migration(boolean enabled) {
        return new UserOwnedBackfillMigration(mongoTemplate, ownerResolver, enabled);
    }

    @Test
    void run_backfillsMissingSnapshots() {
        Document doc = new Document("_id", new ObjectId()).append("ownerId", "u1");
        when(mongoTemplate.find(any(Query.class), eq(Document.class), any(String.class)))
                .thenReturn(List.of(doc));
        when(ownerResolver.resolveOwner("u1"))
                .thenReturn(UserOwned.builder().ownerId("u1").ownerName("Javi").build());

        migration(true).run(null);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq("books"));
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq("games"));
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq("movie_shows"));
    }

    @Test
    void run_skipsDocumentsWithoutOwner() {
        Document doc = new Document("_id", new ObjectId());
        when(mongoTemplate.find(any(Query.class), eq(Document.class), any(String.class)))
                .thenReturn(List.of(doc));

        migration(true).run(null);

        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), any(String.class));
    }

    @Test
    void run_doesNothing_whenDisabled() {
        migration(false).run(null);

        verify(mongoTemplate, never()).find(any(Query.class), eq(Document.class), any(String.class));
    }
}
