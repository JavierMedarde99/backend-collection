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

import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameSearchCriteria;
import com.wikicollection.domain.model.BoardGameSearchResult;
import com.wikicollection.domain.model.BoardGameStatus;
import com.wikicollection.domain.port.out.BoardGameRepository;
import com.wikicollection.infrastructure.adapter.out.bgg.xml.BggXmlClient;

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

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class BoardGameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardGameRepository boardGameRepository;

    @MockitoBean
    private BggXmlClient bggXmlClient;

    private BoardGame sampleGame() {
        return BoardGame.builder()
                .id("bg1")
                .title("Catan")
                .status(BoardGameStatus.OWNED)
                .build();
    }

    @Test
    void listGames_returnsEmptyPage_whenNoGames() throws Exception {
        when(boardGameRepository.search(any(BoardGameSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/boardgames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listGames_filtersByNameAndStatus() throws Exception {
        when(boardGameRepository.search(any(BoardGameSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/boardgames")
                        .param("name", "catan")
                        .param("status", "WISHLIST"))
                .andExpect(status().isOk());

        ArgumentCaptor<BoardGameSearchCriteria> captor = ArgumentCaptor.forClass(BoardGameSearchCriteria.class);
        verify(boardGameRepository).search(captor.capture(), any(Pageable.class));
        BoardGameSearchCriteria criteria = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(criteria.name()).isEqualTo("catan");
        org.assertj.core.api.Assertions.assertThat(criteria.status()).isEqualTo(BoardGameStatus.WISHLIST);
    }

    @Test
    void listGames_rejectsLegacyStatus() throws Exception {
        mockMvc.perform(get("/api/v1/boardgames")
                        .param("status", "PREVIOUSLY_OWNED"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBoardGame_rejectsLegacyStatus() throws Exception {
        mockMvc.perform(post("/api/v1/boardgames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Catan","status":"FOR_TRADE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBoardGame_returnsGame_whenExists() throws Exception {
        when(boardGameRepository.findById("bg1")).thenReturn(Optional.of(sampleGame()));

        mockMvc.perform(get("/api/v1/boardgames/bg1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("bg1"))
                .andExpect(jsonPath("$.title").value("Catan"))
                .andExpect(jsonPath("$.status").value("OWNED"));
    }

    @Test
    void getBoardGame_returns404_whenMissing() throws Exception {
        when(boardGameRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/boardgames/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBoardGame_returns201_withLocation() throws Exception {
        when(boardGameRepository.save(any(BoardGame.class))).thenAnswer(invocation -> {
            BoardGame saved = invocation.getArgument(0);
            saved.setId("bg-new");
            return saved;
        });

        mockMvc.perform(post("/api/v1/boardgames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Catan","status":"OWNED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/v1/boardgames/bg-new")))
                .andExpect(jsonPath("$.id").value("bg-new"))
                .andExpect(jsonPath("$.title").value("Catan"));
    }

    @Test
    void createBoardGame_returns400_whenBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/boardgames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","status":"OWNED"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBoardGame_returns400_whenMissingStatus() throws Exception {
        mockMvc.perform(post("/api/v1/boardgames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Catan"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBoardGame_returns400_whenMinPlayersOutOfRange() throws Exception {
        mockMvc.perform(post("/api/v1/boardgames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Catan","status":"OWNED","minPlayers":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateBoardGame_returnsUpdatedGame() throws Exception {
        when(boardGameRepository.findById("bg1")).thenReturn(Optional.of(sampleGame()));
        when(boardGameRepository.save(any(BoardGame.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/boardgames/bg1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Nuevo título","status":"WISHLIST","notes":"Quiero jugarlo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nuevo título"))
                .andExpect(jsonPath("$.status").value("WISHLIST"))
                .andExpect(jsonPath("$.notes").value("Quiero jugarlo"));
    }

    @Test
    void updateBoardGame_returns404_whenMissing() throws Exception {
        when(boardGameRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/boardgames/nope")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Catan","status":"OWNED"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBoardGame_returns204_whenExists() throws Exception {
        when(boardGameRepository.findById("bg1")).thenReturn(Optional.of(sampleGame()));

        mockMvc.perform(delete("/api/v1/boardgames/bg1"))
                .andExpect(status().isNoContent());

        verify(boardGameRepository).deleteById("bg1");
    }

    @Test
    void deleteBoardGame_returns404_whenMissing() throws Exception {
        when(boardGameRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/boardgames/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_returnsResultsFromBgg() throws Exception {
        BoardGameSearchResult result = new BoardGameSearchResult(
                "31260", "Catan", "Colonización", 2007, 3, 4, 60, 120,
                "Kosmos", List.of("Klaus Teuber"), List.of("Negociación"), List.of("Trading"),
                "http://img", "http://thumb", new java.math.BigDecimal("8.3"), "BGG");
        when(bggXmlClient.search("catan")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/boardgames/search").param("name", "catan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("catan"))
                .andExpect(jsonPath("$.results[0].title").value("Catan"))
                .andExpect(jsonPath("$.results[0].publisher").value("Kosmos"));
    }

    @Test
    void search_returns400_whenBlankQuery() throws Exception {
        mockMvc.perform(get("/api/v1/boardgames/search").param("name", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cors_allowsFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/boardgames")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}