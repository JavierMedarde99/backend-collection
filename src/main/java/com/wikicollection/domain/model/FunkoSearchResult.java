package com.wikicollection.domain.model;

/**
 * Resultado de la búsqueda externa en HobbyQuantHQ: subconjunto del catálogo
 * con los datos mínimos para poder añadir el Pop a la colección. El campo
 * {@code externalId} es el {@code id} entero del catálogo guardado como String.
 */
public record FunkoSearchResult(
        String externalId,
        String name,
        String series,
        String number,
        String category,
        String exclusive,
        Boolean vaulted,
        Integer releaseYear,
        String franchise,
        String popType,
        Integer rarityScore,
        String imageUrl,
        String externalSource) {

    public static final String EXTERNAL_SOURCE = "hobbyquanthq";
}
