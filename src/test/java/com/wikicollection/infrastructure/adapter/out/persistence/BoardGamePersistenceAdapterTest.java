package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameSearchCriteria;
import com.wikicollection.domain.model.BoardGameStatus;

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
class BoardGamePersistenceAdapterTest {

    @Mock
    private SpringDataBoardGameRepository springDataBoardGameRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private BoardGameEntityMapper mapper;

    @InjectMocks
    private BoardGamePersistenceAdapter adapter;

    private BoardGame sampleGame() {
        return BoardGame.builder()
                .id("bg1")
                .title("Catan")
                .status(BoardGameStatus.OWNED)
                .build();
    }

    @Test
    void search_withoutFilters_returnsAllResults() {
        Pageable pageable = PageRequest.of(0, 20);
        BoardGameEntity entity = BoardGameEntity.builder().id("bg1").title("Catan").build();
        BoardGame expected = sampleGame();
        when(mongoTemplate.find(any(Query.class), eq(BoardGameEntity.class))).thenReturn(List.of(entity));
        when(mongoTemplate.count(any(Query.class), eq(BoardGameEntity.class))).thenReturn(1L);
        when(mapper.toDomain(entity)).thenReturn(expected);

        Page<BoardGame> result = adapter.search(new BoardGameSearchCriteria(null, null), pageable);

        assertThat(result.getContent()).containsExactly(expected);
    }

    @Test
    void search_withNameFilter_buildsCaseInsensitiveRegexOnTitle() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(BoardGameEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(BoardGameEntity.class))).thenReturn(0L);

        adapter.search(new BoardGameSearchCriteria("catan", null), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(BoardGameEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("title");
        assertThat(qs).contains("$regularExpression");
        assertThat(qs).contains("catan");
    }

    @Test
    void search_withStatusFilter_addsExactCriteria() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(BoardGameEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(BoardGameEntity.class))).thenReturn(0L);

        adapter.search(new BoardGameSearchCriteria(null, BoardGameStatus.WISHLIST), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(BoardGameEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("status");
    }

    @Test
    void findById_mapsEntity_whenExists() {
        BoardGameEntity entity = BoardGameEntity.builder().id("bg1").title("Catan").build();
        BoardGame expected = sampleGame();
        when(springDataBoardGameRepository.findById("bg1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        Optional<BoardGame> result = adapter.findById("bg1");

        assertThat(result).contains(expected);
    }

    @Test
    void save_mapsDomainToEntity_andBack() {
        BoardGame game = sampleGame();
        BoardGameEntity entity = BoardGameEntity.builder().id("bg1").title("Catan").build();
        when(mapper.toEntity(game)).thenReturn(entity);
        when(springDataBoardGameRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(game);

        BoardGame result = adapter.save(game);

        assertThat(result).isSameAs(game);
    }

    @Test
    void deleteById_delegatesToSpringData() {
        adapter.deleteById("bg1");

        verify(springDataBoardGameRepository).deleteById("bg1");
    }

    @Test
    void mapper_roundTripsAllFields() {
        BoardGame game = BoardGame.builder()
                .id("bg1")
                .title("Agricola")
                .description("Un juego de granjas")
                .yearPublished(2007)
                .minPlayers(1)
                .maxPlayers(5)
                .minPlaytime(30)
                .maxPlaytime(120)
                .publisher("Lookout Games")
                .designers(List.of("Uwe Rosenberg"))
                .categories(List.of("Agricultura"))
                .mechanics(List.of("Gestión de mano"))
                .imageUrl("http://img")
                .thumbnailUrl("http://thumb")
                .bggRating(new BigDecimal("8.3"))
                .bggId("31260")
                .status(BoardGameStatus.OWNED)
                .notes("Mi favorito")
                .dateAdded(LocalDate.of(2026, 1, 1))
                .build();

        BoardGame domain = new BoardGameEntityMapper().toDomain(new BoardGameEntityMapper().toEntity(game));

        assertThat(domain).usingRecursiveComparison().isEqualTo(game);
    }

    @Test
    void mapper_handlesNull() {
        BoardGameEntityMapper gameMapper = new BoardGameEntityMapper();

        assertThat(gameMapper.toEntity(null)).isNull();
        assertThat(gameMapper.toDomain(null)).isNull();
    }
}