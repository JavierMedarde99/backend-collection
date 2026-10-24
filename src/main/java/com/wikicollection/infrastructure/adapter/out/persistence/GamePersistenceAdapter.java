package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameGroup;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.port.out.GameRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class GamePersistenceAdapter implements GameRepository {

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

    @Override
    public Page<GameGroup> searchGrouped(GameSearchCriteria criteria, Pageable pageable) {
        List<AggregationOperation> operations = new java.util.ArrayList<>();
        Criteria matchCriteria = buildMatchCriteria(criteria);
        if (matchCriteria != null) {
            operations.add(Aggregation.match(matchCriteria));
        }

        String normalized = normalizedTitleExpression();
        operations.add(Aggregation.addFields()
                .addField("gameKey")
                .withValueOfExpression(
                        "{$cond: [{$and: [{$ne: [\"$externalId\", null]}, {$ne: [\"$externalId\", \"\"]]}, {$concat: [\"x:\", \"$externalId\"]}, {$concat: [\"t:\", " + normalized + "]}]}")
                .build());

        operations.add(Aggregation.sort(Sort.by(Sort.Direction.ASC, "platform").and(Sort.by(Sort.Direction.ASC, "_id"))));

        GroupOperation group = Aggregation.group("gameKey")
                .first("title").as("title")
                .first("thumbnailUrl").as("thumbnailUrl")
                .push("$$ROOT").as("games");
        operations.add(group);

        Sort groupSort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.ASC, "title");
        operations.add(Aggregation.sort(groupSort));

        FacetOperation facet = Aggregation.facet(
                Aggregation.skip(pageable.getOffset()),
                Aggregation.limit(pageable.getPageSize())
        ).as("content").and(
                Aggregation.count().as("count")
        ).as("total");
        operations.add(facet);

        Aggregation aggregation = Aggregation.newAggregation(operations);
        AggregationResults<GameGroupFacetResult> results = mongoTemplate.aggregate(aggregation, "games",
                GameGroupFacetResult.class);
        List<GameGroupFacetResult> list = results.getMappedResults();
        if (list == null || list.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        GameGroupFacetResult facetResult = list.get(0);
        List<GameGroup> content = (facetResult.getContent() == null ? java.util.List.<GameGroupAggregationResult>of() : facetResult.getContent())
                .stream()
                .map((GameGroupAggregationResult r) -> {
                    java.util.List<GameEntity> entities = new java.util.ArrayList<GameEntity>();
                    if (r.getGames() != null) {
                        for (Object e : r.getGames()) {
                            if (e instanceof GameEntity ge) {
                                entities.add(ge);
                            }
                        }
                    }
                    List<Game> games = new java.util.ArrayList<Game>(entities.size());
                    for (int j = 0; j < entities.size(); j++) {
                        games.add(mapper.toDomain(entities.get(j)));
                    }
                    return new GameGroup(r.getGameKey(), r.getTitle(), r.getThumbnailUrl(), games);
                })
                .toList();
        long total = 0;
        if (facetResult.getTotal() != null && !facetResult.getTotal().isEmpty()
                && facetResult.getTotal().get(0).getCount() != null) {
            total = facetResult.getTotal().get(0).getCount();
        }
        return new PageImpl<>(content, pageable, total);
    }

    private Criteria buildMatchCriteria(GameSearchCriteria criteria) {
        List<Criteria> criterias = new java.util.ArrayList<>();
        if (criteria.hasName()) {
            criterias.add(Criteria.where("title").regex(ciPattern(criteria.name())));
        }
        if (criteria.hasPlatforms()) {
            criterias.add(Criteria.where("platform").in(criteria.effectivePlatforms()));
        }
        if (criteria.hasGenres()) {
            Criteria[] anyGenre = criteria.effectiveGenres().stream()
                    .map(g -> Criteria.where("genres").regex(ciPattern(g)))
                    .toArray(Criteria[]::new);
            criterias.add(new Criteria().orOperator(anyGenre));
        }
        if (criteria.status() != null) {
            criterias.add(Criteria.where("status").is(criteria.status()));
        }
        if (criteria.hasOwnerId()) {
            criterias.add(Criteria.where("ownerId").is(criteria.ownerId()));
        }
        if (criteria.hasExcludeOwnerIds()) {
            criterias.add(Criteria.where("ownerId").nin(criteria.excludeOwnerIds()));
        }
        if (criteria.isBaseOnly()) {
            criterias.add(Criteria.where("parentId").is(null));
        } else if (criteria.isDlcOnly()) {
            criterias.add(Criteria.where("parentId").ne(null));
        }
        if (criterias.isEmpty()) {
            return null;
        }
        return new Criteria().andOperator(criterias.toArray(new Criteria[0]));
    }

    private String normalizedTitleExpression() {
        // Apply lowercasing and accent/punctuation removal similar to DeckNameNormalizer
        String expr = "$toLower(\"$title\")";
        // accents
        String[] pairs = {
                "[áàäâã]", "a", "[ÁÀÄÂÃ]", "a",
                "[éèëê]", "e", "[ÉÈËÊ]", "e",
                "[íìïî]", "i", "[ÍÌÏÎ]", "i",
                "[óòöôõ]", "o", "[ÓÒÖÔÕ]", "o",
                "[úùüû]", "u", "[ÚÙÜÛ]", "u",
                "[ñ]", "n", "[Ñ]", "n",
                "[ç]", "c", "[Ç]", "c"
        };
        for (int i = 0; i < pairs.length; i += 2) {
            String regex = pairs[i].replace("[", "\\[").replace("]", "\\]");
            expr = "{$replaceAll: {" + expr + ", \"regex\": \"" + regex + "\", \"replacement\": \"" + pairs[i + 1] + "\"}}";
        }
        // punctuation
        expr = "{$replaceAll: {" + expr + ", \"regex\": \"[^a-z0-9\\\\s]\", \"replacement\": \"\"}}";
        // collapse spaces
        expr = "{$replaceAll: {" + expr + ", \"regex\": \"\\\\s+\", \"replacement\": \" \"}}";
        expr = "{$trim: {" + expr + "}}";
        return expr;
    }

    private GameGroup toGroup(GameGroupAggregationResult result) {
        java.util.List<Game> games = new java.util.ArrayList<Game>();
        if (result.getGames() != null) {
            for (int j = 0; j < result.getGames().size(); j++) {
                Object e = result.getGames().get(j);
                if (e instanceof GameEntity ge) {
                    games.add(mapper.toDomain(ge));
                }
            }
        }
        return new GameGroup(result.getGameKey(), result.getTitle(), result.getThumbnailUrl(), games);
    }


    private Query buildQuery(GameSearchCriteria criteria) {
        Query query = new Query();
        if (criteria.hasName()) {
            query.addCriteria(Criteria.where("title").regex(ciPattern(criteria.name())));
        }
        if (criteria.hasPlatforms()) {
            // Mismo «cualquiera de la lista» que los géneros: platform=A&platform=B.
            query.addCriteria(Criteria.where("platform").in(criteria.effectivePlatforms()));
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
        if (criteria.isBaseOnly()) {
            // Los registros antiguos no tienen parentId: son bases, no contenido adicional.
            query.addCriteria(Criteria.where("parentId").is(null));
        } else if (criteria.isDlcOnly()) {
            query.addCriteria(Criteria.where("parentId").ne(null));
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
    public void deleteByOwnerId(String ownerId) {
        mongoTemplate.remove(new Query(Criteria.where("ownerId").is(ownerId)), GameEntity.class);
    }

    @Override
    public List<Game> findByOwnerIdAndExternalIdIn(String ownerId, List<String> externalIds) {
        if (externalIds == null || externalIds.isEmpty()) {
            return List.of();
        }
        return springDataGameRepository.findByOwnerIdAndExternalIdIn(ownerId, externalIds).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByOwnerIdAndParentId(String ownerId, String parentId) {
        mongoTemplate.remove(
                new Query(Criteria.where("ownerId").is(ownerId).and("parentId").is(parentId)),
                GameEntity.class);
    }

    @Override
    public void updateOwnerName(String ownerId, String ownerName) {
        mongoTemplate.updateMulti(
                new Query(Criteria.where("ownerId").is(ownerId)),
                new Update().set("userOwned.ownerName", ownerName),
                GameEntity.class);
    }
}
