package com.wikicollection.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.BoardGameStatus;

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
@Document(collection = "board_games")
public class BoardGameEntity {

    @Id
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
