package com.wikicollection.infrastructure.config;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.stereotype.Component;

/**
 * Crea los índices de auth/ownership en Mongo de forma explícita, sin depender de
 * {@code spring.data.mongodb.auto-index-creation} (desactivado por defecto).
 */
@Component
@Slf4j
public class MongoIndexMigration implements ApplicationRunner {

    private static final List<String> OWNER_COLLECTIONS =
            List.of("books", "games", "board_games", "magic_cards", "decks", "movie_shows", "amiibos", "funkos");

    /**
     * Colecciones cuyo servicio valida la unicidad de {@code externalId} en cada operación y
     * lanza 409 en duplicado. El índice único es el respaldo que elimina la carrera entre esa
     * comprobación y la escritura.
     *
     * <p>Se excluyen {@code games} y {@code board_games} porque sus servicios no validan
     * unicidad: añadir el índice cambiaría el comportamiento y podría romper datos existentes.
     */
    private static final List<String> EXTERNAL_ID_COLLECTIONS =
            List.of("books", "movie_shows", "amiibos", "funkos");

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
            for (String collection : EXTERNAL_ID_COLLECTIONS) {
                mongoTemplate.indexOps(collection).ensureIndex(partialUniqueExternalIdIndex());
            }
        } catch (RuntimeException e) {
            log.warn("Creación de índices omitida (Mongo no disponible): {}", e.getMessage());
        }
    }

    /**
     * Índice único <strong>parcial</strong> sobre {@code externalId}: solo indexa los valores
     * string no vacíos.
     *
     * <p>Un índice único normal haría que todos los documentos sin {@code externalId} (los
     * añadidos a mano, sin correspondencia en el catálogo externo) colisionaran entre sí en el
     * valor {@code null}, porque Mongo trata los campos ausentes como {@code null} a efectos de
     * unicidad. El filtro parcial descarta esos documentos del índice, así que solo compiten
     * los identificadores externos reales.
     */
    private static Index partialUniqueExternalIdIndex() {
        Document nonBlankString = new Document("$type", "string").append("$gt", "");
        return new Index()
                .on("externalId", Sort.Direction.ASC)
                .unique()
                .named("externalId_unique_non_blank")
                .partial(PartialIndexFilter.of(new Document("externalId", nonBlankString)));
    }
}
