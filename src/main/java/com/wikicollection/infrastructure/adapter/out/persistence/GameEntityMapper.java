package com.wikicollection.infrastructure.adapter.out.persistence;

import com.wikicollection.domain.model.Game;

import org.springframework.stereotype.Component;

@Component
public class GameEntityMapper {

    public GameEntity toEntity(Game game) {
        if (game == null) {
            return null;
        }
        return GameEntity.builder()
                .id(game.getId())
                .externalId(game.getExternalId())
                .title(game.getTitle())
                .platform(game.getPlatform())
                .thumbnailUrl(game.getThumbnailUrl())
                .status(game.getStatus())
                .userRating(game.getUserRating())
                .comment(game.getComment())
                .dateAdded(game.getDateAdded())
                .dateCompleted(game.getDateCompleted())
                .externalSource(game.getExternalSource())
                .build();
    }

    public Game toDomain(GameEntity entity) {
        if (entity == null) {
            return null;
        }
        return Game.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .title(entity.getTitle())
                .platform(entity.getPlatform())
                .thumbnailUrl(entity.getThumbnailUrl())
                .status(entity.getStatus())
                .userRating(entity.getUserRating())
                .comment(entity.getComment())
                .dateAdded(entity.getDateAdded())
                .dateCompleted(entity.getDateCompleted())
                .externalSource(entity.getExternalSource())
                .build();
    }
}