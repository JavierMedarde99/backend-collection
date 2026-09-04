package com.wikicollection.infrastructure.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameStatus;

import org.junit.jupiter.api.Test;

class GameDtoMapperTest {

    private final GameDtoMapper mapper = new GameDtoMapper();

    @Test
    void toDomain_mapsAllRequestFields() {
        GameRequest request = new GameRequest(
                "external-1", "The Witcher 3", "Aventura", "RPG", GamePlatform.PC,
                "CD Projekt Red", "CD Projekt Red", LocalDate.of(2015, 5, 19),
                "http://img", GameStatus.PLAYING, 5, "Notas",
                LocalDateTime.of(2024, 1, 1, 10, 0), LocalDateTime.of(2024, 2, 1, 10, 0),
                "RAWG");

        Game game = mapper.toDomain(request);

        assertThat(game.getExternalId()).isEqualTo("external-1");
        assertThat(game.getTitle()).isEqualTo("The Witcher 3");
        assertThat(game.getDescription()).isEqualTo("Aventura");
        assertThat(game.getGenre()).isEqualTo("RPG");
        assertThat(game.getPlatform()).isEqualTo(GamePlatform.PC);
        assertThat(game.getPublisher()).isEqualTo("CD Projekt Red");
        assertThat(game.getDeveloper()).isEqualTo("CD Projekt Red");
        assertThat(game.getReleaseDate()).isEqualTo(LocalDate.of(2015, 5, 19));
        assertThat(game.getThumbnailUrl()).isEqualTo("http://img");
        assertThat(game.getStatus()).isEqualTo(GameStatus.PLAYING);
        assertThat(game.getUserRating()).isEqualTo(5);
        assertThat(game.getNotes()).isEqualTo("Notas");
        assertThat(game.getDateAdded()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0));
        assertThat(game.getDateCompleted()).isEqualTo(LocalDateTime.of(2024, 2, 1, 10, 0));
        assertThat(game.getExternalSource()).isEqualTo("RAWG");
    }

    @Test
    void toResponse_mapsAllGameFields() {
        Game game = Game.builder()
                .id("g1")
                .externalId("external-1")
                .title("The Witcher 3")
                .description("Aventura")
                .genre("RPG")
                .platform(GamePlatform.PC)
                .publisher("CD Projekt Red")
                .developer("CD Projekt Red")
                .releaseDate(LocalDate.of(2015, 5, 19))
                .thumbnailUrl("http://img")
                .status(GameStatus.COMPLETED)
                .userRating(5)
                .notes("Notas")
                .dateAdded(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dateCompleted(LocalDateTime.of(2024, 2, 1, 10, 0))
                .externalSource("RAWG")
                .build();

        GameResponse response = mapper.toResponse(game);

        assertThat(response.id()).isEqualTo("g1");
        assertThat(response.externalId()).isEqualTo("external-1");
        assertThat(response.title()).isEqualTo("The Witcher 3");
        assertThat(response.description()).isEqualTo("Aventura");
        assertThat(response.genre()).isEqualTo("RPG");
        assertThat(response.platform()).isEqualTo(GamePlatform.PC);
        assertThat(response.publisher()).isEqualTo("CD Projekt Red");
        assertThat(response.developer()).isEqualTo("CD Projekt Red");
        assertThat(response.releaseDate()).isEqualTo(LocalDate.of(2015, 5, 19));
        assertThat(response.thumbnailUrl()).isEqualTo("http://img");
        assertThat(response.status()).isEqualTo(GameStatus.COMPLETED);
        assertThat(response.userRating()).isEqualTo(5);
        assertThat(response.notes()).isEqualTo("Notas");
        assertThat(response.dateAdded()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0));
        assertThat(response.dateCompleted()).isEqualTo(LocalDateTime.of(2024, 2, 1, 10, 0));
        assertThat(response.externalSource()).isEqualTo("RAWG");
    }

    @Test
    void toDomain_returnsNull_whenRequestIsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toResponse_returnsNull_whenGameIsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }
}