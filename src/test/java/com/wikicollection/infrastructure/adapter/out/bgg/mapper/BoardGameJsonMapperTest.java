package com.wikicollection.infrastructure.adapter.out.bgg.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.wikicollection.domain.model.BoardGameSearchResult;

import org.junit.jupiter.api.Test;

class BoardGameJsonMapperTest {

    private final BoardGameJsonMapper mapper = new BoardGameJsonMapper();

    @Test
    void map_convertsNativeJsonToSearchResult() {
        BoardGameJsonMapper.BggJsonGame game = new BoardGameJsonMapper.BggJsonGame(
                "31260", "Catan", "Descripción", 2007, 3, 4, 60, 120,
                List.of("Kosmos"), List.of("Klaus Teuber"), List.of("Negociación"), List.of("Trading"),
                "http://img", "http://thumb");

        List<BoardGameSearchResult> results = mapper.map(new BoardGameJsonMapper.BggJsonGame[]{game});

        assertThat(results).hasSize(1);
        BoardGameSearchResult result = results.get(0);
        assertThat(result.bggId()).isEqualTo("31260");
        assertThat(result.title()).isEqualTo("Catan");
        assertThat(result.description()).isEqualTo("Descripción");
        assertThat(result.yearPublished()).isEqualTo(2007);
        assertThat(result.minPlayers()).isEqualTo(3);
        assertThat(result.maxPlayers()).isEqualTo(4);
        assertThat(result.minPlaytime()).isEqualTo(60);
        assertThat(result.maxPlaytime()).isEqualTo(120);
        assertThat(result.publisher()).isEqualTo("Kosmos");
        assertThat(result.designers()).containsExactly("Klaus Teuber");
        assertThat(result.categories()).containsExactly("Negociación");
        assertThat(result.mechanics()).containsExactly("Trading");
        assertThat(result.imageUrl()).isEqualTo("http://img");
        assertThat(result.thumbnailUrl()).isEqualTo("http://thumb");
        assertThat(result.externalSource()).isEqualTo("BGG");
    }

    @Test
    void map_usesFirstPublisherAndToleratesMissingLists() {
        BoardGameJsonMapper.BggJsonGame game = new BoardGameJsonMapper.BggJsonGame(
                "1", "Juego", null, null, null, null, null, null,
                List.of("Editorial Uno", "Editorial Dos"), null, null, null, null, null);

        List<BoardGameSearchResult> results = mapper.map(new BoardGameJsonMapper.BggJsonGame[]{game});

        assertThat(results.get(0).publisher()).isEqualTo("Editorial Uno");
        assertThat(results.get(0).designers()).isEmpty();
        assertThat(results.get(0).categories()).isEmpty();
        assertThat(results.get(0).mechanics()).isEmpty();
        assertThat(results.get(0).yearPublished()).isNull();
        assertThat(results.get(0).bggRating()).isNull();
    }

    @Test
    void map_returnsEmpty_whenNull() {
        assertThat(mapper.map(null)).isEmpty();
    }

    @Test
    void map_returnsEmpty_whenEmptyArray() {
        assertThat(mapper.map(new BoardGameJsonMapper.BggJsonGame[]{})).isEmpty();
    }

    @Test
    void map_returnsEmpty_whenSingleElementWithNullPublishers() {
        BoardGameJsonMapper.BggJsonGame game = new BoardGameJsonMapper.BggJsonGame(
                "1", "Juego", null, null, null, null, null, null,
                null, List.of(), List.of(), List.of(), null, null);

        List<BoardGameSearchResult> results = mapper.map(new BoardGameJsonMapper.BggJsonGame[]{game});

        assertThat(results.get(0).publisher()).isNull();
    }
}