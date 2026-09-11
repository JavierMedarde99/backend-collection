package com.wikicollection.infrastructure.adapter.out.persistence;

import java.time.LocalDate;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieStatus;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
@Document(collection = "movie_shows")
public class MovieShowEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String externalId;

    @Indexed
    private String title;

    private String overview;

    private LocalDate releaseDate;

    private String posterUrl;

    private String backdropUrl;

    private Double voteAverage;

    private MovieMediaType mediaType;

    @Indexed
    private MovieStatus status;

    private Integer userRating;

    private String comment;

    private LocalDate dateAdded;

    private LocalDate dateCompleted;

    private String externalSource;
}
