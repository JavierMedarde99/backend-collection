package com.wikicollection.domain.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Un Funko Pop de la colección. {@code externalId} es el {@code id} entero del
 * catálogo de HobbyQuantHQ guardado como String y es la clave natural de la
 * colección. {@code rarityScore} es un índice de la propia API, no un valor de
 * mercado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Funko {

    private String id;
    private String ownerId;
    private UserOwned userOwned;
    private String externalId;
    private String name;
    private String series;
    private String number;
    private String category;
    private String exclusive;
    private Boolean vaulted;
    private Integer releaseYear;
    private String franchise;
    private String popType;
    private Integer rarityScore;
    private String imageUrl;
    private FunkoStatus status;
    private String notes;
    private LocalDate dateAdded;
}
