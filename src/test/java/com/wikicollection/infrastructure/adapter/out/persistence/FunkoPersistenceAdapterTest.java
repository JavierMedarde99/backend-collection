package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Funko;
import com.wikicollection.domain.model.FunkoSearchCriteria;
import com.wikicollection.domain.model.FunkoStatus;

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
class FunkoPersistenceAdapterTest {

    @Mock
    private SpringDataFunkoRepository springDataFunkoRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private FunkoEntityMapper mapper;

    @InjectMocks
    private FunkoPersistenceAdapter adapter;

    private Funko sample() {
        return Funko.builder().id("fk1").name("Batman").status(FunkoStatus.OWNED).build();
    }

    @Test
    void search_withoutFilters_returnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        FunkoEntity entity = FunkoEntity.builder().id("fk1").name("Batman").build();
        Funko expected = sample();
        when(mongoTemplate.find(any(Query.class), eq(FunkoEntity.class))).thenReturn(List.of(entity));
        when(mongoTemplate.count(any(Query.class), eq(FunkoEntity.class))).thenReturn(1L);
        when(mapper.toDomain(entity)).thenReturn(expected);

        Page<Funko> result = adapter.search(new FunkoSearchCriteria(null, null, null, null), pageable);

        assertThat(result.getContent()).containsExactly(expected);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_withNameFilter_buildsCaseInsensitiveRegex() {
        when(mongoTemplate.find(any(Query.class), eq(FunkoEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(FunkoEntity.class))).thenReturn(0L);

        adapter.search(new FunkoSearchCriteria("batman", null, null, null), PageRequest.of(0, 20));

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(FunkoEntity.class));
        String query = captor.getValue().toString();
        assertThat(query).contains("name").contains("$regularExpression").contains("batman");
    }

    @Test
    void search_withStatusFilter_addsExactCriteria() {
        when(mongoTemplate.find(any(Query.class), eq(FunkoEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(FunkoEntity.class))).thenReturn(0L);

        adapter.search(new FunkoSearchCriteria(null, FunkoStatus.WISHLIST, null, null), PageRequest.of(0, 20));

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(FunkoEntity.class));
        assertThat(captor.getValue().toString()).contains("status").contains("WISHLIST");
    }

    @Test
    void search_withOwnerId_addsEqualityCriteria() {
        when(mongoTemplate.find(any(Query.class), eq(FunkoEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(FunkoEntity.class))).thenReturn(0L);

        adapter.search(new FunkoSearchCriteria(null, null, "u1", null), PageRequest.of(0, 20));

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(FunkoEntity.class));
        assertThat(captor.getValue().toString()).contains("ownerId").contains("u1");
    }

    @Test
    void search_withExcludeOwnerIds_addsNinCriteria() {
        when(mongoTemplate.find(any(Query.class), eq(FunkoEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(FunkoEntity.class))).thenReturn(0L);

        adapter.search(new FunkoSearchCriteria(null, null, null, List.of("u2", "u3")), PageRequest.of(0, 20));

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(FunkoEntity.class));
        String query = captor.getValue().toString();
        assertThat(query).contains("ownerId").contains("$nin").contains("u2").contains("u3");
    }

    @Test
    void findById_mapsEntity_whenExists() {
        FunkoEntity entity = FunkoEntity.builder().id("fk1").build();
        Funko expected = sample();
        when(springDataFunkoRepository.findById("fk1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        assertThat(adapter.findById("fk1")).contains(expected);
    }

    @Test
    void findByExternalId_mapsEntity_whenExists() {
        FunkoEntity entity = FunkoEntity.builder().id("fk1").build();
        Funko expected = sample();
        when(springDataFunkoRepository.findByExternalId("44")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        assertThat(adapter.findByExternalId("44")).contains(expected);
    }

    @Test
    void save_mapsDomainToEntity_andBack() {
        Funko funko = sample();
        FunkoEntity entity = FunkoEntity.builder().id("fk1").name("Batman").build();
        when(mapper.toEntity(funko)).thenReturn(entity);
        when(springDataFunkoRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(funko);

        assertThat(adapter.save(funko)).isSameAs(funko);
    }

    @Test
    void deleteById_delegatesToSpringData() {
        adapter.deleteById("fk1");

        verify(springDataFunkoRepository).deleteById("fk1");
    }

    @Test
    void updateOwnerName_setsNestedOwnerName() {
        adapter.updateOwnerName("u1", "Javi");

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateMulti(queryCaptor.capture(), updateCaptor.capture(), eq(FunkoEntity.class));
        assertThat(queryCaptor.getValue().toString()).contains("ownerId").contains("u1");
        assertThat(updateCaptor.getValue().toString()).contains("userOwned.ownerName").contains("Javi");
    }
}
