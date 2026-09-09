package com.wikicollection.infrastructure.adapter.out.bgg.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.wikicollection.domain.model.BoardGameSearchResult;
import com.wikicollection.infrastructure.adapter.out.bgg.mapper.BoardGameXmlMapper.BggXmlItem;

import org.junit.jupiter.api.Test;

class BoardGameXmlMapperTest {

    private final BoardGameXmlMapper mapper = new BoardGameXmlMapper();

    @Test
    void map_convertsNativeXmlToSearchResult() {
        String xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <items total="1">
                  <item type="boardgame" id="31260">
                    <name type="primary" sortindex="1" value="Catan"/>
                    <yearpublished value="2007"/>
                    <image>http://img</image>
                    <thumbnail>http://thumb</thumbnail>
                  </item>
                </items>
                """;

        List<BoardGameSearchResult> results = mapper.map(xml);

        assertThat(results).hasSize(1);
        BoardGameSearchResult result = results.get(0);
        assertThat(result.bggId()).isEqualTo("31260");
        assertThat(result.title()).isEqualTo("Catan");
        assertThat(result.yearPublished()).isEqualTo(2007);
        assertThat(result.imageUrl()).isEqualTo("http://img");
        assertThat(result.thumbnailUrl()).isEqualTo("http://thumb");
        assertThat(result.externalSource()).isEqualTo("BGG");
        assertThat(result.description()).isNull();
        assertThat(result.minPlayers()).isNull();
    }

    @Test
    void map_ignoresUnknownElementsAndAttributes() {
        String xml = """
                <items total="2" foo="bar">
                  <item type="expansion" id="13">
                    <name type="primary" value="Expansión"/>
                    <yearpublished value="2000"/>
                    <unexpected>campo desconocido</unexpected>
                  </item>
                </items>
                """;

        List<BoardGameSearchResult> results = mapper.map(xml);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).bggId()).isEqualTo("13");
        assertThat(results.get(0).title()).isEqualTo("Expansión");
    }

    @Test
    void map_returnsEmpty_whenNull() {
        assertThat(mapper.map(null)).isEmpty();
    }

    @Test
    void map_returnsEmpty_whenBlank() {
        assertThat(mapper.map("   ")).isEmpty();
    }

    @Test
    void map_returnsEmpty_whenNoItems() {
        assertThat(mapper.map("<items total=\"0\"></items>")).isEmpty();
    }

    @Test
    void map_throws_whenMalformedXml() {
        assertThatThrownBy(() -> mapper.map("<items><item></items>"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mapThing_convertsThingXmlToDetailMap() {
        Map<String, BggXmlItem> things = mapper.mapThing(bggThingFixture());

        assertThat(things).hasSize(1);
        BggXmlItem thing = things.get("31260");
        assertThat(thing.description).isEqualTo("Un juego de colonización");
        assertThat(thing.minPlayers.value).isEqualTo(3);
        assertThat(thing.maxPlayers.value).isEqualTo(4);
        assertThat(thing.minPlaytime.value).isEqualTo(60);
        assertThat(thing.maxPlaytime.value).isEqualTo(120);
        assertThat(thing.statistics.ratings.average.value).isEqualByComparingTo(new BigDecimal("8.3"));
        assertThat(thing.statistics.ratings.bayesAverage.value).isEqualByComparingTo(new BigDecimal("7.9"));
        assertThat(thing.links).hasSize(6);
    }

    @Test
    void mapThing_returnsEmpty_whenNull() {
        assertThat(mapper.mapThing(null)).isEmpty();
    }

    @Test
    void mapThing_returnsEmpty_whenBlank() {
        assertThat(mapper.mapThing("   ")).isEmpty();
    }

    @Test
    void enrich_populatesAllFieldsFromThingDetails() {
        BoardGameSearchResult basic = new BoardGameSearchResult(
                "31260", "Catan", null, 2007, null, null, null, null,
                null, null, null, null,
                "http://img", "http://thumb", null, "BGG");
        BggXmlItem thing = mapper.mapThing(bggThingFixture()).get("31260");

        BoardGameSearchResult enriched = mapper.enrich(basic, thing);

        assertThat(enriched.bggId()).isEqualTo("31260");
        assertThat(enriched.title()).isEqualTo("Catan");
        assertThat(enriched.description()).isEqualTo("Un juego de colonización");
        assertThat(enriched.yearPublished()).isEqualTo(2007);
        assertThat(enriched.minPlayers()).isEqualTo(3);
        assertThat(enriched.maxPlayers()).isEqualTo(4);
        assertThat(enriched.minPlaytime()).isEqualTo(60);
        assertThat(enriched.maxPlaytime()).isEqualTo(120);
        assertThat(enriched.publisher()).isEqualTo("Kosmos");
        assertThat(enriched.designers()).containsExactly("Klaus Teuber");
        assertThat(enriched.categories()).containsExactly("Negociación", "Estrategia");
        assertThat(enriched.mechanics()).containsExactly("Dados", "Colocación de losetas");
        assertThat(enriched.bggRating()).isEqualByComparingTo(new BigDecimal("8.3"));
        assertThat(enriched.imageUrl()).isEqualTo("http://img");
        assertThat(enriched.thumbnailUrl()).isEqualTo("http://thumb");
        assertThat(enriched.externalSource()).isEqualTo("BGG");
    }

    @Test
    void enrich_fillsImageAndThumbnailFromThing_whenBasicResultHasNone() {
        BoardGameSearchResult basic = new BoardGameSearchResult(
                "31260", "Catan", null, 2007, null, null, null, null,
                null, null, null, null,
                null, null, null, "BGG");
        BggXmlItem thing = mapper.mapThing(bggThingFixture()).get("31260");

        BoardGameSearchResult enriched = mapper.enrich(basic, thing);

        assertThat(enriched.imageUrl()).isEqualTo("http://img");
        assertThat(enriched.thumbnailUrl()).isEqualTo("http://thumb");
    }

    @Test
    void enrich_returnsSameResult_whenThingIsNull() {
        BoardGameSearchResult basic = new BoardGameSearchResult(
                "31260", "Catan", null, 2007, null, null, null, null,
                null, null, null, null,
                "http://img", "http://thumb", null, "BGG");

        BoardGameSearchResult enriched = mapper.enrich(basic, null);

        assertThat(enriched).isSameAs(basic);
    }

    private String bggThingFixture() {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <items termsofuse="https://boardgamegeek.com/xmlapi/termsofuse">
                  <item type="boardgame" id="31260">
                    <name type="primary" sortindex="1" value="Catan"/>
                    <name type="alternate" sortindex="1" value="Los Colonos de Catan"/>
                    <description>Un juego de colonización</description>
                    <yearpublished value="2007"/>
                    <minplayers value="3"/>
                    <maxplayers value="4"/>
                    <minplaytime value="60"/>
                    <maxplaytime value="120"/>
                    <image>http://img</image>
                    <thumbnail>http://thumb</thumbnail>
                    <link type="boardgamepublisher" id="253" value="Kosmos"/>
                    <link type="boardgamedesigner" id="20" value="Klaus Teuber"/>
                    <link type="boardgamecategory" id="1028" value="Negociación"/>
                    <link type="boardgamecategory" id="1017" value="Estrategia"/>
                    <link type="boardgamemechanic" id="2011" value="Dados"/>
                    <link type="boardgamemechanic" id="2008" value="Colocación de losetas"/>
                    <statistics page="1">
                      <ratings>
                        <usersrated value="50000"/>
                        <average value="8.3"/>
                        <bayesaverage value="7.9"/>
                        <ranks>
                          <rank type="subtype" id="1" name="boardgame" friendlyname="Board Game Rank" value="42" bayesaverage="7.9"/>
                        </ranks>
                      </ratings>
                    </statistics>
                  </item>
                </items>
                """;
    }
}