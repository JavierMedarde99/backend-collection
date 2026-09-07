package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.wikicollection.application.exception.GameNotFoundException;
import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.SteamAchievement;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.domain.port.out.SteamCatalogueClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameAchievementsServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private SteamCatalogueClient steamCatalogueClient;

    @InjectMocks
    private GameAchievementsService gameAchievementsService;

    private Game sampleGameLinkedToSteam() {
        return Game.builder()
                .id("g1")
                .title("Half-Life")
                .steamAppId("70")
                .build();
    }

    @Test
    void getAchievements_combinesSchemaWithPlayerProgress() {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(sampleGameLinkedToSteam()));
        when(steamCatalogueClient.getGameSchema(70L)).thenReturn(List.of(
                new SteamAchievement("ACH_BORN", false, "El nacimiento", "Comienza la aventura.", "http://icon"),
                new SteamAchievement("ACH_END", false, "El final", "Termina la aventura.", "http://icon2")));
        when(steamCatalogueClient.getPlayerAchievements(70L, "7656"))
                .thenReturn(List.of(new SteamAchievement("ACH_BORN", true, "El nacimiento", "Comienza la aventura.", null)));

        List<SteamAchievement> result = gameAchievementsService.getAchievements("g1", "7656");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).achieved()).isTrue();
        assertThat(result.get(0).name()).isEqualTo("El nacimiento");
        assertThat(result.get(0).description()).isEqualTo("Comienza la aventura.");
        assertThat(result.get(0).iconUrl()).isEqualTo("http://icon");
        assertThat(result.get(1).achieved()).isFalse();
        assertThat(result.get(1).name()).isEqualTo("El final");
    }

    @Test
    void getAchievements_returnsPlayerAchievements_whenSchemaEmpty() {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(sampleGameLinkedToSteam()));
        when(steamCatalogueClient.getGameSchema(70L)).thenReturn(List.of());
        when(steamCatalogueClient.getPlayerAchievements(70L, "7656"))
                .thenReturn(List.of(new SteamAchievement("ACH_BORN", true, "El nacimiento", "Comienza la aventura.", null)));

        List<SteamAchievement> result = gameAchievementsService.getAchievements("g1", "7656");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).achieved()).isTrue();
    }

    @Test
    void getAchievements_returnsEmptyList_whenNothingFound() {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(sampleGameLinkedToSteam()));
        when(steamCatalogueClient.getGameSchema(70L)).thenReturn(List.of());
        when(steamCatalogueClient.getPlayerAchievements(70L, "7656")).thenReturn(List.of());

        assertThat(gameAchievementsService.getAchievements("g1", "7656")).isEmpty();
    }

    @Test
    void getAchievements_throwsNotFound_whenGameMissing() {
        when(gameRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameAchievementsService.getAchievements("nope", "7656"))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void getAchievements_throwsBadRequest_whenNoSteamAppId() {
        Game game = sampleGameLinkedToSteam();
        game.setSteamAppId(null);
        when(gameRepository.findById("g1")).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameAchievementsService.getAchievements("g1", "7656"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Steam");
    }

    @Test
    void getAchievements_throwsBadRequest_whenSteamIdBlank() {
        assertThatThrownBy(() -> gameAchievementsService.getAchievements("g1", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("steamId");
    }
}