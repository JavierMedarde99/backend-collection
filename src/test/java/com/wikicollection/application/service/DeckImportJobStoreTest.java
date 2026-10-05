package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckImportJob;
import com.wikicollection.domain.model.DeckImportMode;
import com.wikicollection.domain.model.DeckImportPhase;
import com.wikicollection.domain.model.DeckImportProgress;
import com.wikicollection.domain.model.DeckImportStatus;
import com.wikicollection.domain.model.DeckStatus;
import com.wikicollection.domain.model.DeckStatusReport;
import com.wikicollection.domain.model.UnresolvedCardEntry;
import com.wikicollection.domain.model.UnresolvedReason;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * El registro de trabajos de importación (#353).
 *
 * <p>El job es un record inmutable que el worker reescribe entero con cada avance, así el
 * {@code GET} de estado nunca observa un job a medias. Eso obliga a que los métodos de copia
 * arrastren los campos que no cambian (dueño, mazo, formato, modo y creación): si uno se
 * olvida, el job deja de ser consultable por su propietario sin que nada falle.
 */
class DeckImportJobStoreTest {

    private DeckImportJobStore store;

    @BeforeEach
    void setUp() {
        Cache<String, DeckImportJob> cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .maximumSize(200)
                .build();
        store = new DeckImportJobStore(cache);
    }

    @Test
    void saveThenFind_returnsTheJob() {
        store.save(pending());

        DeckImportJob found = store.find("job-1").orElseThrow();

        assertThat(found.jobId()).isEqualTo("job-1");
        assertThat(found.deckId()).isEqualTo("deck-1");
        assertThat(found.ownerId()).isEqualTo("user-1");
        assertThat(found.status()).isEqualTo(DeckImportStatus.PENDING);
    }

    @Test
    void find_unknownJob_returnsEmpty() {
        assertThat(store.find("nope")).isEmpty();
    }

    @Test
    void savingReplacesThePreviousSnapshot() {
        store.save(pending());

        store.save(pending().progress(DeckImportStatus.RUNNING, DeckImportPhase.RESOLVING,
                new DeckImportProgress(3, 1, 1, 2), List.of()));

        DeckImportJob found = store.find("job-1").orElseThrow();
        assertThat(found.status()).isEqualTo(DeckImportStatus.RUNNING);
        assertThat(found.phase()).isEqualTo(DeckImportPhase.RESOLVING);
        assertThat(found.progress()).isEqualTo(new DeckImportProgress(3, 1, 1, 2));
    }

    @Test
    void evict_removesTheJob() {
        store.save(pending());

        store.evict("job-1");

        assertThat(store.find("job-1")).isEmpty();
    }

    @Test
    void progressCopyCarriesForwardTheImmutableFields() {
        DeckImportJob original = pending();

        DeckImportJob copy = original.progress(DeckImportStatus.RUNNING, DeckImportPhase.PARSING,
                new DeckImportProgress(0, 0, 0, 0), List.of());

        assertThat(copy.jobId()).isEqualTo("job-1");
        assertThat(copy.deckId()).isEqualTo("deck-1");
        assertThat(copy.ownerId()).isEqualTo("user-1");
        assertThat(copy.format()).isEqualTo(DeckImportFormat.TXT);
        assertThat(copy.mode()).isEqualTo(DeckImportMode.REPLACE);
        assertThat(copy.createdAt()).isEqualTo(original.createdAt());
        assertThat(copy.completedAt()).isNull();
        assertThat(copy.updatedAt()).isAfterOrEqualTo(original.createdAt());
    }

    @Test
    void completedCopyStoresTheReportAndFinishes() {
        DeckStatusReport report = new DeckStatusReport(DeckStatus.DRAFT, List.of("El mazo no llega a 99 cartas"));

        DeckImportJob copy = pending().completed(report, "Atraxa, Grand Unifier", List.of("G", "W"));

        assertThat(copy.status()).isEqualTo(DeckImportStatus.COMPLETED);
        assertThat(copy.phase()).isEqualTo(DeckImportPhase.DONE);
        assertThat(copy.validation()).isEqualTo(report);
        assertThat(copy.commanderName()).isEqualTo("Atraxa, Grand Unifier");
        assertThat(copy.commanderColors()).containsExactly("G", "W");
        assertThat(copy.completedAt()).isNotNull();
        assertThat(copy.error()).isNull();
    }

    @Test
    void failedCopyKeepsTheErrorAndFinishes() {
        DeckImportJob copy = pending().failed("Scryfall falló: 503");

        assertThat(copy.status()).isEqualTo(DeckImportStatus.FAILED);
        assertThat(copy.completedAt()).isNotNull();
        assertThat(copy.error()).isEqualTo("Scryfall falló: 503");
    }

    @Test
    void copiesDoNotShareTheUnresolvedList() {
        List<UnresolvedCardEntry> unresolved = List.of(
                new UnresolvedCardEntry(7, "Llanuras", 4, "Llanuras", UnresolvedReason.NOT_FOUND, List.of()));

        DeckImportJob copy = pending().progress(DeckImportStatus.RUNNING, DeckImportPhase.RESOLVING,
                new DeckImportProgress(1, 1, 0, 0), unresolved);

        assertThat(copy.unresolved()).containsExactlyElementsOf(unresolved);
        assertThat(pending().unresolved()).isEmpty();
    }

    private DeckImportJob pending() {
        return DeckImportJob.pending("job-1", "deck-1", "user-1", DeckImportFormat.TXT, DeckImportMode.REPLACE);
    }
}