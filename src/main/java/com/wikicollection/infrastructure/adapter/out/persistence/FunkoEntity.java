package com.wikicollection.infrastructure.adapter.out.persistence;

import java.time.LocalDate;

import com.wikicollection.domain.model.FunkoStatus;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Entidad Mongo de la colección {@code funkos}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Document(collection = "funkos")
public class FunkoEntity {

    @Id
    private String id;
    @Indexed
    private String ownerId;
    private UserOwnedEntity userOwned;

    @Indexed
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
