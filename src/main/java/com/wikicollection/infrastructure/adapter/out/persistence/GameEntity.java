package com.wikicollection.infrastructure.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameStatus;

import org.springframework.data.annotation.Id;
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
@Document(collection = "GAMES")
public class GameEntity {

    @Id
    private String id;

    private String externalId;

    private String title;

    private String description;

    private String genre;

    private GamePlatform platform;

    private String publisher;

    private String developer;

    private LocalDate releaseDate;

    private String thumbnailUrl;

    private GameStatus status;

    private Integer userRating;

    private String notes;

    private LocalDateTime dateAdded;

    private LocalDateTime dateCompleted;

    private String externalSource;
}