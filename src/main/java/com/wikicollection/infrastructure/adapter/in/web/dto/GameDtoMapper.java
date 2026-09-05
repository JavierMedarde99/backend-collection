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
                .platform(request.platform())
                .thumbnailUrl(request.thumbnailUrl())
                .status(request.status())
                .userRating(request.userRating())
                .comment(request.comment())
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
                game.getPlatform(),
                game.getThumbnailUrl(),
                game.getStatus(),
                game.getUserRating(),
                game.getComment(),
                game.getDateAdded(),
                game.getDateCompleted(),
                game.getExternalSource());
    }
}