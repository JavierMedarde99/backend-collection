package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.model.GameSearchResult;
import com.wikicollection.domain.model.GameStatus;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.infrastructure.adapter.out.freetogame.FreeToGameClient;
import com.wikicollection.infrastructure.adapter.out.rawg.RAWGClient;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
@AutoConfigureMockMvc
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameRepository gameRepository;

    @MockitoBean
    private RAWGClient rawgClient;

    @MockitoBean
    private FreeToGameClient freeToGameClient;

    private Game sampleGame() {
        return Game.builder()
                .id("g1")
                .title("The Witcher 3")
                .platform(GamePlatform.PC)
                .status(GameStatus.PLAYING)
                .build();
    }

    @Test
    void listGames_returnsEmptyPage_whenNoGames() throws Exception {
        when(gameRepository.search(any(GameSearchCriteria.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listGames_filtersByNamePlatformAndStatus() throws Exception {
        when(gameRepository.search(any(GameSearchCriteria.class), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/api/games")
                        .param("name", "witc")
                        .param("platform", "PC")
                        .param("status", "PLAYING"))
                .andExpect(status().isOk());

        ArgumentCaptor<GameSearchCriteria> captor = ArgumentCaptor.forClass(GameSearchCriteria.class);
        verify(gameRepository).search(captor.capture(), any(Pageable.class));
        GameSearchCriteria criteria = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(criteria.name()).isEqualTo("witc");
        org.assertj.core.api.Assertions.assertThat(criteria.platform()).isEqualTo(GamePlatform.PC);
        org.assertj.core.api.Assertions.assertThat(criteria.status()).isEqualTo(GameStatus.PLAYING);
    }

    @Test
    void getGame_returnsGame_whenExists() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(sampleGame()));

        mockMvc.perform(get("/api/games/g1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("g1"))
                .andExpect(jsonPath("$.title").value("The Witcher 3"))
                .andExpect(jsonPath("$.status").value("PLAYING"));
    }

    @Test
    void getGame_returns404_whenMissing() throws Exception {
        when(gameRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/games/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createGame_returns201_withLocation() throws Exception {
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> {
            Game saved = invocation.getArgument(0);
            saved.setId("g-new");
            return saved;
        });

        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"The Witcher 3","platform":"PC","status":"PLAYING"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/games/g-new")))
                .andExpect(jsonPath("$.id").value("g-new"))
                .andExpect(jsonPath("$.title").value("The Witcher 3"));
    }

    @Test
    void createGame_returns400_whenInvalid() throws Exception {
        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","platform":"PC","status":"PLAYING"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGame_returns400_whenUserRatingOutOfRange() throws Exception {
        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"The Witcher 3","platform":"PC","status":"PLAYING","userRating":9}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateGame_returnsUpdatedGame() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(sampleGame()));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/games/g1")
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
    void deleteGame_returns204_whenExists() throws Exception {
        when(gameRepository.findById("g1")).thenReturn(Optional.of(sampleGame()));

        mockMvc.perform(delete("/api/games/g1"))
                .andExpect(status().isNoContent());

        verify(gameRepository).deleteById("g1");
    }

    @Test
    void deleteGame_returns404_whenMissing() throws Exception {
        when(gameRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/games/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_returnsResultsThroughFallbackChain() throws Exception {
        GameSearchResult result = new GameSearchResult(
                "3498", "The Witcher 3", "Aventura", "RPG", GamePlatform.PC,
                "CD Projekt Red", "CD Projekt Red", null, "http://img", "RAWG");
        when(rawgClient.search("witcher")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/games/search").param("name", "witcher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("The Witcher 3"))
                .andExpect(jsonPath("$[0].platform").value("PC"));
    }

    @Test
    void search_returns400_whenBlankQuery() throws Exception {
        mockMvc.perform(get("/api/games/search").param("name", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cors_allowsFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/games")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}