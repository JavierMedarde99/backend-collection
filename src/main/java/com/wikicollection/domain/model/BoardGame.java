package com.wikicollection.domain.model;

import java.math.BigDecimal;
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
public class BoardGame {

    private String id;
    private String title;
    private String description;
    private Integer yearPublished;
    private Integer minPlayers;
    private Integer maxPlayers;
    private Integer minPlaytime;
    private Integer maxPlaytime;
    private String publisher;
    private List<String> designers;
    private List<String> categories;
    private List<String> mechanics;
    private String imageUrl;
    private String thumbnailUrl;
    private BigDecimal bggRating;
    private String bggId;
    private BoardGameStatus status;
    private String notes;
    private LocalDate dateAdded;
}
