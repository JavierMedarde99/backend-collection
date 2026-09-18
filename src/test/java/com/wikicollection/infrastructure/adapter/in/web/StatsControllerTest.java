package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.wikicollection.domain.port.in.StatsUseCase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsUseCase statsUseCase;

    @Test
    void globalStats_isPublic() throws Exception {
        when(statsUseCase.getGlobalCounts()).thenReturn(Map.of(
                "books", 10L, "games", 5L, "boardgames", 3L,
                "magic", 7L, "decks", 2L, "movieshows", 4L));

        mockMvc.perform(get("/api/v1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collections.books").value(10))
                .andExpect(jsonPath("$.collections.games").value(5))
                .andExpect(jsonPath("$.collections.movieshows").value(4));
    }
}
