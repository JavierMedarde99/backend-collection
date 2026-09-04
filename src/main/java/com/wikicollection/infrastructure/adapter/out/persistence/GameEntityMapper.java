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
                .description(game.getDescription())
                .genre(game.getGenre())
                .platform(game.getPlatform())
                .publisher(game.getPublisher())
                .developer(game.getDeveloper())
                .releaseDate(game.getReleaseDate())
                .thumbnailUrl(game.getThumbnailUrl())
                .status(game.getStatus())
                .userRating(game.getUserRating())
                .notes(game.getNotes())
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
                .description(entity.getDescription())
                .genre(entity.getGenre())
                .platform(entity.getPlatform())
                .publisher(entity.getPublisher())
                .developer(entity.getDeveloper())
                .releaseDate(entity.getReleaseDate())
                .thumbnailUrl(entity.getThumbnailUrl())
                .status(entity.getStatus())
                .userRating(entity.getUserRating())
                .notes(entity.getNotes())
                .dateAdded(entity.getDateAdded())
                .dateCompleted(entity.getDateCompleted())
                .externalSource(entity.getExternalSource())
                .build();
    }
}