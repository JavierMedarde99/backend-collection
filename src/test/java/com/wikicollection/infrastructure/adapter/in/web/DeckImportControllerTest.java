package com.wikicollection.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.wikicollection.application.exception.DeckImportNotFoundException;
import com.wikicollection.application.exception.DeckNotFoundException;
import com.wikicollection.application.exception.ForbiddenException;
import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckImportJob;
import com.wikicollection.domain.model.DeckImportMode;
import com.wikicollection.domain.model.DeckImportPhase;
import com.wikicollection.domain.model.DeckImportProgress;
import com.wikicollection.domain.model.DeckImportStatus;
import com.wikicollection.domain.model.DeckStatus;
import com.wikicollection.domain.model.DeckStatusReport;
import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.domain.model.UnresolvedCardEntry;
import com.wikicollection.domain.model.UnresolvedReason;
import com.wikicollection.domain.port.in.DeckImportUseCase;
import com.wikicollection.domain.port.in.DeckUseCase;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.domain.port.out.MagicCardRepository;
import com.wikicollection.infrastructure.adapter.out.scryfall.ScryfallClient;
import com.wikicollection.infrastructure.config.CacheTestSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Los tres endpoints de importación de mazos (#353).
 *
 * <p>Lo que se comprueba aquí, más allá del código de estado, es el contrato con el frontend:
 * el 202 lleva {@code Location} y una {@code statusUrl} que el cliente puede pollsar, el GET
 * devuelve el mazo solo cuando el job ha terminado, y las cartas irresolubles llegan con sus
 * candidatos para que el usuario pueda elegir. Un {@code deck} presente mientras el job sigue
 * en curso haría que el frontend pintara un mazo a medias.
 */
@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false",
        "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class DeckImportControllerTest {

    @MockitoBean
    private com.wikicollection.application.service.OwnerResolver ownerResolver;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeckRepository deckRepository;

    @MockitoBean
    private MagicCardRepository magicCardRepository;

    @MockitoBean
    private ScryfallClient scryfallClient;

    @MockitoBean
    private DeckImportUseCase deckImportUseCase;

    @MockitoBean
    private DeckUseCase deckUseCase;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        CacheTestSupport.clearAll(cacheManager);
    }

    // ------------------------------------------------------------- POST /imports

    @Test
    void importDeck_withFile_returns202WithLocation() throws Exception {
        when(deckImportUseCase.startImport(eq("deck-1"), any(), eq(DeckImportFormat.TXT),
                eq(DeckImportMode.REPLACE), eq("user-1")))
                .thenReturn(DeckImportJob.pending("job-1", "deck-1", "user-1",
                        DeckImportFormat.TXT, DeckImportMode.REPLACE));

        mockMvc.perform(multipart("/api/v1/decks/deck-1/imports")
                        .file(new MockMultipartFile("file", "deck.txt", "text/plain",
                                "1 Sol Ring".getBytes(StandardCharsets.UTF_8)))
                        .param("format", "TXT")
                        .param("mode", "REPLACE")
                        .with(user("user-1")))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", containsString("/api/v1/decks/deck-1/imports/job-1")))
                .andExpect(jsonPath("$.jobId").value("job-1"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.statusUrl").value("/api/v1/decks/deck-1/imports/job-1"));
    }

    @Test
    void importDeck_defaultsToReplace() throws Exception {
        when(deckImportUseCase.startImport(eq("deck-1"), any(), any(), eq(DeckImportMode.REPLACE), any()))
                .thenReturn(DeckImportJob.pending("job-1", "deck-1", "user-1",
                        DeckImportFormat.TXT, DeckImportMode.REPLACE));

        mockMvc.perform(multipart("/api/v1/decks/deck-1/imports")
                        .file(new MockMultipartFile("file", "deck.txt", "text/plain", "1 Sol Ring".getBytes()))
                        .with(user("user-1")))
                .andExpect(status().isAccepted());

        verify(deckImportUseCase).startImport(eq("deck-1"), any(), any(), eq(DeckImportMode.REPLACE),
                eq("user-1"));
    }

    @Test
    void importDeck_withMergeMode_passesMergeThrough() throws Exception {
        when(deckImportUseCase.startImport(eq("deck-1"), any(), any(), eq(DeckImportMode.MERGE), any()))
                .thenReturn(DeckImportJob.pending("job-1", "deck-1", "user-1",
                        DeckImportFormat.TXT, DeckImportMode.MERGE));

        mockMvc.perform(multipart("/api/v1/decks/deck-1/imports")
                        .file(new MockMultipartFile("file", "deck.txt", "text/plain", "1 Sol Ring".getBytes()))
                        .param("mode", "merge")
                        .with(user("user-1")))
                .andExpect(status().isAccepted());

        verify(deckImportUseCase).startImport(eq("deck-1"), any(), any(), eq(DeckImportMode.MERGE), any());
    }

    @Test
    void importDeck_unknownMode_returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/decks/deck-1/imports")
                        .file(new MockMultipartFile("file", "deck.txt", "text/plain", "1 Sol Ring".getBytes()))
                        .param("mode", "sideways")
                        .with(user("user-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void importDeck_unknownFormat_returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/decks/deck-1/imports")
                        .file(new MockMultipartFile("file", "deck.bin", "application/octet-stream", "{}".getBytes()))
                        .param("format", "yaml")
                        .with(user("user-1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importDeck_emptyFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/decks/deck-1/imports")
                        .file(new MockMultipartFile("file", "deck.txt", "text/plain", new byte[0]))
                        .with(user("user-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void importDeck_noFile_returns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/decks/deck-1/imports").with(user("user-1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importDeck_unknownDeck_returns404() throws Exception {
        when(deckImportUseCase.startImport(any(), any(), any(), any(), any()))
                .thenThrow(new DeckNotFoundException("No existe"));

        mockMvc.perform(multipart("/api/v1/decks/deck-1/imports")
                        .file(new MockMultipartFile("file", "deck.txt", "text/plain", "1 Sol Ring".getBytes()))
                        .with(user("user-1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void importDeck_foreignDeck_returns403() throws Exception {
        when(deckImportUseCase.startImport(any(), any(), any(), any(), any()))
                .thenThrow(new ForbiddenException("no tuyo"));

        mockMvc.perform(multipart("/api/v1/decks/deck-1/imports")
                        .file(new MockMultipartFile("file", "deck.txt", "text/plain", "1 Sol Ring".getBytes()))
                        .with(user("user-1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void importDeck_declaredFormatMismatch_returns400() throws Exception {
        when(deckImportUseCase.startImport(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("El archivo se declara como JSON"));

        mockMvc.perform(multipart("/api/v1/decks/deck-1/imports")
                        .file(new MockMultipartFile("file", "deck.txt", "text/plain", "1 Sol Ring".getBytes()))
                        .with(user("user-1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importDeck_saturatedExecutor_returns429() throws Exception {
        when(deckImportUseCase.startImport(any(), any(), any(), any(), any()))
                .thenThrow(new org.springframework.core.task.TaskRejectedException("executor lleno"));

        mockMvc.perform(multipart("/api/v1/decks/deck-1/imports")
                        .file(new MockMultipartFile("file", "deck.txt", "text/plain", "1 Sol Ring".getBytes()))
                        .with(user("user-1")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void importDeck_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(multipart("/api/v1/decks/deck-1/imports")
                        .file(new MockMultipartFile("file", "deck.txt", "text/plain", "1 Sol Ring".getBytes())))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------------------------- POST /imports/text

    @Test
    void importDeckText_withPlainText_returns202() throws Exception {
        when(deckImportUseCase.startImport(eq("deck-1"), eq("1 Sol Ring"), eq(DeckImportFormat.TXT),
                eq(DeckImportMode.REPLACE), eq("user-1")))
                .thenReturn(DeckImportJob.pending("job-1", "deck-1", "user-1",
                        DeckImportFormat.TXT, DeckImportMode.REPLACE));

        mockMvc.perform(post("/api/v1/decks/deck-1/imports/text")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("1 Sol Ring")
                        .param("format", "TXT")
                        .with(user("user-1")))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", containsString("/api/v1/decks/deck-1/imports/job-1")))
                .andExpect(jsonPath("$.jobId").value("job-1"));
    }

    @Test
    void importDeckText_emptyContent_returns400() throws Exception {
        when(deckImportUseCase.startImport(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("El archivo está vacío"));

        mockMvc.perform(post("/api/v1/decks/deck-1/imports/text")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("   ")
                        .with(user("user-1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importDeckText_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/decks/deck-1/imports/text")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("1 Sol Ring"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------- GET /imports/{id}

    @Test
    void getImport_completed_returnsDeckAndReport() throws Exception {
        MagicCardSearchResult candidate = new MagicCardSearchResult("c1", "Plains", null, "Basic Land",
                "common", "neo", "Neon Dynasty", "https://img", "0.20", List.of(), List.of(), null);
        DeckImportJob job = DeckImportJob.pending("job-1", "deck-1", "user-1",
                DeckImportFormat.TXT, DeckImportMode.REPLACE)
                .progress(DeckImportStatus.RUNNING, DeckImportPhase.RESOLVING,
                        new DeckImportProgress(3, 3, 2, 1),
                        List.of(new UnresolvedCardEntry(7, "Llanuras", 4, "Llanuras",
                                UnresolvedReason.NOT_FOUND, List.of(candidate))))
                .completed(new DeckStatusReport(DeckStatus.DRAFT, List.of("El mazo no llega a 99 cartas")),
                        "Atraxa, Grand Unifier", List.of("G", "W"));
        when(deckImportUseCase.findJob("deck-1", "job-1", "user-1")).thenReturn(job);
        when(deckUseCase.findById("deck-1"))
                .thenReturn(Deck.builder().id("deck-1").name("Mi Commander").commander("Atraxa").build());

        mockMvc.perform(get("/api/v1/decks/deck-1/imports/job-1").with(user("user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.phase").value("DONE"))
                .andExpect(jsonPath("$.deck.name").value("Mi Commander"))
                .andExpect(jsonPath("$.commander").value("Atraxa, Grand Unifier"))
                .andExpect(jsonPath("$.commanderColors[0]").value("G"))
                .andExpect(jsonPath("$.validation.status").value("DRAFT"))
                .andExpect(jsonPath("$.validation.reasons[0]").value("El mazo no llega a 99 cartas"))
                .andExpect(jsonPath("$.unresolved[0].reason").value("NOT_FOUND"))
                .andExpect(jsonPath("$.unresolved[0].line").value(7))
                .andExpect(jsonPath("$.unresolved[0].quantity").value(4))
                .andExpect(jsonPath("$.unresolved[0].candidates[0].name").value("Plains"))
                .andExpect(jsonPath("$.progress.total").value(3))
                .andExpect(jsonPath("$.progress.processed").value(3))
                .andExpect(jsonPath("$.progress.resolved").value(2))
                .andExpect(jsonPath("$.progress.sideboardIgnored").value(1));
    }

    @Test
    void getImport_running_returnsProgressWithoutDeck() throws Exception {
        DeckImportJob job = DeckImportJob.pending("job-1", "deck-1", "user-1",
                DeckImportFormat.TXT, DeckImportMode.REPLACE)
                .progress(DeckImportStatus.RUNNING, DeckImportPhase.RESOLVING,
                        new DeckImportProgress(99, 12, 10, 4), List.of());
        when(deckImportUseCase.findJob("deck-1", "job-1", "user-1")).thenReturn(job);

        mockMvc.perform(get("/api/v1/decks/deck-1/imports/job-1").with(user("user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.phase").value("RESOLVING"))
                .andExpect(jsonPath("$.progress.processed").value(12))
                // El mazo no se devuelve hasta que el job termina: si se devolviera, el
                // frontend pintaría un mazo a medias.
                .andExpect(jsonPath("$.deck").doesNotExist());
    }

    @Test
    void getImport_failed_returnsError() throws Exception {
        DeckImportJob job = DeckImportJob.pending("job-1", "deck-1", "user-1",
                DeckImportFormat.TXT, DeckImportMode.REPLACE)
                .failed("Scryfall falló: 503");
        when(deckImportUseCase.findJob("deck-1", "job-1", "user-1")).thenReturn(job);

        mockMvc.perform(get("/api/v1/decks/deck-1/imports/job-1").with(user("user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.error").value("Scryfall falló: 503"))
                .andExpect(jsonPath("$.deck").doesNotExist());
    }

    @Test
    void getImport_unknownJob_returns404() throws Exception {
        when(deckImportUseCase.findJob(any(), any(), any()))
                .thenThrow(new DeckImportNotFoundException("no existe"));

        mockMvc.perform(get("/api/v1/decks/deck-1/imports/nope").with(user("user-1")))
                .andExpect(status().isNotFound());
    }

    /** RF4: un job de otro mazo es 404, no 403. Un 403 confirmaría que el job existe. */
    @Test
    void getImport_jobOfAnotherDeck_returns404() throws Exception {
        when(deckImportUseCase.findJob(any(), any(), any()))
                .thenThrow(new DeckImportNotFoundException("no es de este mazo"));

        mockMvc.perform(get("/api/v1/decks/deck-1/imports/job-2").with(user("user-1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getImport_deckOwnedByAnotherUser_returns403() throws Exception {
        when(deckImportUseCase.findJob(any(), any(), any()))
                .thenThrow(new ForbiddenException("no tuyo"));

        mockMvc.perform(get("/api/v1/decks/deck-1/imports/job-1").with(user("user-1")))
                .andExpect(status().isForbidden());
    }

    /**
     * {@code GET /api/v1/**} es público en este proyecto (SecurityConfig), así que un GET anónimo
     * no puede fallar con 401: llega al caso de uso con {@code currentUserId = null} y la
     * validación de propiedad lo rechaza con 403. El 401 queda cubierto por los dos POST.
     */
    @Test
    void getImport_withoutAuthentication_returns403() throws Exception {
        when(deckImportUseCase.findJob(any(), any(), any()))
                .thenThrow(new ForbiddenException("No tienes permiso sobre este recurso"));

        mockMvc.perform(get("/api/v1/decks/deck-1/imports/job-1"))
                .andExpect(status().isForbidden());
    }
}