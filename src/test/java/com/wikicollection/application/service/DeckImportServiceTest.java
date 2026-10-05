package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.wikicollection.application.exception.DeckImportNotFoundException;
import com.wikicollection.application.exception.DeckNotFoundException;
import com.wikicollection.application.exception.ForbiddenException;
import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckImportJob;
import com.wikicollection.domain.model.DeckImportMode;
import com.wikicollection.domain.model.DeckImportStatus;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.infrastructure.adapter.in.decklist.DeckImportFormatDetector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El borde síncrono de la importación (#353): qué se acepta y qué se rechaza con 400 antes de
 * crear un job, y qué se devuelve al consultar uno.
 *
 * <p>Lo que se fija aquí es el <strong>orden</strong> de las validaciones. Todas las baratas
 * van antes de crear nada: si el mazo no es del usuario o el archivo no tiene cartas, no debe
 * quedar un job huérfano en el registro ni una tarea encolada que nadie va a consultar. Y
 * cuando el formato declarado no casa con el contenido se rechaza en vez de adivinar: un CSV
 * que el cliente dice que es JSON acabaría con un error de parseo en segundo plano, en lugar
 * de un 400 que explica el problema.
 */
@ExtendWith(MockitoExtension.class)
class DeckImportServiceTest {

    private static final String TXT_CONTENT = "COMMANDER\n1 Sol Ring\n";

    @Mock
    private DeckImportWorker worker;
    @Mock
    private DeckImportJobStore store;
    @Mock
    private DeckRepository deckRepository;
    @Mock
    private OwnershipValidator ownershipValidator;

    private DeckImportService service;

    @BeforeEach
    void setUp() {
        service = new DeckImportService(worker, store, deckRepository, ownershipValidator,
                new DeckImportFormatDetector(), 500, 5 * 1024 * 1024);
    }

    // ------------------------------------------------------------------- arranque

    @Test
    void startImport_createsPendingJobAndDelegatesToWorker() {
        givenDeck();

        DeckImportJob job = service.startImport("deck-1", TXT_CONTENT, DeckImportFormat.TXT,
                DeckImportMode.REPLACE, "user-1");

        assertThat(job.jobId()).isNotBlank();
        assertThat(job.status()).isEqualTo(DeckImportStatus.PENDING);
        assertThat(job.deckId()).isEqualTo("deck-1");
        assertThat(job.ownerId()).isEqualTo("user-1");
        assertThat(job.format()).isEqualTo(DeckImportFormat.TXT);
        assertThat(job.mode()).isEqualTo(DeckImportMode.REPLACE);
        assertThat(job.progress().total()).isZero();

        verify(store).save(job);
        verify(worker).run(eq(job.jobId()), eq("deck-1"), eq("user-1"), eq(TXT_CONTENT),
                eq(DeckImportFormat.TXT), eq(DeckImportMode.REPLACE));
    }

    @Test
    void startImport_validatesOwnershipBeforeCreatingAnything() {
        Deck deck = Deck.builder().id("deck-1").ownerId("other").build();
        when(deckRepository.findById("deck-1")).thenReturn(Optional.of(deck));
        doThrow(new ForbiddenException("no tuyo"))
                .when(ownershipValidator).validateOwner("other", "user-1");

        assertThatThrownBy(() -> service.startImport("deck-1", TXT_CONTENT, DeckImportFormat.TXT,
                DeckImportMode.REPLACE, "user-1")).isInstanceOf(ForbiddenException.class);

        verify(store, never()).save(any());
        verify(worker, never()).run(anyString(), anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    void startImport_unknownDeck_throws() {
        when(deckRepository.findById("deck-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startImport("deck-1", TXT_CONTENT, DeckImportFormat.TXT,
                DeckImportMode.REPLACE, "user-1")).isInstanceOf(DeckNotFoundException.class);

        verify(store, never()).save(any());
    }

    @Test
    void startImport_blankContent_throws() {
        givenDeck();

        assertThatThrownBy(() -> service.startImport("deck-1", "   \n\n ", DeckImportFormat.TXT,
                DeckImportMode.REPLACE, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void startImport_tooManyEntries_throws() {
        givenDeck();
        String content = "1 Sol Ring\n".repeat(501);

        assertThatThrownBy(() -> service.startImport("deck-1", content, DeckImportFormat.TXT,
                DeckImportMode.REPLACE, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    void startImport_atTheEntryLimit_isAccepted() {
        givenDeck();

        assertThat(service.startImport("deck-1", "1 Sol Ring\n".repeat(500), DeckImportFormat.TXT,
                DeckImportMode.REPLACE, "user-1")).isNotNull();
    }

    @Test
    void startImport_oversizedContent_throws() {
        givenDeck();
        String content = "1 Sol Ring\n".repeat(600_000);

        assertThatThrownBy(() -> service.startImport("deck-1", content, DeckImportFormat.TXT,
                DeckImportMode.REPLACE, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5");
    }

    @Test
    void startImport_declaredFormatInconsistentWithContent_throws() {
        givenDeck();

        assertThatThrownBy(() -> service.startImport("deck-1", TXT_CONTENT, DeckImportFormat.JSON,
                DeckImportMode.REPLACE, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JSON");
    }

    @Test
    void startImport_detectsFormatFromContentWhenNotDeclared() {
        givenDeck();

        DeckImportJob job = service.startImport("deck-1", "{\"cards\":[{\"name\":\"Sol Ring\",\"quantity\":4}]}",
                null, DeckImportMode.REPLACE, "user-1");

        assertThat(job.format()).isEqualTo(DeckImportFormat.JSON);
        verify(worker).run(anyString(), anyString(), anyString(), anyString(),
                eq(DeckImportFormat.JSON), eq(DeckImportMode.REPLACE));
    }

    @Test
    void startImport_declaredFormatTolerantWithJsonContent() {
        givenDeck();

        // El cliente declara JSON y el contenido es JSON: la deduction tiene que coincidir.
        DeckImportJob job = service.startImport("deck-1", "  [{\"name\":\"Sol Ring\",\"quantity\":4}]",
                DeckImportFormat.JSON, DeckImportMode.REPLACE, "user-1");

        assertThat(job.format()).isEqualTo(DeckImportFormat.JSON);
    }

    @Test
    void startImport_undetectableFormatFallsBackToTxt() {
        givenDeck();

        DeckImportJob job = service.startImport("deck-1", TXT_CONTENT, null,
                DeckImportMode.MERGE, "user-1");

        assertThat(job.format()).isEqualTo(DeckImportFormat.TXT);
        assertThat(job.mode()).isEqualTo(DeckImportMode.MERGE);
    }

    @Test
    void startImport_eachJobGetsItsOwnId() {
        givenDeck();

        String first = service.startImport("deck-1", TXT_CONTENT, DeckImportFormat.TXT,
                DeckImportMode.REPLACE, "user-1").jobId();
        String second = service.startImport("deck-1", TXT_CONTENT, DeckImportFormat.TXT,
                DeckImportMode.REPLACE, "user-1").jobId();

        assertThat(first).isNotEqualTo(second);
    }

    // ---------------------------------------------------------------- consulta

    @Test
    void findJob_returnsTheJobForTheOwner() {
        DeckImportJob job = DeckImportJob.pending("job-1", "deck-1", "user-1",
                DeckImportFormat.TXT, DeckImportMode.REPLACE);
        when(store.find("job-1")).thenReturn(Optional.of(job));
        givenDeck();

        assertThat(service.findJob("deck-1", "job-1", "user-1")).isEqualTo(job);
    }

    @Test
    void findJob_unknownJob_throws() {
        when(store.find("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findJob("deck-1", "nope", "user-1"))
                .isInstanceOf(DeckImportNotFoundException.class);
    }

    /**
     * RF4: un job que pertenece a otro mazo no se anuncia como encontrado. Si el store
     * devolviera 404 por el id del job, un usuario podría deducir que un job ajeno existe
     * probando ids; y semanticamente la ruta es por mazo, así que lo que no está en este mazo
     * no existe aquí.
     */
    @Test
    void findJob_jobOfAnotherDeck_throws() {
        when(store.find("job-2")).thenReturn(Optional.of(DeckImportJob.pending("job-2", "deck-2", "user-1",
                DeckImportFormat.TXT, DeckImportMode.REPLACE)));

        assertThatThrownBy(() -> service.findJob("deck-1", "job-2", "user-1"))
                .isInstanceOf(DeckImportNotFoundException.class);
    }

    @Test
    void findJob_deckOwnedByAnotherUser_throws() {
        when(store.find("job-1")).thenReturn(Optional.of(DeckImportJob.pending("job-1", "deck-1", "otro",
                DeckImportFormat.TXT, DeckImportMode.REPLACE)));
        Deck deck = Deck.builder().id("deck-1").ownerId("otro").build();
        when(deckRepository.findById("deck-1")).thenReturn(Optional.of(deck));
        doThrow(new ForbiddenException("no tuyo"))
                .when(ownershipValidator).validateOwner("otro", "user-1");

        assertThatThrownBy(() -> service.findJob("deck-1", "job-1", "user-1"))
                .isInstanceOf(ForbiddenException.class);
    }

    // ------------------------------------------------------------------ ayuda

    private void givenDeck() {
        lenient().when(deckRepository.findById("deck-1"))
                .thenReturn(Optional.of(Deck.builder().id("deck-1").ownerId("user-1").build()));
    }
}