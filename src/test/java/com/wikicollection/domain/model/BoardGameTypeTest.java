package com.wikicollection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

class BoardGameTypeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void jsonValue_usesBggVocabulary() throws Exception {
        assertThat(objectMapper.writeValueAsString(BoardGameType.BOARDGAME)).isEqualTo("\"boardgame\"");
        assertThat(objectMapper.writeValueAsString(BoardGameType.BOARDGAME_EXPANSION))
                .isEqualTo("\"boardgameexpansion\"");
    }

    @Test
    void jsonCreator_readsBggKeysIgnoringCase() throws Exception {
        assertThat(objectMapper.readValue("\"boardgame\"", BoardGameType.class)).isEqualTo(BoardGameType.BOARDGAME);
        assertThat(objectMapper.readValue("\"BOARDGAMEEXPANSION\"", BoardGameType.class))
                .isEqualTo(BoardGameType.BOARDGAME_EXPANSION);
        assertThat(objectMapper.readValue("\"  BoardGameExpansion \"", BoardGameType.class))
                .isEqualTo(BoardGameType.BOARDGAME_EXPANSION);
    }

    @Test
    void fromKey_returnsNull_whenNull() {
        assertThat(BoardGameType.fromKey(null)).isNull();
    }

    @Test
    void fromKey_throws_whenUnknown() {
        assertThatThrownBy(() -> BoardGameType.fromKey("boardgameaccessory"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boardgameaccessory");
    }
}
