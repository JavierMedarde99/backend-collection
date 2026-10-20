package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

/**
 * El mapper arreglado solo cubre las cartas que se guarden a partir de ahora: las DFC ya
 * persistidas con imageUrl nulo siguen sin arte hasta que alguien las re-importe (#510).
 */
@ExtendWith(MockitoExtension.class)
class MagicCardDfcImageBackfillMigrationTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private ExternalMagicCardCatalogClient scryfallClient;

    private MagicCardDfcImageBackfillMigration migration(boolean enabled) {
        return new MagicCardDfcImageBackfillMigration(mongoTemplate, scryfallClient, enabled);
    }

    private Document dfcGuardada() {
        return new Document("_id", new ObjectId())
                .append("scryfallId", "id-dfc")
                .append("layout", "transform")
                .append("imageUrl", null);
    }

    private MagicCard conArte() {
        return MagicCard.builder()
                .scryfallId("id-dfc")
                .imageUrl("http://front")
                .imageLargeUrl("http://front-large")
                .imageUrlBack("http://back")
                .imageLargeUrlBack("http://back-large")
                .build();
    }

    @Test
    void run_backfilleaElArteDeLasDfcYaGuardadas() {
        Document doc = dfcGuardada();
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq("magic_cards")))
                .thenReturn(List.of(doc));
        when(scryfallClient.findById("id-dfc")).thenReturn(conArte());

        migration(true).run(null);

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq("magic_cards"));
        Update update = updateCaptor.getValue();
        assertThat(update.getUpdateObject().toJson())
                .contains("http://front").contains("http://front-large")
                .contains("http://back").contains("http://back-large");
    }

    @Test
    void run_soloEscribeLosCamposDeImagen() {
        Document doc = dfcGuardada();
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq("magic_cards")))
                .thenReturn(List.of(doc));
        when(scryfallClient.findById("id-dfc")).thenReturn(conArte());

        migration(true).run(null);

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq("magic_cards"));
        String json = updateCaptor.getValue().getUpdateObject().toJson();
        assertThat(json).doesNotContain("name").doesNotContain("priceUsd").doesNotContain("quantity");
    }

    @Test
    void run_omiteUnaCartaQueFallaY_SigueConLasDemas() {
        Document rota = dfcGuardada();
        Document sana = new Document("_id", new ObjectId())
                .append("scryfallId", "id-sana").append("layout", "modal_dfc").append("imageUrl", null);
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq("magic_cards")))
                .thenReturn(List.of(rota, sana));
        when(scryfallClient.findById("id-dfc")).thenThrow(new IllegalStateException("Scryfall no disponible"));
        when(scryfallClient.findById("id-sana")).thenReturn(conArte());

        migration(true).run(null);

        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq("magic_cards"));
    }

    @Test
    void run_noEscribeNada_cuandoScryfallNoDaArte() {
        Document doc = dfcGuardada();
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq("magic_cards")))
                .thenReturn(List.of(doc));
        when(scryfallClient.findById("id-dfc")).thenReturn(MagicCard.builder().scryfallId("id-dfc").build());

        migration(true).run(null);

        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), any(String.class));
    }

    @Test
    void run_omiteDocumentosSinScryfallId() {
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq("magic_cards")))
                .thenReturn(List.of(new Document("_id", new ObjectId()).append("layout", "transform")));

        migration(true).run(null);

        verify(scryfallClient, never()).findById(any(String.class));
        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), any(String.class));
    }

    @Test
    void run_doesNothing_whenDisabled() {
        migration(false).run(null);

        verify(mongoTemplate, never()).find(any(Query.class), eq(Document.class), any(String.class));
    }

    @Test
    void run_noRevienta_cuandoMongoNoEstaDisponible() {
        when(mongoTemplate.find(any(Query.class), eq(Document.class), eq("magic_cards")))
                .thenThrow(new IllegalStateException("Mongo caido"));

        migration(true).run(null);
    }
}
