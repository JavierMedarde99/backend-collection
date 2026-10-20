package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameStatus;

import org.junit.jupiter.api.Test;

class GameEntityMapperTest {

    private final GameEntityMapper mapper = new GameEntityMapper();

    @Test
    void toEntity_mapsAcquisitionDateAndPrice() {
        Game game = Game.builder()
                .id("g-1")
                .ownerId("owner-1")
                .title("The Witcher 3")
                .platform("PC")
                .status(GameStatus.COMPLETED)
                .acquisitionDate(LocalDate.of(2024, 3, 15))
                .acquisitionPrice(new BigDecimal("39.99"))
                .build();

        GameEntity entity = mapper.toEntity(game);

        assertEquals(LocalDate.of(2024, 3, 15), entity.getAcquisitionDate());
        assertEquals(new BigDecimal("39.99"), entity.getAcquisitionPrice());
    }

    @Test
    void toDomain_mapsAcquisitionDateAndPrice() {
        GameEntity entity = GameEntity.builder()
                .id("g-1")
                .ownerId("owner-1")
                .title("The Witcher 3")
                .platform("PC")
                .status(GameStatus.COMPLETED)
                .acquisitionDate(LocalDate.of(2024, 3, 15))
                .acquisitionPrice(new BigDecimal("39.99"))
                .build();

        Game game = mapper.toDomain(entity);

        assertEquals(LocalDate.of(2024, 3, 15), game.getAcquisitionDate());
        assertEquals(new BigDecimal("39.99"), game.getAcquisitionPrice());
    }

    @Test
    void toEntity_mapsParentFields() {
        Game game = Game.builder()
                .id("dlc-1")
                .ownerId("owner-1")
                .title("The Witcher 3: Blood and Wine")
                .platform("PC")
                .status(GameStatus.PLAYING)
                .parentId("g-1")
                .parentTitle("The Witcher 3")
                .build();

        GameEntity entity = mapper.toEntity(game);

        assertEquals("g-1", entity.getParentId());
        assertEquals("The Witcher 3", entity.getParentTitle());
    }

    @Test
    void toDomain_mapsParentFields() {
        GameEntity entity = GameEntity.builder()
                .id("dlc-1")
                .ownerId("owner-1")
                .title("The Witcher 3: Blood and Wine")
                .platform("PC")
                .status(GameStatus.PLAYING)
                .parentId("g-1")
                .parentTitle("The Witcher 3")
                .build();

        Game game = mapper.toDomain(entity);

        assertEquals("g-1", game.getParentId());
        assertEquals("The Witcher 3", game.getParentTitle());
    }

    @Test
    void toDomain_readsLegacyDocumentWithoutParentFields_asNull() {
        GameEntity entity = GameEntity.builder()
                .id("g-1")
                .title("The Witcher 3")
                .platform("PC")
                .status(GameStatus.PLAYING)
                .build();

        Game game = mapper.toDomain(entity);

        assertNull(game.getParentId());
        assertNull(game.getParentTitle());
    }

    @Test
    void toDomain_readsLegacyDocumentWithoutAcquisitionFields_asNull() {
        GameEntity entity = GameEntity.builder()
                .id("g-1")
                .title("The Witcher 3")
                .platform("PC")
                .status(GameStatus.PLAYING)
                .build();

        Game game = mapper.toDomain(entity);

        assertNull(game.getAcquisitionDate());
        assertNull(game.getAcquisitionPrice());
    }
}
