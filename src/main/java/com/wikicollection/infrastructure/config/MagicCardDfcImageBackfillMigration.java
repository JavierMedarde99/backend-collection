package com.wikicollection.infrastructure.config;

import java.util.List;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;

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
 * Rellena el arte de las cartas de doble cara guardadas antes de corregir el mapper (#510):
 * Scryfall manda {@code image_uris: null} en esas cartas y las imágenes viven en
 * {@code card_faces}, así que hasta ahora se persistieron con {@code imageUrl} nulo.
 *
 * <p>No basta con el mapper arreglado: las cartas ya guardadas siguen sin imagen hasta que
 * alguien las re-importe. Se re-consulta Scryfall por {@code scryfallId} (el cliente ya va
 * espaciado) y se actualizan <b>solo</b> los campos de imagen.
 *
 * <p>Idempotente: solo mira documentos con {@code imageUrl} nulo, y un documento que no
 * devuelva arte se deja como está. Best-effort: si una carta falla, se salta y sigue.
 */
@Component
@Slf4j
public class MagicCardDfcImageBackfillMigration implements ApplicationRunner {

    static final String COLLECTION = "magic_cards";

    /**
     * Layouts en los que Scryfall manda las imágenes por cara. {@code split}, {@code flip},
     * {@code adventure} y compañía sí traen {@code image_uris} en la raíz, así que no entran.
     */
    static final List<String> DFC_LAYOUTS =
            List.of("transform", "modal_dfc", "double_faced_token", "art_series");

    private final MongoTemplate mongoTemplate;
    private final ExternalMagicCardCatalogClient scryfallClient;
    private final boolean enabled;

    public MagicCardDfcImageBackfillMigration(MongoTemplate mongoTemplate,
                                              ExternalMagicCardCatalogClient scryfallClient,
                                              @Value("${app.migration.enabled:true}") boolean enabled) {
        this.mongoTemplate = mongoTemplate;
        this.scryfallClient = scryfallClient;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        List<Document> sinArte;
        try {
            sinArte = mongoTemplate.find(pendientes(), Document.class, COLLECTION);
        } catch (RuntimeException e) {
            log.warn("Backfill de arte de cartas DFC omitido (Mongo no disponible): {}", e.getMessage());
            return;
        }
        if (sinArte.isEmpty()) {
            return;
        }
        log.info("Revisando {} cartas de doble cara sin imagen (#510)", sinArte.size());
        int arregladas = 0;
        for (Document doc : sinArte) {
            if (backfill(doc)) {
                arregladas++;
            }
        }
        log.info("Cartas de doble cara con imagen restaurada: {}", arregladas);
    }

    private Query pendientes() {
        Query query = new Query(new Criteria().andOperator(
                Criteria.where("layout").in(DFC_LAYOUTS),
                Criteria.where("scryfallId").exists(true),
                new Criteria().orOperator(
                        Criteria.where("imageUrl").exists(false),
                        Criteria.where("imageUrl").is(null))));
        query.fields().include("_id").include("scryfallId");
        return query;
    }

    /** @return true si se actualizó el documento */
    private boolean backfill(Document doc) {
        String scryfallId = doc.getString("scryfallId");
        if (scryfallId == null || scryfallId.isBlank()) {
            return false;
        }
        MagicCard card;
        try {
            card = scryfallClient.findById(scryfallId);
        } catch (RuntimeException e) {
            log.warn("No se pudo re-consultar la carta {} en Scryfall: {}", scryfallId, e.getMessage());
            return false;
        }
        if (card == null || card.getImageUrl() == null) {
            return false;
        }
        Update update = new Update()
                .set("imageUrl", card.getImageUrl())
                .set("imageLargeUrl", card.getImageLargeUrl())
                .set("imageUrlBack", card.getImageUrlBack())
                .set("imageLargeUrlBack", card.getImageLargeUrlBack());
        mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(doc.get("_id"))), update, COLLECTION);
        return true;
    }
}
