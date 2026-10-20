package com.wikicollection.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import com.wikicollection.application.exception.ExternalCatalogException;
import com.wikicollection.application.exception.GameNotFoundException;
import com.wikicollection.domain.model.AchievementsSummary;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameAdditionsPage;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.model.GameTypeFilter;
import com.wikicollection.domain.model.PlatformInfo;
import com.wikicollection.domain.port.in.PlatformCatalogUseCase;
import com.wikicollection.domain.model.GameSearchResult;
import com.wikicollection.domain.model.GameStatus;
import com.wikicollection.domain.model.SteamAchievement;
import com.wikicollection.domain.port.in.GameAchievementsUseCase;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.domain.port.out.SteamCatalogueClient;
import com.wikicollection.infrastructure.adapter.out.freetogame.FreeToGameClient;
import com.wikicollection.infrastructure.adapter.out.rawg.RAWGClient;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class GameControllerTest {

    @MockitoBean
    private com.wikicollection.application.service.OwnerResolver ownerResolver;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameRepository gameRepository;

    /** Sin este mock la comprobación de administrador de OwnershipValidator consulta Mongo. */
    @MockitoBean
    private com.wikicollection.domain.port.out.UserRepository userRepository;

    @MockitoBean
    private RAWGClient rawgClient;

    @MockitoBean
    private FreeToGameClient freeToGameClient;

    @MockitoBean
    private GameAchievementsUseCase gameAchievementsUseCase;

    @MockitoBean
    private SteamCatalogueClient steamCatalogueClient;

    @MockitoBean
    private com.wikicollection.domain.port.in.UserPreferencesUseCase preferencesUseCase;

    @MockitoBean
    private PlatformCatalogUseCase platformCatalogUseCase;

    private Game sampleGame() {
        return Game.builder()
                .id("g1")
                .ownerId("u1")
                .title("The Witcher 3")
                .platform("PC")
                .status(GameStatus.PLAYING)
                .build();
    }

    @BeforeEach
    void stubOwnerResolver() {
        lenient().when(ownerResolver.resolveOwner(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> com.wikicollection.domain.model.UserOwned.builder()
                        .ownerId(invocation.getArgument(0)).ownerName("Javi").build());
    }

    @Test
    void platforms_returnsCatalogFromUseCase() throws Exception {
        when(platformCatalogUseCase.getPlatforms()).thenReturn(List.of(
                new PlatformInfo(1L, "PlayStation 5", "ps5"),
                new PlatformInfo(2L, "Web browser", "web")));

        mockMvc.perform(get("/api/v1/games/platforms").with(user("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("PlayStation 5"))
                .andExpect(jsonPath("$[0].slug").value("ps5"))
                .andExpect(jsonPath("$[1].name").value("Web browser"));
    }

    @Test
    void createGame_acceptsPlatformOutsideTheOldEnum() throws Exception {
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/games").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Unusual","platform":"PlayStation 5","status":"PLAYING"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.platform").value("PlayStation 5"));
    }

    @Test
    void createGame_returns400_whenPlatformIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/games").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Sin plataforma","platform":"","status":"PLAYING"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listGames_filtersByPlatformString() throws Exception {
        when(gameRepository.search(any(GameSearchCriteria.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/games").with(user("u1")).param("platform", "Xbox Series X"))
                .andExpect(status().isOk());

        ArgumentCaptor<GameSearchCriteria> captor = ArgumentCaptor.forClass(GameSearchCriteria.class);
        verify(gameRepository).search(captor.capture(), any(Pageable.class));
        assertThat(captor.getValue().platform()).isEqualTo("Xbox Series X");
    }

    @Test
    void listGames_returnsEmptyPage_whenNoGames() throws Exception {
        when(gameRepository.search(any(GameSearchCriteria.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/games").with(user("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listGames_returns400_whenNameTooLong() throws Exception {
        mockMvc.perform(get("/api/v1/games").with(user("u1")).param("name", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listGames_filtersByNamePlatformAndStatus() throws Exception {
        when(gameRepository.search(any(GameSearchCriteria.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/games").with(user("u1"))
                        .param("name", "witc")
                        .param("platform", "PC")
                        .param("status", "PLAYING"))
                .andExpect(status().isOk());

        ArgumentCaptor<GameSearchCriteria> captor = ArgumentCaptor.forClass(GameSearchCriteria.class);
        verify(gameRepository).search(captor.capture(), any(Pageable.class));
        GameSearchCriteria criteria = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(criteria.name()).isEqualTo("witc");
        org.assertj.core.api.Assertions.assertThat(criteria.platform()).isEqualTo("PC");
        org.assertj.core.api.Assertions.assertThat(criteria.status()).isEqualTo(GameStatus.PLAYING);
    }

    @Test
    void listGames_filtersByGenre() throws Exception {
        when(gameRepository.search(any(GameSearchCriteria.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/games").with(user("u1")).param("genre", "rpg"))
                .andExpect(status().isOk());

        ArgumentCaptor<GameSearchCriteria> captor = ArgumentCaptor.forClass(GameSearchCriteria.class);
        verify(gameRepository).search(captor.capture(), any(Pageable.class));
        org.assertj.core.api.Assertions.assertThat(captor.getValue().genres()).containsExactly("rpg");
    }

    @Test
    void getGame_returnsGame_whenExists() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(sampleGame()));

        mockMvc.perform(get("/api/v1/games/g1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("g1"))
                .andExpect(jsonPath("$.title").value("The Witcher 3"))
                .andExpect(jsonPath("$.status").value("PLAYING"));
    }

    @Test
    void getGame_hidesPrivateFields_whenNotOwner() throws Exception {
        Game game = sampleGame();
        game.setComment("privado");
        game.setUserRating(10);
        game.setUserOwned(new com.wikicollection.domain.model.UserOwned("u1", "Javi", "javi"));
        when(gameRepository.findById("g1")).thenReturn(Optional.of(game));
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.GAMES))
                .thenReturn(List.of("u1"));

        mockMvc.perform(get("/api/v1/games/g1").with(user("other")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.userRating").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.userOwned.ownerName").value("Javi"));
    }

    @Test
    void getGame_returns404_whenMissing() throws Exception {
        when(gameRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/games/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createGame_returns201_withLocation() throws Exception {
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game saved = invocation.getArgument(0);
            saved.setId("g-new");
            return saved;
        });

        mockMvc.perform(post("/api/v1/games").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"The Witcher 3","platform":"PC","status":"PLAYING"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/v1/games/g-new")))
                .andExpect(jsonPath("$.id").value("g-new"))
                .andExpect(jsonPath("$.title").value("The Witcher 3"));
    }

    @Test
    void createGame_returns400_whenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/games").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","platform":"PC","status":"PLAYING"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGame_returns400_whenUserRatingOutOfRange() throws Exception {
        mockMvc.perform(post("/api/v1/games").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"The Witcher 3","platform":"PC","status":"PLAYING","userRating":9}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGame_returns400_whenDateAddedIsFuture() throws Exception {
        mockMvc.perform(post("/api/v1/games").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"The Witcher 3","platform":"PC","status":"PLAYING","dateAdded":"2099-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Fechas mal formadas")));
    }

    @Test
    void createGame_returns400_whenDateAddedAfterDateCompleted() throws Exception {
        mockMvc.perform(post("/api/v1/games").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"The Witcher 3","platform":"PC","status":"PLAYING","dateAdded":"2024-06-01","dateCompleted":"2024-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Fechas mal formadas")));
    }

    @Test
    void updateGame_returns400_whenDateCompletedIsFuture() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(sampleGame()));

        mockMvc.perform(put("/api/v1/games/g1").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nuevo título","platform":"PC","status":"PLAYING","dateCompleted":"2099-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Fechas mal formadas")));
    }

    @Test
    void updateGame_persistsAndReturnsAcquisitionFields() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(sampleGame()));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/games/g1").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"The Witcher 3","platform":"PC","status":"COMPLETED",
                                 "acquisitionDate":"2024-03-15","acquisitionPrice":39.99}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.acquisitionDate").value("2024-03-15"))
                .andExpect(jsonPath("$.acquisitionPrice").value(39.99));
    }

    @Test
    void updateGame_returns400_whenAcquisitionPriceIsNegative() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(sampleGame()));

        mockMvc.perform(put("/api/v1/games/g1").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"The Witcher 3","platform":"PC","status":"COMPLETED","acquisitionPrice":-5}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateGame_returns400_whenAcquisitionPriceHasTooManyDecimals() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(sampleGame()));

        mockMvc.perform(put("/api/v1/games/g1").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"The Witcher 3","platform":"PC","status":"COMPLETED","acquisitionPrice":39.999}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGame_resolvesSteamAppId_whenObtainPlatinumTrue() throws Exception {
        when(steamCatalogueClient.searchGameByName("The Witcher 3")).thenReturn(570L);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/games").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"The Witcher 3","platform":"PC","status":"PLAYING","obtainPlatinum":true}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.steamAppId").value("570"));

        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getSteamAppId()).isEqualTo("570");
    }

    @Test
    void createGame_usesBodyAppIdWithoutCallingSteam() throws Exception {
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/games").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"The Witcher 3","platform":"PC","status":"PLAYING","steamAppId":"999"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.steamAppId").value("999"));

        verifyNoInteractions(steamCatalogueClient);
    }

    @Test
    void createGame_doesNotCallSteam_whenObtainPlatinumFalse() throws Exception {
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/v1/games").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"The Witcher 3","platform":"PC","status":"PLAYING"}
                                """))
                .andExpect(status().isCreated());

        verifyNoInteractions(steamCatalogueClient);
    }

    @Test
    void updateGame_returnsUpdatedGame() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(sampleGame()));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/games/g1").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nuevo título","platform":"PC","status":"COMPLETED","userRating":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nuevo título"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.userRating").value(5));
    }

    @Test
    void updateGame_resolvesSteamAppId_whenObtainPlatinumTrue() throws Exception {
        Game existing = sampleGame();
        existing.setId("g1");
        when(gameRepository.findById("g1")).thenReturn(Optional.of(existing));
        when(steamCatalogueClient.searchGameByName("Nuevo título")).thenReturn(570L);
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/games/g1").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nuevo título","platform":"PC","status":"PLAYING","obtainPlatinum":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steamAppId").value("570"));
    }

    @Test
    void deleteGame_returns204_whenExists() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(sampleGame()));

        mockMvc.perform(delete("/api/v1/games/g1").with(user("u1")))
                .andExpect(status().isNoContent());

        verify(gameRepository).deleteById("g1");
    }

    @Test
    void deleteGame_returns404_whenMissing() throws Exception {
        when(gameRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/games/nope").with(user("u1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_returnsResultsThroughFallbackChain() throws Exception {
        GameSearchResult result = new GameSearchResult(
                "3498", "The Witcher 3", "Aventura", "RPG", "PC",
                "CD Projekt Red", "CD Projekt Red", null, "http://img", "RAWG");
        when(rawgClient.search("witcher")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/games/search").param("name", "witcher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("The Witcher 3"))
                .andExpect(jsonPath("$.content[0].platform").value("PC"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void search_returns400_whenBlankQuery() throws Exception {
        mockMvc.perform(get("/api/v1/games/search").param("name", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAchievements_returnsAchievements_whenLinkedToSteam() throws Exception {
        when(gameAchievementsUseCase.getAchievements("g1", "7656")).thenReturn(new AchievementsSummary(
                List.of(new SteamAchievement("ACH_BORN", true, "El nacimiento", "Comienza la aventura.", "http://icon")),
                1, 1, 100.0));

        mockMvc.perform(get("/api/v1/games/g1/achievements").param("steamId", "7656"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.achievements[0].name").value("El nacimiento"))
                .andExpect(jsonPath("$.achievements[0].achieved").value(true))
                .andExpect(jsonPath("$.achievements[0].iconUrl").value("http://icon"))
                .andExpect(jsonPath("$.totalAchievements").value(1))
                .andExpect(jsonPath("$.totalAchieved").value(1))
                .andExpect(jsonPath("$.percentage").value(100.0));
    }

    @Test
    void getAchievements_returns404_whenGameNotFound() throws Exception {
        when(gameAchievementsUseCase.getAchievements("nope", "7656"))
                .thenThrow(new GameNotFoundException("Juego no encontrado con id: nope"));

        mockMvc.perform(get("/api/v1/games/nope/achievements").param("steamId", "7656"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAchievements_returns400_whenGameNotLinkedToSteam() throws Exception {
        when(gameAchievementsUseCase.getAchievements("g1", "7656"))
                .thenThrow(new IllegalArgumentException("El juego no está vinculado a Steam"));

        mockMvc.perform(get("/api/v1/games/g1/achievements").param("steamId", "7656"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAchievements_returns400_whenSteamIdMissing() throws Exception {
        when(gameAchievementsUseCase.getAchievements("g1", null))
                .thenThrow(new IllegalArgumentException("El parámetro steamId es obligatorio"));

        mockMvc.perform(get("/api/v1/games/g1/achievements"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAchievements_usesOwnerSteamId_whenSteamIdMissing() throws Exception {
        when(gameAchievementsUseCase.getAchievements("g1", null)).thenReturn(new AchievementsSummary(
                List.of(new SteamAchievement("ACH_BORN", true, "El nacimiento", "Comienza la aventura.", "http://icon")),
                1, 1, 100.0));

        mockMvc.perform(get("/api/v1/games/g1/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.achievements[0].name").value("El nacimiento"))
                .andExpect(jsonPath("$.totalAchieved").value(1));
    }

    // ------------------------------------------------------------------- contenido adicional

    @Test
    void getAdditions_returnsCatalogWithInCollectionAndAlreadyLinked() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(rawgBaseGame(GameStatus.PLAYING)));
        when(rawgClient.getAdditions("292030")).thenReturn(new GameAdditionsPage(
                List.of(addition("355880"), addition("378648")), 12));
        when(gameRepository.findByOwnerIdAndExternalIdIn("u1", List.of("355880", "378648")))
                .thenReturn(List.of(Game.builder().id("d1").ownerId("u1").externalId("355880")
                        .parentId("g1").build()));

        mockMvc.perform(get("/api/v1/games/g1/additions").with(user("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseGameId").value("g1"))
                .andExpect(jsonPath("$.baseExternalId").value("292030"))
                .andExpect(jsonPath("$.additions.length()").value(2))
                .andExpect(jsonPath("$.additions[0].externalId").value("355880"))
                .andExpect(jsonPath("$.additions[0].title").value("Adición 355880"))
                .andExpect(jsonPath("$.additions[0].inCollection").value(true))
                .andExpect(jsonPath("$.additions[0].alreadyLinked").value(true))
                .andExpect(jsonPath("$.additions[1].inCollection").value(false))
                .andExpect(jsonPath("$.additions[1].alreadyLinked").value(false))
                .andExpect(jsonPath("$.totalAvailable").value(12));
    }

    @Test
    void getAdditions_returns404_whenBaseMissing() throws Exception {
        when(gameRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/games/nope/additions"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAdditions_returns422_whenBaseDoesNotComeFromRawg() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(Game.builder()
                .id("g1").ownerId("u1").title("FreeToGame").externalId("1")
                .externalSource("FreeToGame").status(GameStatus.PLAYING).build()));

        mockMvc.perform(get("/api/v1/games/g1/additions"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void getAdditions_returns502_whenRawgFails() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(rawgBaseGame(GameStatus.PLAYING)));
        when(rawgClient.getAdditions("292030"))
                .thenThrow(new ExternalCatalogException("RAWG devolvió error 500"));

        mockMvc.perform(get("/api/v1/games/g1/additions"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void addAdditions_returns201_withCreatedLinkedAndSkipped() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(rawgBaseGame(GameStatus.PLAYING)));
        when(rawgClient.getAdditions("292030")).thenReturn(new GameAdditionsPage(
                List.of(addition("100"), addition("200"), addition("300")), 3));
        when(gameRepository.findByOwnerIdAndExternalIdIn("u1", List.of("100", "200", "300")))
                .thenReturn(List.of(
                        Game.builder().id("loose").ownerId("u1").externalId("200").build(),
                        Game.builder().id("linked").ownerId("u1").externalId("300").parentId("g1").build()));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId("dlc-" + saved.getExternalId());
            }
            return saved;
        });

        mockMvc.perform(post("/api/v1/games/g1/additions").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalIds":["100","200","300"],"status":"PLAYING"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created.length()").value(1))
                .andExpect(jsonPath("$.created[0].id").value("dlc-100"))
                .andExpect(jsonPath("$.created[0].parentId").value("g1"))
                .andExpect(jsonPath("$.created[0].parentTitle").value("The Witcher 3"))
                .andExpect(jsonPath("$.created[0].externalSource").value("RAWG"))
                .andExpect(jsonPath("$.linked.length()").value(1))
                .andExpect(jsonPath("$.linked[0].externalId").value("200"))
                .andExpect(jsonPath("$.linked[0].parentId").value("g1"))
                .andExpect(jsonPath("$.skipped.length()").value(1))
                .andExpect(jsonPath("$.skipped[0].externalId").value("300"))
                .andExpect(jsonPath("$.skipped[0].reason").value("ALREADY_LINKED"));
    }

    @Test
    void addAdditions_returns400_whenExternalIdsIsEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/games/g1/additions").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalIds":[],"status":"PLAYING"}
                                """))
                .andExpect(status().isBadRequest());

        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void addAdditions_returns400_whenAnExternalIdIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/games/g1/additions").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalIds":["355880","  "],"status":"PLAYING"}
                                """))
                .andExpect(status().isBadRequest());

        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void addAdditions_returns400_whenStatusIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/games/g1/additions").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalIds":["355880"]}
                                """))
                .andExpect(status().isBadRequest());

        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void addAdditions_returns404_whenBaseMissing() throws Exception {
        when(gameRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/games/nope/additions").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalIds":["355880"],"status":"PLAYING"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void addAdditions_returns403_whenBaseBelongsToAnotherUser() throws Exception {
        Game other = rawgBaseGame(GameStatus.PLAYING);
        other.setOwnerId("u2");
        when(gameRepository.findById("g1")).thenReturn(Optional.of(other));

        mockMvc.perform(post("/api/v1/games/g1/additions").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalIds":["355880"],"status":"PLAYING"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void addAdditions_returns422_whenBaseIsWishlist() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(rawgBaseGame(GameStatus.WISHLIST)));

        mockMvc.perform(post("/api/v1/games/g1/additions").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalIds":["355880"],"status":"PLAYING"}
                                """))
                .andExpect(status().isUnprocessableEntity());

        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void addAdditions_returns422_whenTheIdIsNotAnAdditionOfTheBase() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(rawgBaseGame(GameStatus.PLAYING)));
        when(rawgClient.getAdditions("292030")).thenReturn(new GameAdditionsPage(
                List.of(addition("355880")), 1));

        mockMvc.perform(post("/api/v1/games/g1/additions").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalIds":["777"],"status":"PLAYING"}
                                """))
                .andExpect(status().isUnprocessableEntity());

        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void list_filtersByTypeDlc() throws Exception {
        when(gameRepository.search(any(GameSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/games").with(user("u1")).param("type", "dlc"))
                .andExpect(status().isOk());

        ArgumentCaptor<GameSearchCriteria> captor = ArgumentCaptor.forClass(GameSearchCriteria.class);
        verify(gameRepository).search(captor.capture(), any(Pageable.class));
        assertThat(captor.getValue().type()).isEqualTo(GameTypeFilter.DLC);
    }

    private Game rawgBaseGame(GameStatus status) {
        return Game.builder()
                .id("g1").ownerId("u1").title("The Witcher 3")
                .externalId("292030").externalSource("RAWG").platform("PC").status(status)
                .build();
    }

    private GameSearchResult addition(String externalId) {
        return new GameSearchResult(externalId, "Adición " + externalId, "desc", "RPG", "PC",
                "CD Projekt Red", "CD Projekt Red", java.time.LocalDate.of(2016, 5, 31),
                "http://thumb/" + externalId, "RAWG");
    }

    @Test
    void cors_allowsFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/games")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}