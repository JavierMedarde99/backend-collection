package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardCondition;
import com.wikicollection.domain.model.MagicCardLanguage;
import com.wikicollection.domain.model.MagicCardSearchCriteria;

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
class MagicCardPersistenceAdapterTest {

    @Mock
    private SpringDataMagicCardRepository springDataMagicCardRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private MagicCardEntityMapper mapper;

    @InjectMocks
    private MagicCardPersistenceAdapter adapter;

    private MagicCard sampleCard() {
        return MagicCard.builder()
                .id("mc1")
                .name("Lightning Bolt")
                .rarity("uncommon")
                .build();
    }

    @Test
    void search_withoutFilters_returnsAllResults() {
        Pageable pageable = PageRequest.of(0, 20);
        MagicCardEntity entity = MagicCardEntity.builder().id("mc1").name("Lightning Bolt").build();
        MagicCard expected = sampleCard();
        when(mongoTemplate.find(any(Query.class), eq(MagicCardEntity.class))).thenReturn(List.of(entity));
        when(mongoTemplate.count(any(Query.class), eq(MagicCardEntity.class))).thenReturn(1L);
        when(mapper.toDomain(entity)).thenReturn(expected);

        Page<MagicCard> result = adapter.search(new MagicCardSearchCriteria(null, null, null, null), pageable);

        assertThat(result.getContent()).containsExactly(expected);
    }

    @Test
    void search_withNameFilter_buildsCaseInsensitiveRegexOnName() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(MagicCardEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(MagicCardEntity.class))).thenReturn(0L);

        adapter.search(new MagicCardSearchCriteria("lightning", null, null, null), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(MagicCardEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("name");
        assertThat(qs).contains("$regularExpression");
        assertThat(qs).contains("lightning");
    }

    @Test
    void search_withAllFilters_buildsQueryCorrectly() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(MagicCardEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(MagicCardEntity.class))).thenReturn(0L);

        adapter.search(new MagicCardSearchCriteria("bolt", "rare", "R", "Instant"), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(MagicCardEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("name");
        assertThat(qs).contains("rarity");
        assertThat(qs).contains("colors");
        assertThat(qs).contains("type");
        assertThat(qs).doesNotContain("convertedManaCost");
    }

    @Test
    void findById_mapsEntity_whenExists() {
        MagicCardEntity entity = MagicCardEntity.builder().id("mc1").name("Lightning Bolt").build();
        MagicCard expected = sampleCard();
        when(springDataMagicCardRepository.findById("mc1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        Optional<MagicCard> result = adapter.findById("mc1");

        assertThat(result).contains(expected);
    }

    @Test
    void findById_returnsEmpty_whenMissing() {
        when(springDataMagicCardRepository.findById("nope")).thenReturn(Optional.empty());

        Optional<MagicCard> result = adapter.findById("nope");

        assertThat(result).isEmpty();
    }

    @Test
    void save_mapsDomainToEntity_andBack() {
        MagicCard card = sampleCard();
        MagicCardEntity entity = MagicCardEntity.builder().id("mc1").name("Lightning Bolt").build();
        when(mapper.toEntity(card)).thenReturn(entity);
        when(springDataMagicCardRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(card);

        MagicCard result = adapter.save(card);

        assertThat(result).isSameAs(card);
    }

    @Test
    void deleteById_delegatesToSpringData() {
        adapter.deleteById("mc1");

        verify(springDataMagicCardRepository).deleteById("mc1");
    }

    @Test
    void mapper_roundTripsAllFields() {
        MagicCard card = MagicCard.builder()
                .id("mc1")
                .scryfallId("s1")
                .oracleId("o1")
                .name("Lightning Bolt")
                .language(MagicCardLanguage.ENGLISH)
                .releaseDate("2026-06-26")
                .manaCost("{R}")
                .convertedManaCost(1.0)
                .type("Instant")
                .text("Lightning Bolt deals 3 damage to any target.")
                .power("1")
                .toughness("1")
                .loyalty("2")
                .colors(List.of("R"))
                .colorIdentity(List.of("R"))
                .keywords(List.of("Flash"))
                .rarity("uncommon")
                .setCode("msc")
                .setName("Marvel Super Heroes Commander")
                .artist("Milivoj")
                .frame("2015")
                .borderColor("black")
                .layout("normal")
                .legalities(java.util.Map.of("standard", "legal"))
                .priceUsd("0.65")
                .priceEur("2.02")
                .imageUrl("http://img")
                .imageLargeUrl("http://large")
                .artCropUrl("http://artcrop")
                .condition(MagicCardCondition.NEAR_MINT)
                .isFoil(true)
                .quantity(3)
                .notes("Mi favorita")
                .dateAdded(LocalDateTime.of(2026, 1, 1, 12, 0))
                .build();

        MagicCard domain = new MagicCardEntityMapper().toDomain(new MagicCardEntityMapper().toEntity(card));

        assertThat(domain).usingRecursiveComparison().isEqualTo(card);
    }

    @Test
    void mapper_handlesNull() {
        MagicCardEntityMapper cardMapper = new MagicCardEntityMapper();

        assertThat(cardMapper.toEntity(null)).isNull();
        assertThat(cardMapper.toDomain(null)).isNull();
    }
}
