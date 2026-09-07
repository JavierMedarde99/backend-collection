package com.wikicollection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class GameTest {

    @Test
    void builder_setsAllFields() {
        Game game = Game.builder()
                .id("g1")
                .externalId("rawg-001")
                .title("The Witcher 3")
                .platform(GamePlatform.PC)
                .thumbnailUrl("http://img")
                .status(GameStatus.PLAYING)
                .userRating(5)
                .comment("Gran historia")
                .dateAdded(LocalDate.of(2024, 1, 1))
                .dateCompleted(LocalDate.of(2024, 2, 1))
                .externalSource("RAWG")
                .steamAppId("570")
                .build();

        assertThat(game.getId()).isEqualTo("g1");
        assertThat(game.getExternalId()).isEqualTo("rawg-001");
        assertThat(game.getTitle()).isEqualTo("The Witcher 3");
        assertThat(game.getPlatform()).isEqualTo(GamePlatform.PC);
        assertThat(game.getThumbnailUrl()).isEqualTo("http://img");
        assertThat(game.getStatus()).isEqualTo(GameStatus.PLAYING);
        assertThat(game.getUserRating()).isEqualTo(5);
        assertThat(game.getComment()).isEqualTo("Gran historia");
        assertThat(game.getDateAdded()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(game.getDateCompleted()).isEqualTo(LocalDate.of(2024, 2, 1));
        assertThat(game.getExternalSource()).isEqualTo("RAWG");
        assertThat(game.getSteamAppId()).isEqualTo("570");
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