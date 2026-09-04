package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.Game;

import org.springframework.stereotype.Component;

@Component
public class GameDtoMapper {

    public Game toDomain(GameRequest request) {
        if (request == null) {
            return null;
        }
        return Game.builder()
                .externalId(request.externalId())
                .title(request.title())
                .description(request.description())
                .genre(request.genre())
                .platform(request.platform())
                .publisher(request.publisher())
                .developer(request.developer())
                .releaseDate(request.releaseDate())
                .thumbnailUrl(request.thumbnailUrl())
                .status(request.status())
                .userRating(request.userRating())
                .notes(request.notes())
                .dateAdded(request.dateAdded())
                .dateCompleted(request.dateCompleted())
                .externalSource(request.externalSource())
                .build();
    }

    public GameResponse toResponse(Game game) {
        if (game == null) {
            return null;
        }
        return new GameResponse(
                game.getId(),
                game.getExternalId(),
                game.getTitle(),
                game.getDescription(),
                game.getGenre(),
                game.getPlatform(),
                game.getPublisher(),
                game.getDeveloper(),
                game.getReleaseDate(),
                game.getThumbnailUrl(),
                game.getStatus(),
                game.getUserRating(),
                game.getNotes(),
                game.getDateAdded(),
                game.getDateCompleted(),
                game.getExternalSource());
    }
}