package com.wikicollection.infrastructure.adapter.out.persistence;

import com.wikicollection.domain.model.BoardGame;

import org.springframework.stereotype.Component;

@Component
public class BoardGameEntityMapper {

    public BoardGameEntity toEntity(BoardGame boardGame) {
        if (boardGame == null) {
            return null;
        }
        return BoardGameEntity.builder()
                .id(boardGame.getId())
                .title(boardGame.getTitle())
                .description(boardGame.getDescription())
                .yearPublished(boardGame.getYearPublished())
                .minPlayers(boardGame.getMinPlayers())
                .maxPlayers(boardGame.getMaxPlayers())
                .minPlaytime(boardGame.getMinPlaytime())
                .maxPlaytime(boardGame.getMaxPlaytime())
                .publisher(boardGame.getPublisher())
                .designers(boardGame.getDesigners())
                .categories(boardGame.getCategories())
                .mechanics(boardGame.getMechanics())
                .imageUrl(boardGame.getImageUrl())
                .thumbnailUrl(boardGame.getThumbnailUrl())
                .bggRating(boardGame.getBggRating())
                .bggId(boardGame.getBggId())
                .status(boardGame.getStatus())
                .notes(boardGame.getNotes())
                .dateAdded(boardGame.getDateAdded())
                .build();
    }

    public BoardGame toDomain(BoardGameEntity entity) {
        if (entity == null) {
            return null;
        }
        return BoardGame.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .yearPublished(entity.getYearPublished())
                .minPlayers(entity.getMinPlayers())
                .maxPlayers(entity.getMaxPlayers())
                .minPlaytime(entity.getMinPlaytime())
                .maxPlaytime(entity.getMaxPlaytime())
                .publisher(entity.getPublisher())
                .designers(entity.getDesigners())
                .categories(entity.getCategories())
                .mechanics(entity.getMechanics())
                .imageUrl(entity.getImageUrl())
                .thumbnailUrl(entity.getThumbnailUrl())
                .bggRating(entity.getBggRating())
                .bggId(entity.getBggId())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .dateAdded(entity.getDateAdded())
                .build();
    }
}
