package com.wikicollection.domain.model;

/**
 * Un bggId pedido que no se ha creado porque ya estaba en la colección del
 * usuario. El motivo va como {@code String} porque es lo que la API serializa.
 */
public record BoardGameExpansionSkip(String bggId, String reason) {

    public static final String REASON_ALREADY_IN_COLLECTION = "ALREADY_IN_COLLECTION";
}
