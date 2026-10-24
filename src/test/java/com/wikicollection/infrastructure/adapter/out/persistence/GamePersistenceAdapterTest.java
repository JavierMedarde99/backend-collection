package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.bson.Document;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameGroup;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.model.GameStatus;
import com.wikicollection.domain.model.GameTypeFilter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
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
                .platform("PC")
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

        Page<Game> result = adapter.search(new GameSearchCriteria(null, null, null, null, null, null, null), pageable);

        assertThat(result.getContent()).containsExactly(expected);
    }

    @Test
    void search_withNameFilter_buildsCaseInsensitiveRegexOnTitle() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(GameEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(GameEntity.class))).thenReturn(0L);

        adapter.search(new GameSearchCriteria("witc", null, null, null, null, null, null), pageable);

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

        adapter.search(new GameSearchCriteria(null, List.of("PC"), GameStatus.PLAYING, null, null, null, null), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(GameEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("platform");
        assertThat(qs).contains("status");
    }

    @Test
    void search_withSeveralPlatforms_filtersByAnyOfThem() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(GameEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(GameEntity.class))).thenReturn(0L);

        adapter.search(new GameSearchCriteria(null, List.of("PC", "PlayStation 5"), null, null, null, null, null),
                pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(GameEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("platform").contains("$in");
        assertThat(qs).contains("PC").contains("PlayStation 5");
    }

    @Test
    void search_withoutPlatforms_doesNotAddCriteria() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(GameEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(GameEntity.class))).thenReturn(0L);

        adapter.search(new GameSearchCriteria(null, List.of(), null, null, null, null, null), pageable);
        ArgumentCaptor<Query> emptyCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(emptyCaptor.capture(), eq(GameEntity.class));
        assertThat(emptyCaptor.getValue().toString()).doesNotContain("platform");

        adapter.search(new GameSearchCriteria(null, java.util.Arrays.asList("  ", null), null, null, null, null, null),
                pageable);
        ArgumentCaptor<Query> blankCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, org.mockito.Mockito.times(2)).find(blankCaptor.capture(), eq(GameEntity.class));
        assertThat(blankCaptor.getAllValues().get(1).toString()).doesNotContain("platform");
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
                .platform("PC")
                .thumbnailUrl("http://img")
                .status(GameStatus.COMPLETED)
                .userRating(5)
                .comment("Obra maestra")
                .dateAdded(LocalDate.of(2024, 1, 1))
                .dateCompleted(LocalDate.of(2024, 2, 1))
                .externalSource("RAWG")
                .steamAppId("570")
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

    @Test
    void search_withTypeBase_addsNullParentIdCriteria() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(GameEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(GameEntity.class))).thenReturn(0L);

        adapter.search(new GameSearchCriteria(null, null, null, null, null, null, GameTypeFilter.BASE), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(GameEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("parentId");
        assertThat(qs).doesNotContain("$ne");
    }

    @Test
    void search_withTypeDlc_addsNotNullParentIdCriteria() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(GameEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(GameEntity.class))).thenReturn(0L);

        adapter.search(new GameSearchCriteria(null, null, null, null, null, null, GameTypeFilter.DLC), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(GameEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("parentId");
        assertThat(qs).contains("$ne");
    }

    @Test
    void search_withTypeAll_addsNoParentIdCriteria() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(GameEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(GameEntity.class))).thenReturn(0L);

        adapter.search(new GameSearchCriteria(null, null, null, null, null, null, GameTypeFilter.ALL), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(GameEntity.class));
        assertThat(queryCaptor.getValue().toString()).doesNotContain("parentId");
    }

    @Test
    void searchGrouped_mapsGroupsAndFacetTotal() {
        Pageable pageable = PageRequest.of(1, 2);
        GameEntity pc = GameEntity.builder().id("g1").externalId("3498").platform("PC").build();
        GameEntity switchRow = GameEntity.builder().id("g2").externalId("3498").platform("Nintendo Switch").build();
        GameGroupAggregationResult group = new GameGroupAggregationResult();
        group.setGameKey("x:3498");
        group.setTitle("Hollow Knight");
        group.setThumbnailUrl("http://thumb");
        group.setGames(List.of(pc, switchRow));
        GameGroupFacetResult facet = new GameGroupFacetResult();
        facet.setContent(List.of(group));
        GameGroupCountResult count = new GameGroupCountResult();
        count.setCount(27L);
        facet.setTotal(List.of(count));
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("games"), eq(GameGroupFacetResult.class)))
                .thenReturn(new AggregationResults<>(List.of(facet), Document.parse("{}")));
        when(mapper.toDomain(pc)).thenReturn(Game.builder().id("g1").platform("PC").build());
        when(mapper.toDomain(switchRow)).thenReturn(Game.builder().id("g2").platform("Nintendo Switch").build());

        Page<GameGroup> result = adapter.searchGrouped(
                new GameSearchCriteria(null, null, null, null, null, null, null, true), pageable);

        assertThat(result.getContent()).hasSize(1);
        GameGroup mapped = result.getContent().get(0);
        assertThat(mapped.gameKey()).isEqualTo("x:3498");
        assertThat(mapped.title()).isEqualTo("Hollow Knight");
        assertThat(mapped.thumbnailUrl()).isEqualTo("http://thumb");
        assertThat(mapped.games()).extracting(Game::getPlatform)
                .containsExactly("PC", "Nintendo Switch");
        // totalElements cuenta grupos (27), no registros.
        assertThat(result.getTotalElements()).isEqualTo(27);
        assertThat(result.getNumber()).isEqualTo(1);
    }

    @Test
    void searchGrouped_buildsMatchKeySortGroupAndFacetPipeline() {
        Pageable pageable = PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "title"));
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("games"), eq(GameGroupFacetResult.class)))
                .thenReturn(new AggregationResults<>(List.of(new GameGroupFacetResult()), Document.parse("{}")));

        adapter.searchGrouped(new GameSearchCriteria(null, List.of("PlayStation 5"), null, null, "u1", null, null,
                true), pageable);

        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongoTemplate).aggregate(captor.capture(), eq("games"), eq(GameGroupFacetResult.class));
        assertThat(captor.getValue()).isNotNull();
    }

    @Test
    void searchGrouped_returnsEmptyPage_whenFacetHasNoGroups() {
        Pageable pageable = PageRequest.of(0, 20);
        GameGroupFacetResult facet = new GameGroupFacetResult();
        facet.setContent(List.of());
        facet.setTotal(List.of());
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("games"), eq(GameGroupFacetResult.class)))
                .thenReturn(new AggregationResults<>(List.of(facet), Document.parse("{}")));

        Page<GameGroup> result = adapter.searchGrouped(
                new GameSearchCriteria(null, null, null, null, null, null, null, true), pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void searchGrouped_returnsEmptyPage_whenAggregationReturnsNoDocument() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("games"), eq(GameGroupFacetResult.class)))
                .thenReturn(new AggregationResults<>(List.of(), Document.parse("{}")));

        Page<GameGroup> result = adapter.searchGrouped(
                new GameSearchCriteria(null, null, null, null, null, null, null, true), pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void deleteByOwnerId_removesInBulk_withoutLoadingDocuments() {
        adapter.deleteByOwnerId("u1");

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).remove(queryCaptor.capture(), eq(GameEntity.class));
        assertThat(queryCaptor.getValue().toString()).contains("ownerId").contains("u1");
    }

    @Test
    void findByOwnerIdAndExternalIdIn_scopesToOwner_andMapsEntities() {
        GameEntity entity = GameEntity.builder().id("g1").externalId("355880").build();
        Game mapped = sampleGame();
        when(springDataGameRepository.findByOwnerIdAndExternalIdIn("u1", List.of("355880")))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(mapped);

        List<Game> games = adapter.findByOwnerIdAndExternalIdIn("u1", List.of("355880"));

        assertThat(games).containsExactly(mapped);
    }

    @Test
    void findByOwnerIdAndExternalIdIn_returnsEmpty_whenNoIdsAreGiven() {
        assertThat(adapter.findByOwnerIdAndExternalIdIn("u1", List.of())).isEmpty();
        assertThat(adapter.findByOwnerIdAndExternalIdIn("u1", null)).isEmpty();
        verifyNoInteractions(springDataGameRepository);
    }

    @Test
    void deleteByOwnerIdAndParentId_removesInBulk_withoutLoadingDocuments() {
        adapter.deleteByOwnerIdAndParentId("u1", "base-1");

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).remove(queryCaptor.capture(), eq(GameEntity.class));
        assertThat(queryCaptor.getValue().toString())
                .contains("ownerId").contains("u1")
                .contains("parentId").contains("base-1");
    }
}
