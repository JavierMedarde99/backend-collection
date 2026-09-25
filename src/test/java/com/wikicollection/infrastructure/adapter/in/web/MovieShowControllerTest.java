package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
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

import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.MovieSearchResult;
import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.model.MovieStatus;
import com.wikicollection.domain.model.ProviderAccessType;
import com.wikicollection.domain.model.TmdbWatchProvider;
import com.wikicollection.domain.port.out.MovieShowRepository;
import com.wikicollection.infrastructure.adapter.out.tmdb.TmdbClient;

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
class MovieShowControllerTest {

    @MockitoBean
    private com.wikicollection.application.service.OwnerResolver ownerResolver;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieShowRepository movieShowRepository;

    @MockitoBean
    private TmdbClient catalogClient;

    @MockitoBean
    private com.wikicollection.domain.port.in.UserPreferencesUseCase preferencesUseCase;

    private MovieShow sampleShow() {
        return MovieShow.builder()
                .id("m1")
                .ownerId("u1")
                .externalId("550")
                .title("Fight Club")
                .mediaType(MovieMediaType.MOVIE)
                .status(MovieStatus.WATCHED)
                .externalSource("TMDB")
                .build();
    }

    @BeforeEach
    void stubOwnerResolver() {
        lenient().when(ownerResolver.resolveOwner(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> com.wikicollection.domain.model.UserOwned.builder()
                        .ownerId(invocation.getArgument(0)).ownerName("Javi").build());
    }

    @Test
    void listShows_returns400_whenNameTooLong() throws Exception {
        mockMvc.perform(get("/api/v1/movieshows").with(user("u1")).param("name", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listShows_returnsEmptyPage_whenNoShows() throws Exception {
        when(movieShowRepository.findByCriteria(any(MovieSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/movieshows").with(user("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listShows_returns400_whenStatusInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/movieshows").with(user("u1"))
                        .param("status", "NO_EXISTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Valor de parámetro inválido: status"));
    }

    @Test
    void listShows_filtersByNameStatusAndType() throws Exception {
        when(movieShowRepository.findByCriteria(any(MovieSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/movieshows").with(user("u1"))
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
    void listShows_filtersByGenre() throws Exception {
        when(movieShowRepository.findByCriteria(any(MovieSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/movieshows").with(user("u1")).param("genre", "drama"))
                .andExpect(status().isOk());

        ArgumentCaptor<MovieSearchCriteria> captor = ArgumentCaptor.forClass(MovieSearchCriteria.class);
        verify(movieShowRepository).findByCriteria(captor.capture(), any(Pageable.class));
        org.assertj.core.api.Assertions.assertThat(captor.getValue().genres()).containsExactly("drama");
    }

    @Test
    void getShow_returnsShow_whenExists() throws Exception {
        when(movieShowRepository.findById("m1")).thenReturn(Optional.of(sampleShow()));

        mockMvc.perform(get("/api/v1/movieshows/m1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("m1"))
                .andExpect(jsonPath("$.title").value("Fight Club"));
    }

    @Test
    void getShow_hidesPrivateFields_whenNotOwner() throws Exception {
        MovieShow show = sampleShow();
        show.setComment("privado");
        show.setUserOwned(new com.wikicollection.domain.model.UserOwned("u1", "Javi", "javi"));
        when(movieShowRepository.findById("m1")).thenReturn(Optional.of(show));
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.MOVIESHOWS))
                .thenReturn(List.of("u1"));
        mockMvc.perform(get("/api/v1/movieshows/m1").with(user("other")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.userOwned.ownerName").value("Javi"));
    }

    @Test
    void getShow_returns404_whenMissing() throws Exception {
        when(movieShowRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/movieshows/nope"))
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

        mockMvc.perform(post("/api/v1/movieshows").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"550","title":"Fight Club","mediaType":"MOVIE","status":"WATCHED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/v1/movieshows/m-new")))
                .andExpect(jsonPath("$.id").value("m-new"))
                .andExpect(jsonPath("$.title").value("Fight Club"));
    }

    @Test
    void createShow_returns400_whenBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/movieshows").with(user("u1"))
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

        mockMvc.perform(put("/api/v1/movieshows/m1").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"550","title":"Se7en","mediaType":"MOVIE","status":"WATCHED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Se7en"));
    }

    @Test
    void refreshProviders_returnsUpdatedShow() throws Exception {
        when(movieShowRepository.findById("m1")).thenReturn(Optional.of(sampleShow()));
        when(movieShowRepository.save(any(MovieShow.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(catalogClient.getWatchProviders(550L, MovieMediaType.MOVIE, "ES"))
                .thenReturn(java.util.Map.of(ProviderAccessType.FLATRATE,
                        java.util.List.of(new TmdbWatchProvider(10, "Netflix", "/netflix.jpg"))));

        mockMvc.perform(post("/api/v1/movieshows/m1/refresh-providers").with(user("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streamingProviders[0].providerName").value("Netflix"))
                .andExpect(jsonPath("$.watchCountry").value("ES"));
    }

    @Test
    void refreshProviders_returns404_whenMissing() throws Exception {
        when(movieShowRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/movieshows/nope/refresh-providers").with(user("u1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateShow_returns404_whenMissing() throws Exception {
        when(movieShowRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/movieshows/nope").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"550","title":"Se7en"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteShow_returns204_whenExists() throws Exception {
        when(movieShowRepository.findById("m1")).thenReturn(Optional.of(sampleShow()));

        mockMvc.perform(delete("/api/v1/movieshows/m1").with(user("u1")))
                .andExpect(status().isNoContent());

        verify(movieShowRepository).deleteById("m1");
    }

    @Test
    void deleteShow_returns404_whenMissing() throws Exception {
        when(movieShowRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/movieshows/nope").with(user("u1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void search_returnsResultsFromTmdb() throws Exception {
        MovieSearchResult result = new MovieSearchResult("550", "Fight Club", "Overview",
                LocalDate.of(1999, 10, 15), "http://poster", "http://backdrop",
                8.4, MovieMediaType.MOVIE, "TMDB");
        when(catalogClient.search("fight", null)).thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/movieshows/search").param("name", "fight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Fight Club"))
                .andExpect(jsonPath("$.content[0].mediaType").value("MOVIE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void search_filtersByMediaType() throws Exception {
        MovieSearchResult result = new MovieSearchResult("550", "Fight Club", "Overview",
                LocalDate.of(1999, 10, 15), "http://poster", "http://backdrop",
                8.4, MovieMediaType.MOVIE, "TMDB");
        when(catalogClient.search("fight", MovieMediaType.MOVIE)).thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/movieshows/search").param("name", "fight").param("mediaType", "movie"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Fight Club"))
                .andExpect(jsonPath("$.content[0].mediaType").value("MOVIE"));
    }

    @Test
    void search_returns400_whenBlankQuery() throws Exception {
        mockMvc.perform(get("/api/v1/movieshows/search").param("name", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cors_allowsFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/movieshows")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
