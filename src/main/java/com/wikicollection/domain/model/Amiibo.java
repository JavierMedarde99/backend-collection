package com.wikicollection.domain.model;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Amiibo {

    private String id;
    private String ownerId;
    private UserOwned userOwned;
    private String externalId;
    private String name;
    private String character;
    private String amiiboSeries;
    private String gameSeries;
    private AmiiboType amiiboType;
    private AmiiboReleaseDates releaseDates;
    private String imageUrl;
    private List<String> compatibleGames;
    private AmiiboStatus status;
    private String notes;
    private LocalDate dateAdded;
}
