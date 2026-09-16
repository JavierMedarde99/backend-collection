package com.wikicollection.infrastructure.config;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Crea los índices de auth/ownership en Mongo de forma explícita, sin depender de
 * {@code spring.data.mongodb.auto-index-creation} (desactivado por defecto).
 */
@Component
@Slf4j
public class MongoIndexMigration implements ApplicationRunner {

    private static final List<String> OWNER_COLLECTIONS =
            List.of("books", "games", "board_games", "magic_cards", "decks", "movie_shows");

    private final MongoTemplate mongoTemplate;
    private final boolean enabled;

    public MongoIndexMigration(MongoTemplate mongoTemplate,
                               @Value("${app.migration.enabled:true}") boolean enabled) {
        this.mongoTemplate = mongoTemplate;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        try {
            mongoTemplate.indexOps("users")
                    .ensureIndex(new Index().on("username", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps("users")
                    .ensureIndex(new Index().on("email", Sort.Direction.ASC).unique());
            mongoTemplate.indexOps("user_preferences")
                    .ensureIndex(new Index().on("userId", Sort.Direction.ASC).unique());
            Index ownerIndex = new Index().on("ownerId", Sort.Direction.ASC);
            for (String collection : OWNER_COLLECTIONS) {
                mongoTemplate.indexOps(collection).ensureIndex(ownerIndex);
            }
        } catch (RuntimeException e) {
            log.warn("Creación de índices omitida (Mongo no disponible): {}", e.getMessage());
        }
    }
}
