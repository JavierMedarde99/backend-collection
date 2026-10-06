package com.wikicollection.infrastructure.adapter.in.web.dto;

/**
 * Respuesta del 202 de una importación aceptada (#353).
 *
 * @param jobId id del trabajo, para consultar su avance
 * @param status estado inicial, siempre {@code PENDING}
 * @param statusUrl URL del GET que devuelve el avance, en el {@code Location} de la respuesta
 */
public record DeckImportAcceptedResponse(
        String jobId,
        String status,
        String statusUrl) {
}