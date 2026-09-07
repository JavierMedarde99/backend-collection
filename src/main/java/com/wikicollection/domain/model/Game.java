package com.wikicollection.domain.model;

import java.time.LocalDate;

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
public class Game {

    private String id;
    private String externalId;
    private String title;
    private GamePlatform platform;
    private String thumbnailUrl;
    private GameStatus status;
    private Integer userRating;
    private String comment;
    private LocalDate dateAdded;
    private LocalDate dateCompleted;
    private String externalSource;
}