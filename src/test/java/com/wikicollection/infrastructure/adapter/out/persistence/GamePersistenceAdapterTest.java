package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.model.GameStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class GamePersistenceAdapterTest {

    @Mock
    private SpringDataGameRepository springDataGameRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private GameEntityMapper mapper;

    @InjectMocks
    private GamePersistenceAdapter adapter;

    private Game sampleGame() {
        return Game.builder()
                .id("g1")
                .title("The Witcher 3")
                .status(GameStatus.PLAYING)
                .platform(GamePlatform.PC)
                .build();
    }

    @Test
    void search_withoutFilters_returnsAllResults() {
        Pageable pageable = PageRequest.of(0, 20);
        GameEntity entity = GameEntity.builder().id("g1").title("The Witcher 3").build();
        Game expected = sampleGame();
        when(mongoTemplate.find(any(Query.class), eq(GameEntity.class))).thenReturn(List.of(entity));
        when(mongoTemplate.count(any(Query.class), eq(GameEntity.class))).thenReturn(1L);
        when(mapper.toDomain(entity)).thenReturn(expected);

        Page<Game> result = adapter.search(new GameSearchCriteria(null, null, null), pageable);

        assertThat(result.getContent()).containsExactly(expected);
    }

    @Test
    void search_withNameFilter_buildsCaseInsensitiveRegexOnTitle() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(GameEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(GameEntity.class))).thenReturn(0L);

        adapter.search(new GameSearchCriteria("witc", null, null), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(GameEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("title");
        assertThat(qs).contains("$regularExpression");
        assertThat(qs).contains("witc");
    }

    @Test
    void search_withPlatformFilter_addsExactCriteria() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(GameEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(GameEntity.class))).thenReturn(0L);

        adapter.search(new GameSearchCriteria(null, GamePlatform.PC, GameStatus.PLAYING), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(GameEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("platform");
        assertThat(qs).contains("status");
    }

    @Test
    void findById_mapsEntity_whenExists() {
        GameEntity entity = GameEntity.builder().id("g1").title("The Witcher 3").build();
        Game expected = sampleGame();
        when(springDataGameRepository.findById("g1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        Optional<Game> result = adapter.findById("g1");

        assertThat(result).contains(expected);
    }

    @Test
    void save_mapsDomainToEntity_andBack() {
        Game game = sampleGame();
        GameEntity entity = GameEntity.builder().id("g1").title("The Witcher 3").build();
        when(mapper.toEntity(game)).thenReturn(entity);
        when(springDataGameRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(game);

        Game result = adapter.save(game);

        assertThat(result).isSameAs(game);
    }

    @Test
    void deleteById_delegatesToSpringData() {
        adapter.deleteById("g1");

        verify(springDataGameRepository).deleteById("g1");
    }

    @Test
    void mapper_roundTripsAllFields() {
        Game game = Game.builder()
                .id("g1")
                .externalId("rawg-001")
                .title("Hollow Knight")
                .platform(GamePlatform.PC)
                .thumbnailUrl("http://img")
                .status(GameStatus.COMPLETED)
                .userRating(5)
                .comment("Obra maestra")
                .dateAdded(java.time.LocalDateTime.of(2024, 1, 1, 10, 0))
                .dateCompleted(java.time.LocalDateTime.of(2024, 2, 1, 18, 0))
                .externalSource("RAWG")
                .build();

        Game domain = new GameEntityMapper().toDomain(new GameEntityMapper().toEntity(game));

        assertThat(domain).usingRecursiveComparison().isEqualTo(game);
    }

    @Test
    void mapper_handlesNull() {
        GameEntityMapper gameMapper = new GameEntityMapper();

        assertThat(gameMapper.toEntity(null)).isNull();
        assertThat(gameMapper.toDomain(null)).isNull();
    }
}