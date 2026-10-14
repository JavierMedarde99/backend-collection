package com.wikicollection.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.Funko;
import com.wikicollection.domain.model.FunkoCategory;
import com.wikicollection.domain.model.FunkoSearchCriteria;
import com.wikicollection.domain.model.FunkoSearchResult;
import com.wikicollection.domain.model.FunkoStatus;
import com.wikicollection.domain.model.UserOwned;
import com.wikicollection.domain.port.out.FunkoRepository;
import com.wikicollection.domain.port.out.UserRepository;
import com.wikicollection.infrastructure.adapter.out.funko.FunkoApiClient;
import com.wikicollection.infrastructure.config.CacheTestSupport;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientResponseException;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class FunkoControllerTest {

    @MockitoBean
    private com.wikicollection.application.service.OwnerResolver ownerResolver;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FunkoRepository funkoRepository;

    @MockitoBean
    private FunkoApiClient funkoApiClient;

    @MockitoBean
    private com.wikicollection.domain.port.in.UserPreferencesUseCase preferencesUseCase;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        CacheTestSupport.clearAll(cacheManager);
    }

    @BeforeEach
    void stubOwnerResolver() {
        lenient().when(ownerResolver.resolveOwner(any()))
                .thenAnswer(invocation -> UserOwned.builder()
                        .ownerId(invocation.getArgument(0)).ownerName("Javi").build());
    }

    private Funko sampleFunko() {
        return Funko.builder()
                .id("fk1")
                .ownerId("u1")
                .externalId("44")
                .name("Batman")
                .status(FunkoStatus.OWNED)
                .userOwned(UserOwned.builder().ownerId("u1").ownerName("Javi").username("javi").build())
                .build();
    }

    @Test
    void list_returnsEmptyPage() throws Exception {
        when(funkoRepository.search(any(FunkoSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/funkos").with(user("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void list_filtersByNameAndStatus_andResolvesOwner() throws Exception {
        when(funkoRepository.search(any(FunkoSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/funkos").with(user("u1"))
                        .param("name", "batman")
                        .param("status", "WISHLIST"))
                .andExpect(status().isOk());

        ArgumentCaptor<FunkoSearchCriteria> captor = ArgumentCaptor.forClass(FunkoSearchCriteria.class);
        verify(funkoRepository).search(captor.capture(), any(Pageable.class));
        assertThat(captor.getValue().name()).isEqualTo("batman");
        assertThat(captor.getValue().status()).isEqualTo(FunkoStatus.WISHLIST);
        assertThat(captor.getValue().ownerId()).isEqualTo("u1");
    }

    @Test
    void getById_returnsFunko() throws Exception {
        when(funkoRepository.findById("fk1")).thenReturn(Optional.of(sampleFunko()));

        mockMvc.perform(get("/api/v1/funkos/fk1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("fk1"))
                .andExpect(jsonPath("$.externalId").value("44"))
                .andExpect(jsonPath("$.status").value("OWNED"))
                .andExpect(jsonPath("$.userOwned.ownerName").value("Javi"));
    }

    @Test
    void getById_stripsPrivateNotes_forOtherUser() throws Exception {
        Funko funko = sampleFunko();
        funko.setNotes("privado");
        when(funkoRepository.findById("fk1")).thenReturn(Optional.of(funko));
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.FUNKOS))
                .thenReturn(List.of("u1"));

        mockMvc.perform(get("/api/v1/funkos/fk1").with(user("other")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.userOwned.ownerName").value("Javi"));
    }

    @Test
    void getById_returns404_whenMissing() throws Exception {
        when(funkoRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/funkos/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns201_withLocation() throws Exception {
        when(funkoRepository.save(any(Funko.class))).thenAnswer(invocation -> {
            Funko saved = invocation.getArgument(0);
            saved.setId("fk-new");
            return saved;
        });

        mockMvc.perform(post("/api/v1/funkos").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"44","name":"Batman","status":"OWNED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/v1/funkos/fk-new")))
                .andExpect(jsonPath("$.id").value("fk-new"))
                .andExpect(jsonPath("$.externalId").value("44"))
                .andExpect(jsonPath("$.userOwned.ownerName").value("Javi"));
    }

    @Test
    void create_returns400_whenNameMissing() throws Exception {
        mockMvc.perform(post("/api/v1/funkos").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"44","status":"OWNED"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns409_whenExternalIdExists() throws Exception {
        when(funkoRepository.findByExternalId("44"))
                .thenReturn(Optional.of(Funko.builder().id("otro").build()));

        mockMvc.perform(post("/api/v1/funkos").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"44","name":"Batman","status":"OWNED"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void update_returns200() throws Exception {
        when(funkoRepository.findById("fk1")).thenReturn(Optional.of(sampleFunko()));
        when(funkoRepository.save(any(Funko.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/funkos/fk1").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"44","name":"Nuevo","status":"WISHLIST","notes":"quimera","acquisitionDate":"2024-05-01","acquisitionPrice":24.99}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nuevo"))
                .andExpect(jsonPath("$.status").value("WISHLIST"))
                .andExpect(jsonPath("$.notes").value("quimera"))
                .andExpect(jsonPath("$.acquisitionDate").value("2024-05-01"))
                .andExpect(jsonPath("$.acquisitionPrice").value(24.99));
    }

    @Test
    void update_returns403_forOtherOwner() throws Exception {
        when(funkoRepository.findById("fk1"))
                .thenReturn(Optional.of(Funko.builder().id("fk1").ownerId("u2").build()));

        mockMvc.perform(put("/api/v1/funkos/fk1").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"44","name":"Nuevo","status":"OWNED"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_returns204() throws Exception {
        when(funkoRepository.findById("fk1")).thenReturn(Optional.of(sampleFunko()));

        mockMvc.perform(delete("/api/v1/funkos/fk1").with(user("u1")))
                .andExpect(status().isNoContent());

        verify(funkoRepository).deleteById("fk1");
    }

    @Test
    void search_returnsResults() throws Exception {
        FunkoSearchResult result = new FunkoSearchResult(
                "44", "Batman", "DC Comics", "01", "Comics", null, true, 2012,
                null, null, 30, "http://img", FunkoSearchResult.EXTERNAL_SOURCE);
        when(funkoApiClient.searchByName("batman")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/funkos/search").param("name", "batman"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].externalId").value("44"))
                .andExpect(jsonPath("$.content[0].vaulted").value(true))
                .andExpect(jsonPath("$.content[0].rarityScore").value(30))
                .andExpect(jsonPath("$.content[0].externalSource").value("hobbyquanthq"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void search_returns400_whenNameMissing() throws Exception {
        mockMvc.perform(get("/api/v1/funkos/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_returns400_whenNameTooShort() throws Exception {
        mockMvc.perform(get("/api/v1/funkos/search").param("name", "b"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_propagatesExternalError_as502() throws Exception {
        when(funkoApiClient.searchByName(eq("batman"))).thenThrow(
                new RestClientResponseException(
                        "error", 500, "Internal Server Error", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/v1/funkos/search").param("name", "batman"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void categories_returnsList() throws Exception {
        when(funkoApiClient.listCategories())
                .thenReturn(List.of(new FunkoCategory("Movies", 1238)));

        mockMvc.perform(get("/api/v1/funkos/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Movies"))
                .andExpect(jsonPath("$[0].count").value(1238));
    }

    @Test
    void cors_preflightAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/funkos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
