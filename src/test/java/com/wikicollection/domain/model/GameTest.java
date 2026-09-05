package com.wikicollection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class GameTest {

    @Test
    void builder_setsAllFields() {
        Game game = Game.builder()
                .id("g1")
                .externalId("rawg-001")
                .title("The Witcher 3")
                .description("Aventura de rol")
                .genre("RPG")
                .platform(GamePlatform.PC)
                .publisher("CD Projekt")
                .developer("CD Projekt Red")
                .releaseDate(LocalDate.of(2015, 5, 19))
                .thumbnailUrl("http://img")
                .status(GameStatus.PLAYING)
                .userRating(5)
                .notes("Gran historia")
                .dateAdded(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dateCompleted(LocalDateTime.of(2024, 2, 1, 20, 0))
                .externalSource("RAWG")
                .build();

        assertThat(game.getId()).isEqualTo("g1");
        assertThat(game.getExternalId()).isEqualTo("rawg-001");
        assertThat(game.getTitle()).isEqualTo("The Witcher 3");
        assertThat(game.getDescription()).isEqualTo("Aventura de rol");
        assertThat(game.getGenre()).isEqualTo("RPG");
        assertThat(game.getPlatform()).isEqualTo(GamePlatform.PC);
        assertThat(game.getPublisher()).isEqualTo("CD Projekt");
        assertThat(game.getDeveloper()).isEqualTo("CD Projekt Red");
        assertThat(game.getReleaseDate()).isEqualTo(LocalDate.of(2015, 5, 19));
        assertThat(game.getThumbnailUrl()).isEqualTo("http://img");
        assertThat(game.getStatus()).isEqualTo(GameStatus.PLAYING);
        assertThat(game.getUserRating()).isEqualTo(5);
        assertThat(game.getNotes()).isEqualTo("Gran historia");
        assertThat(game.getDateAdded()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0));
        assertThat(game.getDateCompleted()).isEqualTo(LocalDateTime.of(2024, 2, 1, 20, 0));
        assertThat(game.getExternalSource()).isEqualTo("RAWG");
    }

    @Test
    void noArgsConstructor_allowsFieldMutation() {
        Game game = new Game();
        game.setTitle("Hollow Knight");
        game.setStatus(GameStatus.WISHLIST);

        assertThat(game.getTitle()).isEqualTo("Hollow Knight");
        assertThat(game.getStatus()).isEqualTo(GameStatus.WISHLIST);
    }

    @Test
    void gameStatus_containsExpectedValues() {
        assertThat(GameStatus.values()).containsExactly(
                GameStatus.PLAYING, GameStatus.COMPLETED, GameStatus.WISHLIST, GameStatus.ABANDONED);
    }

    @Test
    void gamePlatform_containsExpectedValues() {
        assertThat(GamePlatform.values()).containsExactly(
                GamePlatform.PC, GamePlatform.PS2, GamePlatform.PS3, GamePlatform.WII_U, GamePlatform.SWITCH);
    }

    @Test
    void gameSearchResult_recordExposesFields() {
        GameSearchResult result = new GameSearchResult(
                "rawg-002", "Hades", "Roguelike", "Action",
                GamePlatform.PC, "Supergiant", "Supergiant Games",
                LocalDate.of(2020, 9, 17), "http://thumb", "RAWG");

        assertThat(result.id()).isEqualTo("rawg-002");
        assertThat(result.title()).isEqualTo("Hades");
        assertThat(result.description()).isEqualTo("Roguelike");
        assertThat(result.genre()).isEqualTo("Action");
        assertThat(result.platform()).isEqualTo(GamePlatform.PC);
        assertThat(result.publisher()).isEqualTo("Supergiant");
        assertThat(result.developer()).isEqualTo("Supergiant Games");
        assertThat(result.releaseDate()).isEqualTo(LocalDate.of(2020, 9, 17));
        assertThat(result.thumbnailUrl()).isEqualTo("http://thumb");
        assertThat(result.externalSource()).isEqualTo("RAWG");
    }
}