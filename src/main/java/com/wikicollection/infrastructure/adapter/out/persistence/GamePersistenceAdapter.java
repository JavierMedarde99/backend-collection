package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameGroup;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.port.out.GameRepository;

import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class GamePersistenceAdapter implements GameRepository {

    private static final String GAMES_COLLECTION = "games";
    private static final String GAME_KEY_FIELD = "gameKey";
    private static final Map<String, String> ACCENT_FOLDING = accentFolding();

    private final SpringDataGameRepository springDataGameRepository;
    private final MongoTemplate mongoTemplate;
    private final GameEntityMapper mapper;

    public GamePersistenceAdapter(SpringDataGameRepository springDataGameRepository,
                                  MongoTemplate mongoTemplate,
                                  GameEntityMapper mapper) {
        this.springDataGameRepository = springDataGameRepository;
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public List<String> distinctGenres(List<String> excludeOwnerIds) {
        var distinct = mongoTemplate.query(GameEntity.class).distinct("genres").as(String.class);
        if (excludeOwnerIds != null && !excludeOwnerIds.isEmpty()) {
            return distinct.matching(new Query(Criteria.where("ownerId").nin(excludeOwnerIds))).all();
        }
        return distinct.all();
    }

    @Override
    public Page<Game> search(GameSearchCriteria criteria, Pageable pageable) {
        Query query = buildQuery(criteria);
        long total = mongoTemplate.count(query, GameEntity.class);
        query.with(pageable);
        List<Game> content = mongoTemplate.find(query, GameEntity.class).stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Listado agrupado por juego (issue #516). Los filtros se aplican en el {@code $match}
     * antes del {@code $group}, así que un filtro por plataforma deja el grupo con una sola
     * fila. La página se corta por grupos dentro del {@code $facet}.
     *
     * <p>La {@code gameKey} se construye con documentos BSON crudos ({@link Document}), no con
     * {@code withValueOfExpression}: Spring Data interpreta esos strings como SpEL y la
     * expresión de Mongo ({@code $cond}/{@code $concat}) no es SpEL válido.
     */
    @Override
    public Page<GameGroup> searchGrouped(GameSearchCriteria criteria, Pageable pageable) {
        Aggregation aggregation = Aggregation.newAggregation(
                stage(new Document("$match", buildQuery(criteria).getQueryObject())),
                stage(new Document("$addFields", new Document(GAME_KEY_FIELD, gameKeyExpression()))),
                stage(new Document("$sort", new Document("platform", 1))),
                stage(new Document("$group", new Document("_id", "$" + GAME_KEY_FIELD)
                        .append("title", new Document("$first", "$title"))
                        .append("thumbnailUrl", new Document("$first", "$thumbnailUrl"))
                        .append("games", new Document("$push", "$$ROOT")))),
                stage(new Document("$sort",
                        new Document("title", groupSortDirection(pageable)).append("_id", 1))),
                stage(new Document("$facet",
                        new Document("content", List.of(
                                new Document("$skip", pageable.getOffset()),
                                new Document("$limit", (long) pageable.getPageSize())))
                                .append("total", List.of(new Document("$count", "count"))))));

        AggregationResults<GameGroupFacetResult> results =
                mongoTemplate.aggregate(aggregation, GAMES_COLLECTION, GameGroupFacetResult.class);
        GameGroupFacetResult facet = results.getUniqueMappedResult();
        List<GameGroup> content = facet == null || facet.getContent() == null ? List.of()
                : facet.getContent().stream()
                        .filter(Objects::nonNull)
                        .map(this::toGameGroup)
                        .toList();
        return new PageImpl<>(content, pageable, facetTotal(facet));
    }

    private GameGroup toGameGroup(GameGroupAggregationResult result) {
        List<Game> games = result.getGames() == null ? List.of()
                : result.getGames().stream()
                        .filter(Objects::nonNull)
                        .map(mapper::toDomain)
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(Game::getPlatform,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList();
        String title = games.isEmpty() ? result.getTitle() : games.get(0).getTitle();
        String thumbnailUrl = games.stream()
                .map(Game::getThumbnailUrl)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(result.getThumbnailUrl());
        return new GameGroup(result.getGameKey(), title, thumbnailUrl, games);
    }

    private long facetTotal(GameGroupFacetResult facet) {
        if (facet == null || facet.getTotal() == null || facet.getTotal().isEmpty()
                || facet.getTotal().get(0) == null || facet.getTotal().get(0).getCount() == null) {
            return 0L;
        }
        return facet.getTotal().get(0).getCount();
    }

    private int groupSortDirection(Pageable pageable) {
        Sort sort = pageable.getSort();
        for (Sort.Order order : sort) {
            if ("title".equalsIgnoreCase(order.getProperty())) {
                return order.isDescending() ? -1 : 1;
            }
        }
        return 1;
    }

    /**
     * {@code "x:" + externalId} si el registro viene del catálogo; si no,
     * {@code "t:" + título normalizado} (minúsculas, sin acentos, sin puntuación y con los
     * espacios colapsados).
     */
    private Document gameKeyExpression() {
        Document externalIdKey = new Document("$concat", List.of("x:", "$externalId"));
        Document titleKey = new Document("$concat", List.of("t:", normalizedTitleExpression()));
        return new Document("$cond", List.of(
                new Document("$ne", List.of(new Document("$ifNull", List.of("$externalId", "")), "")),
                externalIdKey,
                titleKey));
    }

    private Document normalizedTitleExpression() {
        Object lowered = new Document("$toLower", "$title");
        for (Map.Entry<String, String> folding : ACCENT_FOLDING.entrySet()) {
            lowered = new Document("$replaceAll", new Document("input", lowered)
                    .append("find", folding.getKey())
                    .append("replacement", folding.getValue()));
        }
        // Quita cualquier carácter que no sea [a-z0-9 ] (puntuación incluida).
        Object trimmedPunctuation = new Document("$reduce", new Document("input",
                new Document("$regexFindAll", new Document("input", lowered).append("regex", "[^a-z0-9 ]")))
                .append("initialValue", lowered)
                .append("in", new Document("$replaceAll", new Document("input", "$$value")
                        .append("find", "$$this.match")
                        .append("replacement", ""))));

        // Colapsa los espacios repetidos: split -> descarta vacíos -> reduce con " ".
        return new Document("$reduce", new Document("input", new Document("$filter",
                new Document("input", new Document("$split", List.of(trimmedPunctuation, " ")))
                        .append("as", "token")
                        .append("cond", new Document("$ne", List.of("$$token", "")))))
                .append("initialValue", "")
                .append("in", new Document("$cond", List.of(
                        new Document("$eq", List.of("$$value", "")),
                        "$$this",
                        new Document("$concat", List.of("$$value", " ", "$$this"))))));
    }

    private static Map<String, String> accentFolding() {
        Map<String, String> folding = new LinkedHashMap<>();
        folding.put("á", "a");
        folding.put("é", "e");
        folding.put("í", "i");
        folding.put("ó", "o");
        folding.put("ú", "u");
        folding.put("ü", "u");
        folding.put("ñ", "n");
        folding.put("à", "a");
        folding.put("è", "e");
        folding.put("ì", "i");
        folding.put("ò", "o");
        folding.put("ù", "u");
        folding.put("â", "a");
        folding.put("ê", "e");
        folding.put("î", "i");
        folding.put("ô", "o");
        folding.put("û", "u");
        folding.put("ä", "a");
        folding.put("ë", "e");
        folding.put("ï", "i");
        folding.put("ö", "o");
        folding.put("ç", "c");
        return Collections.unmodifiableMap(folding);
    }

    private static AggregationOperation stage(Document document) {
        return context -> document;
    }

    private Query buildQuery(GameSearchCriteria criteria) {
        Query query = new Query();
        if (criteria.hasName()) {
            query.addCriteria(Criteria.where("title").regex(ciPattern(criteria.name())));
        }
        if (criteria.platform() != null) {
            query.addCriteria(Criteria.where("platform").is(criteria.platform()));
        }
        if (criteria.hasGenres()) {
            Criteria[] anyGenre = criteria.effectiveGenres().stream()
                    .map(g -> Criteria.where("genres").regex(ciPattern(g)))
                    .toArray(Criteria[]::new);
            query.addCriteria(new Criteria().orOperator(anyGenre));
        }
        if (criteria.status() != null) {
            query.addCriteria(Criteria.where("status").is(criteria.status()));
        }
        if (criteria.hasOwnerId()) {
            query.addCriteria(Criteria.where("ownerId").is(criteria.ownerId()));
        }
        if (criteria.hasExcludeOwnerIds()) {
            query.addCriteria(Criteria.where("ownerId").nin(criteria.excludeOwnerIds()));
        }
        return query;
    }

    private Pattern ciPattern(String value) {
        return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Override
    public Optional<Game> findById(String id) {
        return springDataGameRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Game save(Game game) {
        GameEntity saved = springDataGameRepository.save(mapper.toEntity(game));
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(String id) {
        springDataGameRepository.deleteById(id);
    }

    @Override
    public void updateOwnerName(String ownerId, String ownerName) {
        mongoTemplate.updateMulti(
                new Query(Criteria.where("ownerId").is(ownerId)),
                new Update().set("userOwned.ownerName", ownerName),
                GameEntity.class);
    }

    @Override
    public List<Game> findByOwnerIdAndExternalIdIn(String ownerId, List<String> externalIds) {
        if (externalIds == null || externalIds.isEmpty()) return List.of();
        return springDataGameRepository.findByOwnerIdAndExternalIdIn(ownerId, externalIds).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public void deleteByOwnerIdAndParentId(String ownerId, String parentId) {
        mongoTemplate.remove(new Query(Criteria.where("ownerId").is(ownerId).and("parentId").is(parentId)), GameEntity.class);
    }

    @Override
    public void deleteByOwnerId(String ownerId) {
        mongoTemplate.remove(new Query(Criteria.where("ownerId").is(ownerId)), GameEntity.class);
    }

}
