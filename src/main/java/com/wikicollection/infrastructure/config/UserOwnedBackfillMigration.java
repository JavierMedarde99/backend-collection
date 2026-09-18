package com.wikicollection.infrastructure.config;

import java.util.List;
import java.util.Map;

import com.wikicollection.application.service.OwnerResolver;
import com.wikicollection.domain.model.UserOwned;

import lombok.extern.slf4j.Slf4j;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Rellena userOwned en documentos guardados antes del snapshot (solo donde falta).
 * Idempotente: nunca toca documentos que ya tienen snapshot.
 */
@Component
@Slf4j
public class UserOwnedBackfillMigration implements ApplicationRunner {

    private static final List<String> COLLECTIONS =
            List.of("books", "games", "board_games", "magic_cards", "decks", "movie_shows");

    private final MongoTemplate mongoTemplate;
    private final OwnerResolver ownerResolver;
    private final boolean enabled;

    public UserOwnedBackfillMigration(MongoTemplate mongoTemplate,
                                      OwnerResolver ownerResolver,
                                      @Value("${app.migration.enabled:true}") boolean enabled) {
        this.mongoTemplate = mongoTemplate;
        this.ownerResolver = ownerResolver;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        try {
            for (String collection : COLLECTIONS) {
                backfill(collection);
            }
        } catch (RuntimeException e) {
            log.warn("Backfill de userOwned omitido (Mongo no disponible): {}", e.getMessage());
        }
    }

    private void backfill(String collection) {
        Query missing = new Query(new Criteria().orOperator(
                Criteria.where("userOwned").exists(false),
                Criteria.where("userOwned.username").exists(false)));
        missing.fields().include("_id").include("ownerId");
        for (Document doc : mongoTemplate.find(missing, Document.class, collection)) {
            String ownerId = doc.getString("ownerId");
            if (ownerId == null) {
                continue;
            }
            UserOwned owned = ownerResolver.resolveOwner(ownerId);
            Update update = new Update().set("userOwned", Map.of(
                    "ownerId", owned.getOwnerId(),
                    "ownerName", owned.getOwnerName(),
                    "username", owned.getUsername()));
            mongoTemplate.updateFirst(
                    new Query(Criteria.where("_id").is(doc.get("_id"))), update, collection);
        }
    }
}
