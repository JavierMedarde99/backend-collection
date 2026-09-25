package com.wikicollection.infrastructure.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameStatus;

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
@Document(collection = "games")
public class GameEntity {

    @Id
    private String id;
    @Indexed
    private String ownerId;
    private UserOwnedEntity userOwned;

    private String externalId;

    private String title;

    private List<String> genres;

    private GamePlatform platform;

    private String thumbnailUrl;

    private GameStatus status;

    private Integer userRating;

    private String comment;

    private LocalDate dateAdded;

    private LocalDate dateCompleted;

    private String externalSource;

    private String steamAppId;
}