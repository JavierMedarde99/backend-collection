package com.wikicollection.infrastructure.adapter.out.bgg.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.wikicollection.domain.model.BoardGameSearchResult;

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
}