package com.wikicollection.infrastructure.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameStatus;
import com.wikicollection.domain.model.Difficulty;

import org.junit.jupiter.api.Test;

class BoardGameDtoMapperTest {

    private final BoardGameDtoMapper mapper = new BoardGameDtoMapper();

    private BoardGame sampleBoardGame() {
        return BoardGame.builder()
                .id("bg1")
                .ownerId("owner1")
                .title("Catan")
                .genres(List.of("Estrategia"))
                .description("Comercia, construye y coloniza")
                .yearPublished(1995)
                .minPlayers(3)
                .maxPlayers(4)
                .minPlaytime(60)
                .maxPlaytime(120)
                .publisher("Kosmos")
                .designers(List.of("Klaus Teuber"))
                .categories(List.of("Comercio"))
                .mechanics(List.of("Tirada de dados"))
                .imageUrl("http://img")
                .thumbnailUrl("http://thumb")
                .bggRating(new java.math.BigDecimal("7.2"))
                .bggId("13")
                .status(BoardGameStatus.OWNED)
                .notes("Nota privada")
                .dateAdded(LocalDate.of(2024, 1, 1))
                .personalRating(4)
                .playCount(7)
                .lastPlayedDate(LocalDate.of(2024, 6, 15))
                .difficulty(Difficulty.MEDIUM)
                .build();
    }

    @Test
    void toResponse_mapsPersonalFields() {
        BoardGameResponse response = mapper.toResponse(sampleBoardGame());

        assertThat(response.personalRating()).isEqualTo(4);
        assertThat(response.playCount()).isEqualTo(7);
        assertThat(response.lastPlayedDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(response.difficulty()).isEqualTo(Difficulty.MEDIUM);
    }

    @Test
    void withoutPrivate_hidesPersonalFields() {
        BoardGameResponse response = mapper.toResponse(sampleBoardGame());

        BoardGameResponse stripped = response.withoutPrivate();

        assertThat(stripped.personalRating()).isNull();
        assertThat(stripped.playCount()).isNull();
        assertThat(stripped.lastPlayedDate()).isNull();
        assertThat(stripped.difficulty()).isNull();
        assertThat(stripped.notes()).isNull();
        assertThat(stripped.title()).isEqualTo("Catan");
        assertThat(stripped.id()).isEqualTo("bg1");
    }

    @Test
    void toResponse_returnsNull_whenBoardGameNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}
