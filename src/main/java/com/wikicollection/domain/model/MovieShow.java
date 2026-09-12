package com.wikicollection.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
public class MovieShow {

    private String id;
    private String externalId;
    private String title;
    private String overview;
    private LocalDate releaseDate;
    private String posterUrl;
    private String backdropUrl;
    private Double voteAverage;
    private MovieMediaType mediaType;
    private MovieStatus status;
    private Integer userRating;
    private String comment;
    private LocalDate dateAdded;
    private LocalDate dateCompleted;
    private String externalSource;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
