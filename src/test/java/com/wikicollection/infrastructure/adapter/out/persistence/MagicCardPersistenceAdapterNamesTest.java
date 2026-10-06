package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

/**
 * Nombres de la colección de un dueño en una sola consulta.
 *
 * <p>La importación de mazos (#353) necesita saber, para cada carta del mazo, si el dueño ya
 * la tiene. Hacerlo carta a carta con una búsqueda por nombre compila un regex sobre
 * {@code name}, que no está indexado ({@code auto-index-creation=false} y
 * {@code MongoIndexMigration} solo crea el índice de {@code ownerId}): un mazo de 99 cartas
 * serían 99 escaneos. Aquí sale la proyección en una consulta con el índice que sí existe.
 */
@ExtendWith(MockitoExtension.class)
class MagicCardPersistenceAdapterNamesTest {

    @Mock
    private SpringDataMagicCardRepository springDataMagicCardRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private MagicCardEntityMapper mapper;

    @InjectMocks
    private MagicCardPersistenceAdapter adapter;

    @Test
    void findNamesByOwnerId_returnsDistinctNames() {
        when(mongoTemplate.find(any(Query.class), eq(MagicCardEntity.class))).thenReturn(List.of(
                entity("Sol Ring"),
                entity("Sol Ring"),
                entity("Plains"),
                entity("  "),
                entity(null)));

        List<String> names = adapter.findNamesByOwnerId("user-1");

        assertThat(names).containsExactly("Sol Ring", "Plains");
    }

    @Test
    void findNamesByOwnerId_filtersByOwnerProjectingOnlyTheName() {
        when(mongoTemplate.find(any(Query.class), eq(MagicCardEntity.class))).thenReturn(List.of());

        adapter.findNamesByOwnerId("user-1");

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(MagicCardEntity.class));

        Query query = captor.getValue();
        assertThat(query.getQueryObject()).containsEntry("ownerId", "user-1");
        assertThat(query.fields().getFieldsObject()).containsOnlyKeys("name");
    }

    @Test
    void findNamesByOwnerId_capsTheNumberOfNames() {
        when(mongoTemplate.find(any(Query.class), eq(MagicCardEntity.class))).thenReturn(List.of());

        adapter.findNamesByOwnerId("user-1");

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(MagicCardEntity.class));

        assertThat(captor.getValue().getLimit()).isEqualTo(5000);
    }

    @Test
    void findNamesByOwnerId_returnsEmpty_whenOwnerHasNoCards() {
        when(mongoTemplate.find(any(Query.class), eq(MagicCardEntity.class))).thenReturn(List.of());

        assertThat(adapter.findNamesByOwnerId("user-1")).isEmpty();
    }

    @Test
    void findNamesByOwnerId_returnsEmpty_withoutQuerying_whenOwnerIdIsBlank() {
        assertThat(adapter.findNamesByOwnerId("  ")).isEmpty();
        assertThat(adapter.findNamesByOwnerId(null)).isEmpty();
        verify(mongoTemplate, never()).find(any(Query.class), eq(MagicCardEntity.class));
    }

    private MagicCardEntity entity(String name) {
        return MagicCardEntity.builder().ownerId("user-1").name(name).build();
    }
}