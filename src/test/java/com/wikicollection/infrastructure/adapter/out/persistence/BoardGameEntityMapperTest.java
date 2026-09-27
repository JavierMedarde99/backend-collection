package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameStatus;
import com.wikicollection.domain.model.Difficulty;

class BoardGameEntityMapperTest {

    private final BoardGameEntityMapper mapper = new BoardGameEntityMapper();

    @Test
    void toEntity_mapsPersonalRatingPlayCountLastPlayedDateAndDifficulty() {
        BoardGame boardGame = BoardGame.builder()
                .id("bg-1")
                .ownerId("owner-1")
                .title("Catan")
                .personalRating(8)
                .playCount(42)
                .lastPlayedDate(LocalDate.of(2026, 9, 20))
                .difficulty(Difficulty.MEDIUM)
                .build();

        BoardGameEntity entity = mapper.toEntity(boardGame);

        assertEquals(8, entity.getPersonalRating());
        assertEquals(42, entity.getPlayCount());
        assertEquals(LocalDate.of(2026, 9, 20), entity.getLastPlayedDate());
        assertEquals(Difficulty.MEDIUM, entity.getDifficulty());
    }

    @Test
    void toDomain_mapsPersonalRatingPlayCountLastPlayedDateAndDifficulty() {
        BoardGameEntity entity = BoardGameEntity.builder()
                .id("bg-1")
                .ownerId("owner-1")
                .title("Catan")
                .status(BoardGameStatus.OWNED)
                .personalRating(7)
                .playCount(10)
                .lastPlayedDate(LocalDate.of(2026, 8, 15))
                .difficulty(Difficulty.HARD)
                .build();

        BoardGame boardGame = mapper.toDomain(entity);

        assertEquals(7, boardGame.getPersonalRating());
        assertEquals(10, boardGame.getPlayCount());
        assertEquals(LocalDate.of(2026, 8, 15), boardGame.getLastPlayedDate());
        assertEquals(Difficulty.HARD, boardGame.getDifficulty());
    }

    @Test
    void toEntity_nullInput_returnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toDomain_nullInput_returnsNull() {
        assertNull(mapper.toDomain(null));
    }
}
