package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckImportJob;
import com.wikicollection.domain.model.DeckStatusReport;
import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.domain.model.UnresolvedCardEntry;

import org.springframework.stereotype.Component;

/**
 * Traduce el job de importación a lo que ve el cliente (#353).
 */
@Component
public class DeckImportDtoMapper {

    /**
     * @param statusUrl URL del GET que devuelve el avance; se repite en el cuerpo además de
     *                  en el {@code Location} para que un cliente que solo mira el JSON no
     *                  tenga que reconstruirla
     */
    public DeckImportAcceptedResponse toAccepted(DeckImportJob job, String statusUrl) {
        return new DeckImportAcceptedResponse(job.jobId(), job.status().name(), statusUrl);
    }

    /**
     * @param deck el mazo guardado, o {@code null} si el job no ha terminado. Es
     *             intencionado que no se invente: mientras el job corre, el mazo en Mongo es
     *             el de antes de la importación, y devolverlo haría que el frontend pintara un
     *             mazo a medias.
     */
    public DeckImportJobResponse toJobResponse(DeckImportJob job, Deck deck, DeckDtoMapper deckMapper) {
        return new DeckImportJobResponse(
                job.jobId(),
                job.status().name(),
                job.phase().name(),
                deck == null ? null : deckMapper.toResponse(deck),
                job.commanderName(),
                job.commanderColors(),
                job.unresolved().stream().map(this::toUnresolved).toList(),
                toValidation(job.validation()),
                job.error(),
                new DeckImportProgressResponse(
                        job.progress().total(),
                        job.progress().processed(),
                        job.progress().resolved(),
                        job.progress().sideboardIgnored()),
                job.createdAt(),
                job.updatedAt(),
                job.completedAt());
    }

    private DeckImportValidationResponse toValidation(DeckStatusReport report) {
        return report == null ? null : new DeckImportValidationResponse(report.status(), report.reasons());
    }

    private DeckImportUnresolvedResponse toUnresolved(UnresolvedCardEntry entry) {
        return new DeckImportUnresolvedResponse(
                entry.line(),
                entry.raw(),
                entry.quantity(),
                entry.name(),
                entry.reason().name(),
                entry.candidates().stream().map(this::toCandidate).toList());
    }

    private DeckImportCandidateResponse toCandidate(MagicCardSearchResult card) {
        return new DeckImportCandidateResponse(
                card.scryfallId(),
                card.name(),
                card.manaCost(),
                card.type(),
                card.rarity(),
                card.setCode(),
                card.setName(),
                card.imageUrl(),
                card.priceUsd(),
                card.colorIdentity());
    }
}