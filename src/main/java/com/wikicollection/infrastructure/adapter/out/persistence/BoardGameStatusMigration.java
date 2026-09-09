package com.wikicollection.infrastructure.adapter.out.persistence;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.boardgame-status-migration.enabled", havingValue = "true", matchIfMissing = true)
public class BoardGameStatusMigration implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    public BoardGameStatusMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        mongoTemplate.updateMulti(
                new Query(Criteria.where("status").in("PREVIOUSLY_OWNED", "FOR_TRADE")),
                new Update().set("status", "WISHLIST"),
                BoardGameEntity.class);
    }
}