package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Amiibo;
import com.wikicollection.domain.model.AmiiboSearchCriteria;
import com.wikicollection.domain.model.AmiiboStatus;

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
import org.springframework.data.mongodb.core.query.Update;

@ExtendWith(MockitoExtension.class)
class AmiiboPersistenceAdapterTest {

    @Mock
    private SpringDataAmiiboRepository springDataAmiiboRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private AmiiboEntityMapper mapper;

    @InjectMocks
    private AmiiboPersistenceAdapter adapter;

    private Amiibo sample() {
        return Amiibo.builder().id("am1").name("Mario").status(AmiiboStatus.OWNED).build();
    }

    @Test
    void search_withoutFilters_returnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        AmiiboEntity entity = AmiiboEntity.builder().id("am1").name("Mario").build();
        Amiibo expected = sample();
        when(mongoTemplate.find(any(Query.class), eq(AmiiboEntity.class))).thenReturn(List.of(entity));
        when(mongoTemplate.count(any(Query.class), eq(AmiiboEntity.class))).thenReturn(1L);
        when(mapper.toDomain(entity)).thenReturn(expected);

        Page<Amiibo> result = adapter.search(new AmiiboSearchCriteria(null, null, null, null), pageable);

        assertThat(result.getContent()).containsExactly(expected);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_withNameFilter_buildsCaseInsensitiveRegex() {
        when(mongoTemplate.find(any(Query.class), eq(AmiiboEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(AmiiboEntity.class))).thenReturn(0L);

        adapter.search(new AmiiboSearchCriteria("mario", null, null, null), PageRequest.of(0, 20));

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(AmiiboEntity.class));
        String query = captor.getValue().toString();
        assertThat(query).contains("name").contains("$regularExpression").contains("mario");
    }

    @Test
    void search_withStatusFilter_addsExactCriteria() {
        when(mongoTemplate.find(any(Query.class), eq(AmiiboEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(AmiiboEntity.class))).thenReturn(0L);

        adapter.search(new AmiiboSearchCriteria(null, AmiiboStatus.WISHLIST, null, null), PageRequest.of(0, 20));

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(AmiiboEntity.class));
        assertThat(captor.getValue().toString()).contains("status").contains("WISHLIST");
    }

    @Test
    void search_withOwnerId_addsEqualityCriteria() {
        when(mongoTemplate.find(any(Query.class), eq(AmiiboEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(AmiiboEntity.class))).thenReturn(0L);

        adapter.search(new AmiiboSearchCriteria(null, null, "u1", null), PageRequest.of(0, 20));

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(AmiiboEntity.class));
        assertThat(captor.getValue().toString()).contains("ownerId").contains("u1");
    }

    @Test
    void search_withExcludeOwnerIds_addsNinCriteria() {
        when(mongoTemplate.find(any(Query.class), eq(AmiiboEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(AmiiboEntity.class))).thenReturn(0L);

        adapter.search(new AmiiboSearchCriteria(null, null, null, List.of("u2", "u3")), PageRequest.of(0, 20));

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(AmiiboEntity.class));
        String query = captor.getValue().toString();
        assertThat(query).contains("ownerId").contains("$nin").contains("u2").contains("u3");
    }

    @Test
    void findById_mapsEntity_whenExists() {
        AmiiboEntity entity = AmiiboEntity.builder().id("am1").build();
        Amiibo expected = sample();
        when(springDataAmiiboRepository.findById("am1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        assertThat(adapter.findById("am1")).contains(expected);
    }

    @Test
    void findByExternalId_mapsEntity_whenExists() {
        AmiiboEntity entity = AmiiboEntity.builder().id("am1").build();
        Amiibo expected = sample();
        when(springDataAmiiboRepository.findByExternalId("09d0030102bb0e02")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        assertThat(adapter.findByExternalId("09d0030102bb0e02")).contains(expected);
    }

    @Test
    void save_mapsDomainToEntity_andBack() {
        Amiibo amiibo = sample();
        AmiiboEntity entity = AmiiboEntity.builder().id("am1").name("Mario").build();
        when(mapper.toEntity(amiibo)).thenReturn(entity);
        when(springDataAmiiboRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(amiibo);

        assertThat(adapter.save(amiibo)).isSameAs(amiibo);
    }

    @Test
    void deleteById_delegatesToSpringData() {
        adapter.deleteById("am1");

        verify(springDataAmiiboRepository).deleteById("am1");
    }

    @Test
    void updateOwnerName_setsNestedOwnerName() {
        adapter.updateOwnerName("u1", "Javi");

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateMulti(queryCaptor.capture(), updateCaptor.capture(), eq(AmiiboEntity.class));
        assertThat(queryCaptor.getValue().toString()).contains("ownerId").contains("u1");
        assertThat(updateCaptor.getValue().toString()).contains("userOwned.ownerName").contains("Javi");
    }

    @Test
    void deleteByOwnerId_removesInBulk_withoutLoadingDocuments() {
        adapter.deleteByOwnerId("u1");

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).remove(queryCaptor.capture(), eq(AmiiboEntity.class));
        assertThat(queryCaptor.getValue().toString()).contains("ownerId").contains("u1");
    }
}
