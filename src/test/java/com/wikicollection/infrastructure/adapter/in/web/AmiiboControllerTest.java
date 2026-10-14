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

import com.wikicollection.domain.model.Amiibo;
import com.wikicollection.domain.model.AmiiboSearchCriteria;
import com.wikicollection.domain.model.AmiiboSearchResult;
import com.wikicollection.domain.model.AmiiboStatus;
import com.wikicollection.domain.model.AmiiboType;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.UserOwned;
import com.wikicollection.domain.port.out.AmiiboRepository;
import com.wikicollection.domain.port.out.UserRepository;
import com.wikicollection.infrastructure.adapter.out.amiibo.AmiiboApiClient;
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
class AmiiboControllerTest {

    @MockitoBean
    private com.wikicollection.application.service.OwnerResolver ownerResolver;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AmiiboRepository amiiboRepository;

    @MockitoBean
    private AmiiboApiClient amiiboApiClient;

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

    private Amiibo sampleAmiibo() {
        return Amiibo.builder()
                .id("am1")
                .ownerId("u1")
                .externalId("09d0030102bb0e02")
                .name("Metal Mario")
                .status(AmiiboStatus.OWNED)
                .userOwned(UserOwned.builder().ownerId("u1").ownerName("Javi").username("javi").build())
                .build();
    }

    @Test
    void list_returnsEmptyPage() throws Exception {
        when(amiiboRepository.search(any(AmiiboSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/amiibos").with(user("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void list_filtersByNameAndStatus_andResolvesOwner() throws Exception {
        when(amiiboRepository.search(any(AmiiboSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/amiibos").with(user("u1"))
                        .param("name", "mario")
                        .param("status", "WISHLIST"))
                .andExpect(status().isOk());

        ArgumentCaptor<AmiiboSearchCriteria> captor = ArgumentCaptor.forClass(AmiiboSearchCriteria.class);
        verify(amiiboRepository).search(captor.capture(), any(Pageable.class));
        assertThat(captor.getValue().name()).isEqualTo("mario");
        assertThat(captor.getValue().status()).isEqualTo(AmiiboStatus.WISHLIST);
        assertThat(captor.getValue().ownerId()).isEqualTo("u1");
    }

    @Test
    void getById_returnsAmiibo() throws Exception {
        when(amiiboRepository.findById("am1")).thenReturn(Optional.of(sampleAmiibo()));

        mockMvc.perform(get("/api/v1/amiibos/am1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("am1"))
                .andExpect(jsonPath("$.externalId").value("09d0030102bb0e02"))
                .andExpect(jsonPath("$.status").value("OWNED"))
                .andExpect(jsonPath("$.userOwned.ownerName").value("Javi"));
    }

    @Test
    void getById_stripsPrivateNotes_forOtherUser() throws Exception {
        Amiibo amiibo = sampleAmiibo();
        amiibo.setNotes("privado");
        when(amiiboRepository.findById("am1")).thenReturn(Optional.of(amiibo));
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.AMIIBOS))
                .thenReturn(List.of("u1"));

        mockMvc.perform(get("/api/v1/amiibos/am1").with(user("other")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.userOwned.ownerName").value("Javi"));
    }

    @Test
    void getById_returns404_whenMissing() throws Exception {
        when(amiiboRepository.findById("nope")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/amiibos/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns201_withLocation() throws Exception {
        when(amiiboRepository.save(any(Amiibo.class))).thenAnswer(invocation -> {
            Amiibo saved = invocation.getArgument(0);
            saved.setId("am-new");
            return saved;
        });

        mockMvc.perform(post("/api/v1/amiibos").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"09d0030102bb0e02","name":"Metal Mario","status":"OWNED"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/v1/amiibos/am-new")))
                .andExpect(jsonPath("$.id").value("am-new"))
                .andExpect(jsonPath("$.externalId").value("09d0030102bb0e02"))
                .andExpect(jsonPath("$.userOwned.ownerName").value("Javi"));
    }

    @Test
    void create_returns400_whenNameMissing() throws Exception {
        mockMvc.perform(post("/api/v1/amiibos").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"09d0030102bb0e02","status":"OWNED"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returns409_whenExternalIdExists() throws Exception {
        when(amiiboRepository.findByExternalId("09d0030102bb0e02"))
                .thenReturn(Optional.of(Amiibo.builder().id("otro").build()));

        mockMvc.perform(post("/api/v1/amiibos").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"09d0030102bb0e02","name":"Metal Mario","status":"OWNED"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void update_returns200() throws Exception {
        when(amiiboRepository.findById("am1")).thenReturn(Optional.of(sampleAmiibo()));
        when(amiiboRepository.save(any(Amiibo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/v1/amiibos/am1").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"09d0030102bb0e02","name":"Nuevo","status":"WISHLIST","notes":"quimera","acquisitionDate":"2024-05-01","acquisitionPrice":14.5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nuevo"))
                .andExpect(jsonPath("$.status").value("WISHLIST"))
                .andExpect(jsonPath("$.notes").value("quimera"))
                .andExpect(jsonPath("$.acquisitionDate").value("2024-05-01"))
                .andExpect(jsonPath("$.acquisitionPrice").value(14.5));
    }

    @Test
    void update_returns403_forOtherOwner() throws Exception {
        when(amiiboRepository.findById("am1"))
                .thenReturn(Optional.of(Amiibo.builder().id("am1").ownerId("u2").build()));

        mockMvc.perform(put("/api/v1/amiibos/am1").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"09d0030102bb0e02","name":"Nuevo","status":"OWNED"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_returns204() throws Exception {
        when(amiiboRepository.findById("am1")).thenReturn(Optional.of(sampleAmiibo()));

        mockMvc.perform(delete("/api/v1/amiibos/am1").with(user("u1")))
                .andExpect(status().isNoContent());

        verify(amiiboRepository).deleteById("am1");
    }

    @Test
    void search_returnsResults() throws Exception {
        AmiiboSearchResult result = new AmiiboSearchResult(
                "09d0030102bb0e02", "Metal Mario", "Metal Mario",
                "Mario Sports Superstars", "Mario Sports Superstars", AmiiboType.FIGURE,
                null, "http://img", List.of("Mario Sports Superstars"), AmiiboSearchResult.EXTERNAL_SOURCE);
        when(amiiboApiClient.searchByName("mario")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/v1/amiibos/search").param("name", "mario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].externalId").value("09d0030102bb0e02"))
                .andExpect(jsonPath("$.content[0].amiiboType").value("FIGURE"))
                .andExpect(jsonPath("$.content[0].externalSource").value("amiiboapi"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void search_returns400_whenNameMissing() throws Exception {
        mockMvc.perform(get("/api/v1/amiibos/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_propagatesExternalError_as502() throws Exception {
        when(amiiboApiClient.searchByName(eq("mario"))).thenThrow(
                new RestClientResponseException(
                        "error", 500, "Internal Server Error", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/v1/amiibos/search").param("name", "mario"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void cors_preflightAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/amiibos")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
