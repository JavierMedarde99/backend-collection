package com.wikicollection.domain.model;

/**
 * Un externalId pedido que no se ha creado porque el usuario ya lo tenía
 * enlazado a este mismo base. El motivo va como {@code String} porque es lo que
 * la API serializa.
 */
public record GameAdditionSkip(String externalId, String reason) {

    public static final String REASON_ALREADY_LINKED = "ALREADY_LINKED";
}
