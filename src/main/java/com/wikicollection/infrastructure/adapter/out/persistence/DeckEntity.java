package com.wikicollection.infrastructure.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;

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
@Document(collection = "decks")
public class DeckEntity {

    @Id
    private String id;

    private String name;

    private String description;

    private String commander;

    private List<String> commanderColors;

    private List<DeckCardEntity> cards;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
