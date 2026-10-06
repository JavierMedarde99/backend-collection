package com.wikicollection.application.service;

import java.util.UUID;

import com.wikicollection.application.exception.DeckImportNotFoundException;
import com.wikicollection.application.exception.DeckNotFoundException;
import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckImportJob;
import com.wikicollection.domain.model.DeckImportMode;
import com.wikicollection.domain.port.in.DeckImportUseCase;
import com.wikicollection.domain.port.out.DeckRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

/**
 * El borde síncrono de la importación (#353): valida lo barato y encola el trabajo.
 *
 * <p>Las validaciones van antes de crear el job, y en orden de coste. Si el mazo no es del
 * usuario o el archivo no trae cartas, no debe quedar un job huérfano en el registro ni una
 * tarea encolada que nadie va a consultar: por eso nada se guarda hasta que todo ha pasado.
 *
 * <p>Los límites de tamaño y de número de líneas están aquí y no en el multipart del
 * controller porque son reglas del dominio de la importación, y porque un test unitario del
 * servicio es mucho más barato de escribir que uno con el contexto de Spring entero.
 */
@Service
public class DeckImportService implements DeckImportUseCase {

    private final DeckImportWorker worker;
    private final DeckImportJobStore store;
    private final DeckRepository deckRepository;
    private final OwnershipValidator ownershipValidator;
    private final DeckImportFormatDetector formatDetector;
    private final int maxEntries;
    private final int maxFileSize;

    public DeckImportService(DeckImportWorker worker,
                             DeckImportJobStore store,
                             DeckRepository deckRepository,
                             OwnershipValidator ownershipValidator,
                             DeckImportFormatDetector formatDetector,
                             @Value("${deck.import.max-entries:500}") int maxEntries,
                             @Value("${deck.import.max-file-size:5242880}") int maxFileSize) {
        this.worker = worker;
        this.store = store;
        this.deckRepository = deckRepository;
        this.ownershipValidator = ownershipValidator;
        this.formatDetector = formatDetector;
        this.maxEntries = maxEntries;
        this.maxFileSize = maxFileSize;
    }

    @Override
    public DeckImportJob startImport(String deckId, String content, DeckImportFormat format,
                                     DeckImportMode mode, String userId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new DeckNotFoundException("No existe el mazo " + deckId));
        ownershipValidator.validateOwner(deck.getOwnerId(), userId);

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        int entries = countNonBlankLines(content);
        if (entries > maxEntries) {
            throw new IllegalArgumentException("El archivo trae " + entries + " líneas y el máximo es "
                    + maxEntries);
        }
        if (content.length() > maxFileSize) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo de "
                    + formatSize(maxFileSize));
        }
        formatDetector.requireConsistent(format, content, null);

        DeckImportFormat resolvedFormat = format == null
                ? formatDetector.detect(content, null)
                : format;
        DeckImportJob job = DeckImportJob.pending(
                UUID.randomUUID().toString(), deckId, userId, resolvedFormat,
                mode == null ? DeckImportMode.REPLACE : mode);
        store.save(job);
        try {
            worker.run(job.jobId(), deckId, userId, content, resolvedFormat, job.mode());
        } catch (TaskRejectedException e) {
            // El executor está lleno y este job no va a correr nunca: sin este evict quedaría
            // un PENDING huérfano que el usuario vería eterno hasta que la TTL lo borrara.
            store.evict(job.jobId());
            throw e;
        }
        return job;
    }

    @Override
    public DeckImportJob findJob(String deckId, String jobId, String userId) {
        DeckImportJob job = store.find(jobId)
                .filter(candidate -> candidate.deckId().equals(deckId))
                .orElseThrow(() -> new DeckImportNotFoundException(
                        "No hay ninguna importación " + jobId + " en el mazo " + deckId));

        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new DeckNotFoundException("No existe el mazo " + deckId));
        ownershipValidator.validateOwner(deck.getOwnerId(), userId);
        return job;
    }

    private int countNonBlankLines(String content) {
        return (int) content.lines().filter(line -> !line.isBlank()).count();
    }

    private String formatSize(int bytes) {
        return bytes % (1024 * 1024) == 0
                ? (bytes / (1024 * 1024)) + " MB"
                : (bytes / 1024) + " KB";
    }
}