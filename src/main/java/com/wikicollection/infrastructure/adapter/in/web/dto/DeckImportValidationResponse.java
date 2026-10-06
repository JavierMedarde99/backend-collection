package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.util.List;

import com.wikicollection.domain.model.DeckStatus;

/**
 * El resultado de validar el mazo tras importar (#353).
 *
 * <p>No reutiliza el {@code DeckStatusResponse} de los endpoints de mazo: aquel es
 * {@code (status, message)} y su {@code message} solo viene informado en {@code INVALID},
 * mientras que la importación necesita la lista entera de motivos para que el frontend los
 * pinte uno a uno. Un campo de texto único obligaría al frontend a partir una cadena para
 * sacar lo mismo.
 *
 * @param status {@code VALID} o {@code DRAFT}, según las reglas de Commander
 * @param reasons motivos que impiden que el mazo sea válido; vacío si lo es
 */
public record DeckImportValidationResponse(
        DeckStatus status,
        List<String> reasons) {
}