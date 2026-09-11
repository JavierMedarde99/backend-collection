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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.MovieSearchResult;
import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.model.MovieStatus;
import com.wikicollection.domain.port.out.ExternalMovieCatalogClient;
import com.wikicollection.domain.port.out.MovieShowRepository;

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
class MovieShowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieShowRepository movieShowRepository;

    @MockitoBean
    private ExternalMovieCatalogClient catalogClient;

    private MovieShow sampleShow() {
        return MovieShow.builder()
                .id("m1")
                .externalId("550")
                .title("Fight Club")
                .mediaType(MovieMediaType.MOVIE)
                .status(MovieStatus.WATCHED)
                .externalSource("TMDB")
                .build();
    }

    @Test
    void listShows_returnsEmptyPage_whenNoShows() throws Exception {
        when(movieShowRepository.findByCriteria(any(MovieSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listShows_filtersByNameStatusAndType() throws Exception {
        when(movieShowRepository.findByCriteria(any(MovieSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/movies")
                        .param("name", "fight")
                        .param("status", "watched")
                        .param("mediaType", "movie"))
                .andExpect(status().isOk());

        ArgumentCaptor<MovieSearchCriteria> captor = ArgumentCaptor.forClass(MovieSearchCriteria.class);
        verify(movieShowRepository).findByCriteria(captor.capture(), any(Pageable.class));
        MovieSearchCriteria criteria = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(criteria.name()).isEqualTo("fight");
        org.assertj.core.api.Assertions.assertThat(criteria.status()).isEqualTo(MovieStatus.WATCHED);
        org.assertj.core.api.Assertions.assertThat(criteria.mediaType()).isEqualTo(MovieMediaType.MOVIE);
    }

    @Test
    void getShow_returnsShow_whenExists() throws Exception {
        when(movieShowRepository.findById("m1")).thenReturn(Optional.of(sampleShow()));

        mockMvc.perform(get("/api/movies/m1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("m1"))
                .andExpect(jsonPath("$.title").value("Fight Club"));
    }

    @Test
    void getShow_returns404_whenMissing() throws Exception {
        when(movieShowRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/movies/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createShow_returns201_withLocation() throws Exception {
        when(movieShowRepository.findByExternalId("550")).thenReturn(Optional.empty());
        when(movieShowRepository.save(any(MovieShow.class))).thenAnswer(invocation -> {
            MovieShow saved = invocation.getArgument(0);
            saved.setId("m-new");
            return saved;
        });

        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"550","title":"Fight Club","mediaType":"MOVIE","status":"WATCHED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/movies/m-new")))
                .andExpect(jsonPath("$.id").value("m-new"))
                .andExpect(jsonPath("$.title").value("Fight Club"));
    }

    @Test
    void createShow_returns400_whenBlankTitle() throws Exception {
        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"550","title":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateShow_returnsUpdatedShow() throws Exception {
        when(movieShowRepository.findById("m1")).thenReturn(Optional.of(sampleShow()));
        when(movieShowRepository.findByExternalId("550")).thenReturn(Optional.of(sampleShow()));
        when(movieShowRepository.save(any(MovieShow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/movies/m1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"550","title":"Se7en","mediaType":"MOVIE","status":"WATCHED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Se7en"));
    }

    @Test
    void updateShow_returns404_whenMissing() throws Exception {
        when(movieShowRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/movies/nope")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"550","title":"Se7en"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteShow_returns204_whenExists() throws Exception {
        when(movieShowRepository.findById("m1")).thenReturn(Optional.of(sampleShow()));

        mockMvc.perform(delete("/api/movies/m1"))
                .andExpect(status().isNoContent());

        verify(movieShowRepository).deleteById("m1");
    }

    @Test
    void deleteShow_returns404_whenMissing() throws Exception {
        when(movieShowRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/movies/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_returnsResultsFromTmdb() throws Exception {
        MovieSearchResult result = new MovieSearchResult("550", "Fight Club", "Overview",
                LocalDate.of(1999, 10, 15), "http://poster", "http://backdrop",
                8.4, MovieMediaType.MOVIE, "TMDB");
        when(catalogClient.search("fight", null)).thenReturn(List.of(result));

        mockMvc.perform(get("/api/movies/search").param("name", "fight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Fight Club"))
                .andExpect(jsonPath("$[0].mediaType").value("MOVIE"));
    }

    @Test
    void search_filtersByMediaType() throws Exception {
        MovieSearchResult result = new MovieSearchResult("550", "Fight Club", "Overview",
                LocalDate.of(1999, 10, 15), "http://poster", "http://backdrop",
                8.4, MovieMediaType.MOVIE, "TMDB");
        when(catalogClient.search("fight", MovieMediaType.MOVIE)).thenReturn(List.of(result));

        mockMvc.perform(get("/api/movies/search").param("name", "fight").param("mediaType", "movie"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Fight Club"))
                .andExpect(jsonPath("$[0].mediaType").value("MOVIE"));
    }

    @Test
    void search_returns400_whenBlankQuery() throws Exception {
        mockMvc.perform(get("/api/movies/search").param("name", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cors_allowsFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/movies")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
