package com.wikicollection.infrastructure.adapter.out.scryfall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchResult;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

class ScryfallClientTest {

    private MockWebServer server;
    private ScryfallClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new ScryfallClient(new RestTemplate(), server.url("").toString(), 0, 0, new MagicCardMapper());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void search_mapsScryfallListResponse() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(searchFixture()));

        List<MagicCardSearchResult> results = client.search("lightning");

        assertThat(results).hasSize(1);
        MagicCardSearchResult result = results.get(0);
        assertThat(result.scryfallId()).isEqualTo("id-1");
        assertThat(result.name()).isEqualTo("Lightning Bolt");
        assertThat(result.manaCost()).isEqualTo("{R}");
        assertThat(result.type()).isEqualTo("Instant");
        assertThat(result.rarity()).isEqualTo("uncommon");
        assertThat(result.setCode()).isEqualTo("msc");
        assertThat(result.priceUsd()).isEqualTo("0.65");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/cards/search?q=lightning");
    }

    @Test
    void search_returnsEmpty_onEmptyList() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"object\":\"list\",\"data\":[]}"));

        List<MagicCardSearchResult> results = client.search("nada");

        assertThat(results).isEmpty();
    }

    @Test
    void search_returnsEmpty_whenBlankQuery() {
        assertThat(client.search("  ")).isEmpty();
        assertThat(client.search(null)).isEmpty();
    }

    @Test
    void retriesAndReturnsEmpty_whenServerError() throws Exception {
        for (int i = 0; i < 4; i++) {
            server.enqueue(new MockResponse().setResponseCode(503));
        }

        ScryfallClient retrying = new ScryfallClient(new RestTemplate(), server.url("").toString(), 3, 0, new MagicCardMapper());
        List<MagicCardSearchResult> results = retrying.search("catan");

        assertThat(results).isEmpty();
        assertThat(server.getRequestCount()).isEqualTo(4);
    }

    @Test
    void findById_mapsSingleCard() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(cardFixture()));

        MagicCard card = client.findById("id-1");

        assertThat(card.getScryfallId()).isEqualTo("id-1");
        assertThat(card.getName()).isEqualTo("Lightning Bolt");
        assertThat(card.getPriceUsd()).isEqualTo("0.65");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/cards/id-1");
    }

    @Test
    void findById_rethrows_whenNotFound() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404));

        assertThatThrownBy(() -> client.findById("missing"))
                .isInstanceOf(RestClientResponseException.class);
    }

    @Test
    void findByName_mapsFuzzyCard() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(cardFixture()));

        MagicCard card = client.findByName("lightning bollt");

        assertThat(card.getName()).isEqualTo("Lightning Bolt");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).startsWith("/cards/named?fuzzy=lightning");
    }

    private String searchFixture() {
        return """
                {
                  "object": "list",
                  "total_cards": 1,
                  "data": [
                    {
                      "id": "id-1",
                      "oracle_id": "oracle-1",
                      "name": "Lightning Bolt",
                      "released_at": "2026-06-26",
                      "mana_cost": "{R}",
                      "cmc": 1.0,
                      "type_line": "Instant",
                      "oracle_text": "Lightning Bolt deals 3 damage to any target.",
                      "colors": ["R"],
                      "color_identity": ["R"],
                      "keywords": [],
                      "rarity": "uncommon",
                      "set": "msc",
                      "set_name": "Marvel Super Heroes Commander",
                      "layout": "normal",
                      "legalities": {"standard": "legal"},
                      "prices": {"usd": "0.65", "eur": "2.02"},
                      "image_uris": {
                        "normal": "http://normal",
                        "large": "http://large",
                        "art_crop": "http://crop"
                      }
                    }
                  ]
                }
                """;
    }

    private String cardFixture() {
        return """
                {
                  "object": "card",
                  "id": "id-1",
                  "oracle_id": "oracle-1",
                  "name": "Lightning Bolt",
                  "released_at": "2026-06-26",
                  "mana_cost": "{R}",
                  "cmc": 1.0,
                  "type_line": "Instant",
                  "oracle_text": "Lightning Bolt deals 3 damage to any target.",
                  "colors": ["R"],
                  "color_identity": ["R"],
                  "keywords": [],
                  "rarity": "uncommon",
                  "set": "msc",
                  "set_name": "Marvel Super Heroes Commander",
                  "artist": "Milivoj",
                  "frame": "2015",
                  "border_color": "black",
                  "layout": "normal",
                  "legalities": {"standard": "legal"},
                  "prices": {"usd": "0.65", "eur": "2.02"},
                  "image_uris": {
                    "normal": "http://normal",
                    "large": "http://large",
                    "art_crop": "http://crop"
                  }
                }
                """;
    }
}
