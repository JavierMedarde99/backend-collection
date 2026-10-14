package com.wikicollection.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.AmiiboStatus;
import com.wikicollection.domain.model.AmiiboType;

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
 * Entidad Mongo de la colección {@code amiibos}. Las fechas de lanzamiento se
 * aplanan en cuatro campos ({@code releaseDateAu/Eu/Jp/Na}) para no depender de
 * cómo serialice Mongo un record anidado; el mapper reconstruye el record de
 * dominio {@code AmiiboReleaseDates}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Document(collection = "amiibos")
public class AmiiboEntity {

    @Id
    private String id;
    @Indexed
    private String ownerId;
    private UserOwnedEntity userOwned;

    @Indexed
    private String externalId;
    private String name;
    private String character;
    private String amiiboSeries;
    private String gameSeries;
    private AmiiboType amiiboType;
    private LocalDate releaseDateAu;
    private LocalDate releaseDateEu;
    private LocalDate releaseDateJp;
    private LocalDate releaseDateNa;
    private String imageUrl;
    private List<String> compatibleGames;
    private AmiiboStatus status;
    private String notes;
    private LocalDate dateAdded;
    private LocalDate acquisitionDate;
    private BigDecimal acquisitionPrice;
}
