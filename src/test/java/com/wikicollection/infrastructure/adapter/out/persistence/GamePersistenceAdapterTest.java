package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameGroup;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.model.GameStatus;

import org.bson.Document;

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
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.Sort;

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

        Page<Game> result = adapter.search(new GameSearchCriteria(null, null, null, null, null, null), pageable);

        assertThat(result.getContent()).containsExactly(expected);
    }

    @Test
    void search_withNameFilter_buildsCaseInsensitiveRegexOnTitle() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(GameEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(GameEntity.class))).thenReturn(0L);

        adapter.search(new GameSearchCriteria("witc", null, null, null, null, null), pageable);

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

        adapter.search(new GameSearchCriteria(null, "PC", GameStatus.PLAYING, null, null, null), pageable);

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
    void searchGrouped_buildsMatchAddFieldsSortGroupAndFacetPipeline() {
        Pageable pageable = PageRequest.of(2, 12, Sort.by(Sort.Direction.DESC, "title"));
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("games"), eq(GameGroupFacetResult.class)))
                .thenReturn(new AggregationResults<>(List.of(new GameGroupFacetResult()), new Document()));

        adapter.searchGrouped(
                new GameSearchCriteria(null, "Nintendo Switch", null, null, null, null, true), pageable);

        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongoTemplate).aggregate(captor.capture(), eq("games"), eq(GameGroupFacetResult.class));
        List<Document> pipeline = captor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);

        assertThat(pipeline).hasSize(6);
        assertThat(pipeline.get(0).get("$match", Document.class))
                .containsEntry("platform", "Nintendo Switch");

        // La clave se calcula con un documento BSON crudo: nunca un String evaluado por SpEL.
        Document gameKey = pipeline.get(1).get("$addFields", Document.class).get("gameKey", Document.class);
        assertThat(gameKey).containsKey("$cond");
        assertThat(gameKey.toJson()).contains("$toLower").contains("$replaceAll").contains("x:").contains("t:");

        assertThat(pipeline.get(2).get("$sort", Document.class)).isEqualTo(new Document("platform", 1));
        assertThat(pipeline.get(3).get("$group", Document.class))
                .containsEntry("_id", "$gameKey")
                .containsKey("games");
        assertThat(pipeline.get(4).get("$sort", Document.class)).containsEntry("title", -1);

        Document facet = pipeline.get(5).get("$facet", Document.class);
        assertThat(facet.getList("content", Document.class))
                .containsExactly(new Document("$skip", 24L), new Document("$limit", 12L));
        assertThat(facet.getList("total", Document.class))
                .containsExactly(new Document("$count", "count"));
    }

    @Test
    void searchGrouped_mapsGroups_sortingRowsByPlatform() {
        GameEntity pcEntity = GameEntity.builder().id("g1").externalId("3498").title("Hollow Knight")
                .platform("PC (Windows)").thumbnailUrl("pc.jpg").build();
        GameEntity switchEntity = GameEntity.builder().id("g2").externalId("3498").title("Hollow Knight")
                .platform("Nintendo Switch").build();
        Game pcGame = Game.builder().id("g1").title("Hollow Knight").platform("PC (Windows)")
                .thumbnailUrl("pc.jpg").build();
        Game switchGame = Game.builder().id("g2").title("Hollow Knight").platform("Nintendo Switch").build();
        when(mapper.toDomain(pcEntity)).thenReturn(pcGame);
        when(mapper.toDomain(switchEntity)).thenReturn(switchGame);

        GameGroupAggregationResult groupResult = new GameGroupAggregationResult();
        groupResult.setGameKey("x:3498");
        groupResult.setTitle("Hollow Knight");
        groupResult.setGames(List.of(pcEntity, switchEntity));

        GameGroupCountResult count = new GameGroupCountResult();
        count.setCount(5L);
        GameGroupFacetResult facet = new GameGroupFacetResult();
        facet.setContent(List.of(groupResult));
        facet.setTotal(List.of(count));
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("games"), eq(GameGroupFacetResult.class)))
                .thenReturn(new AggregationResults<>(List.of(facet), new Document()));

        Page<GameGroup> page = adapter.searchGrouped(
                new GameSearchCriteria(null, null, null, null, null, null, true), PageRequest.of(1, 2));

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(1);
        GameGroup group = page.getContent().get(0);
        assertThat(group.gameKey()).isEqualTo("x:3498");
        assertThat(group.title()).isEqualTo("Hollow Knight");
        assertThat(group.thumbnailUrl()).isEqualTo("pc.jpg");
        assertThat(group.games()).containsExactly(switchGame, pcGame);
    }

    @Test
    void searchGrouped_returnsEmptyPage_whenFacetHasNoContent() {
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("games"), eq(GameGroupFacetResult.class)))
                .thenReturn(new AggregationResults<>(List.of(new GameGroupFacetResult()), new Document()));

        Page<GameGroup> page = adapter.searchGrouped(
                new GameSearchCriteria(null, null, null, null, null, null, true), PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }
}
