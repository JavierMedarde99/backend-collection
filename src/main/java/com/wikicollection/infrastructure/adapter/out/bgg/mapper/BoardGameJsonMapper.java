package com.wikicollection.infrastructure.adapter.out.bgg.mapper;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wikicollection.domain.model.BoardGameSearchResult;

import org.springframework.stereotype.Component;

@Component
public class BoardGameJsonMapper {

    private static final String EXTERNAL_SOURCE = "BGG";

    public List<BoardGameSearchResult> map(BggJsonGame[] games) {
        if (games == null) {
            return List.of();
        }
        return Arrays.stream(games)
                .map(this::toResult)
                .toList();
    }

    private BoardGameSearchResult toResult(BggJsonGame game) {
        return new BoardGameSearchResult(
                game.id(),
                game.name(),
                game.description(),
                game.yearPublished(),
                game.minPlayers(),
                game.maxPlayers(),
                game.minPlaytime(),
                game.maxPlaytime(),
                first(game.publishers()),
                emptyIfNull(game.designers()),
                emptyIfNull(game.categories()),
                emptyIfNull(game.mechanics()),
                game.imageUrl(),
                game.thumbUrl(),
                null,
                EXTERNAL_SOURCE);
    }

    private String first(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private List<String> emptyIfNull(List<String> values) {
        return values == null ? List.of() : values;
    }

    public record BggJsonGame(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("year_published") Integer yearPublished,
            @JsonProperty("min_players") Integer minPlayers,
            @JsonProperty("max_players") Integer maxPlayers,
            @JsonProperty("min_playtime") Integer minPlaytime,
            @JsonProperty("max_playtime") Integer maxPlaytime,
            @JsonProperty("publishers") List<String> publishers,
            @JsonProperty("designers") List<String> designers,
            @JsonProperty("categories") List<String> categories,
            @JsonProperty("mechanics") List<String> mechanics,
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("thumb_url") String thumbUrl) {
    }
}