package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.BoardGame;

import org.springframework.stereotype.Component;

@Component
public class BoardGameDtoMapper {

    public BoardGame toDomain(BoardGameRequest request) {
        if (request == null) {
            return null;
        }
        return BoardGame.builder()
                .title(request.title())
                .description(request.description())
                .yearPublished(request.yearPublished())
                .minPlayers(request.minPlayers())
                .maxPlayers(request.maxPlayers())
                .minPlaytime(request.minPlaytime())
                .maxPlaytime(request.maxPlaytime())
                .publisher(request.publisher())
                .designers(request.designers())
                .categories(request.categories())
                .mechanics(request.mechanics())
                .imageUrl(request.imageUrl())
                .thumbnailUrl(request.thumbnailUrl())
                .bggRating(request.bggRating())
                .bggId(request.bggId())
                .status(request.status())
                .notes(request.notes())
                .dateAdded(request.dateAdded())
                .build();
    }

    public BoardGameResponse toResponse(BoardGame boardGame) {
        if (boardGame == null) {
            return null;
        }
        return new BoardGameResponse(
                boardGame.getId(),
                boardGame.getTitle(),
                boardGame.getDescription(),
                boardGame.getYearPublished(),
                boardGame.getMinPlayers(),
                boardGame.getMaxPlayers(),
                boardGame.getMinPlaytime(),
                boardGame.getMaxPlaytime(),
                boardGame.getPublisher(),
                boardGame.getDesigners(),
                boardGame.getCategories(),
                boardGame.getMechanics(),
                boardGame.getImageUrl(),
                boardGame.getThumbnailUrl(),
                boardGame.getBggRating(),
                boardGame.getBggId(),
                boardGame.getStatus(),
                boardGame.getNotes(),
                boardGame.getDateAdded());
    }
}