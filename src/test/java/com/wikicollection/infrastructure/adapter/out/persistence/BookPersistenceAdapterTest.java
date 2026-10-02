package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookSearchCriteria;
import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.ExecutableFindOperation;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class BookPersistenceAdapterTest {

    @Mock
    private SpringDataBookRepository springDataBookRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private BookEntityMapper mapper;

    @Mock
    private ExecutableFindOperation.ExecutableFind<BookEntity> findOperation;

    /** distinct("series") devuelve TerminatingDistinct<Object> antes de proyectar. */
    @Mock
    @SuppressWarnings("rawtypes")
    private ExecutableFindOperation.TerminatingDistinct distinctUnprojected;

    @Mock
    private ExecutableFindOperation.TerminatingDistinct<String> distinctResult;

    @InjectMocks
    private BookPersistenceAdapter adapter;

    private Book sampleBook() {
        Book book = new Book();
        book.setId("b1");
        book.setTitle("Cien años de soledad");
        book.setAuthor("Gabriel García Márquez");
        book.setState(BookState.READING);
        book.setType(BookType.NOVEL);
        return book;
    }

    @Test
    void findById_mapsEntity_whenExists() {
        BookEntity entity = BookEntity.builder().id("b1").title("Cien años de soledad").build();
        Book expected = sampleBook();
        when(springDataBookRepository.findById("b1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        Optional<Book> result = adapter.findById("b1");

        assertThat(result).contains(expected);
    }

    @Test
    void findByExternalId_mapsEntity_whenExists() {
        BookEntity entity = BookEntity.builder().id("b1").externalId("gb123").title("Cien años de soledad").build();
        Book expected = sampleBook();
        when(springDataBookRepository.findByExternalId("gb123")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        Optional<Book> result = adapter.findByExternalId("gb123");

        assertThat(result).contains(expected);
    }

    @Test
    void findByExternalId_returnsEmpty_whenMissing() {
        when(springDataBookRepository.findByExternalId("gb999")).thenReturn(Optional.empty());

        Optional<Book> result = adapter.findByExternalId("gb999");

        assertThat(result).isEmpty();
        verify(springDataBookRepository).findByExternalId("gb999");
    }

    @Test
    void save_mapsDomainToEntity_andBack() {
        Book book = sampleBook();
        BookEntity entity = BookEntity.builder().id("b1").title("Cien años de soledad").build();
        when(mapper.toEntity(book)).thenReturn(entity);
        when(springDataBookRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(book);

        Book result = adapter.save(book);

        assertThat(result).isSameAs(book);
    }

    @Test
    void deleteById_delegatesToSpringData() {
        adapter.deleteById("b1");

        verify(springDataBookRepository).deleteById("b1");
    }

    @Test
    void distinctSeries_queriesDistinctSeries() {
        stubDistinct(List.of("Harry Potter"));

        List<String> result = adapter.distinctSeries(List.of());

        assertThat(result).containsExactly("Harry Potter");
        verify(mongoTemplate).query(BookEntity.class);
        verify(findOperation).distinct("series");
    }

    @Test
    void distinctSeries_excludesPrivateOwners() {
        stubDistinct(List.of());

        adapter.distinctSeries(List.of("oculto"));

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(distinctResult).matching(queryCaptor.capture());
        assertThat(queryCaptor.getValue().toString()).contains("ownerId").contains("oculto");
    }

    @Test
    void distinctSeries_treatsNullExclusionsAsNoFilter() {
        stubDistinct(List.of());

        adapter.distinctSeries(null);

        verify(distinctResult, never()).matching(any(Query.class));
    }

    /** Encadena mongoTemplate.query(..).distinct("series").as(String.class). */
    private void stubDistinct(List<String> values) {
        lenient().when(mongoTemplate.query(BookEntity.class)).thenReturn(findOperation);
        lenient().when(findOperation.distinct("series")).thenReturn(distinctUnprojected);
        lenient().when(distinctUnprojected.as(String.class)).thenReturn(distinctResult);
        lenient().when(distinctResult.matching(any(Query.class))).thenReturn(distinctResult);
        lenient().when(distinctResult.all()).thenReturn(values);
    }

    @Test
    void search_withoutFilters_returnsAllResults() {
        Pageable pageable = PageRequest.of(0, 20);
        BookEntity entity = BookEntity.builder().id("b1").title("Cien años de soledad").build();
        Book expected = sampleBook();
        when(mongoTemplate.find(any(Query.class), eq(BookEntity.class))).thenReturn(List.of(entity));
        when(mongoTemplate.count(any(Query.class), eq(BookEntity.class))).thenReturn(1L);
        when(mapper.toDomain(entity)).thenReturn(expected);

        Page<Book> result = adapter.search(new BookSearchCriteria(null, null, null, null, null, null, null), pageable);

        assertThat(result.getContent()).containsExactly(expected);
    }

    @Test
    void search_withNameFilter_buildsCaseInsensitiveRegexOnTitle() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(BookEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(BookEntity.class))).thenReturn(0L);

        adapter.search(new BookSearchCriteria("gar", null, null, null, null, null, null), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(BookEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("title");
        assertThat(qs).contains("$regularExpression");
        assertThat(qs).contains("gar");
    }

    @Test
    void search_withAuthorFilter_buildsCaseInsensitiveRegexOnAuthor() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(BookEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(BookEntity.class))).thenReturn(0L);

        adapter.search(new BookSearchCriteria(null, "garcía", null, null, null, null, null), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(BookEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("author");
        assertThat(qs).contains("$regularExpression");
        assertThat(qs).contains("garcía");
    }

    @Test
    void search_withGenreFilter_buildsCaseInsensitiveRegexOnGenres() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(BookEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(BookEntity.class))).thenReturn(0L);

        adapter.search(new BookSearchCriteria(null, null, null, null, java.util.List.of("fantasía", "terror"), null, null), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(BookEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("genres");
        assertThat(qs).contains("$or");
        assertThat(qs).contains("$regularExpression");
        assertThat(qs).contains("fantasía");
        assertThat(qs).contains("terror");
    }

    @Test
    void search_withSeriesFilter_buildsCaseInsensitiveRegexOnSeries() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(BookEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(BookEntity.class))).thenReturn(0L);

        adapter.search(new BookSearchCriteria(null, null, null, null, null, null, null, "potter"), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(BookEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("series");
        assertThat(qs).contains("$regularExpression");
        assertThat(qs).contains("potter");
    }

    @Test
    void search_withBlankSeriesFilter_doesNotFilter() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(BookEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(BookEntity.class))).thenReturn(0L);

        adapter.search(new BookSearchCriteria(null, null, null, null, null, null, null, "  "), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(BookEntity.class));
        assertThat(queryCaptor.getValue().getQueryObject()).doesNotContainKey("series");
    }

    @Test
    void search_withTypeFilter_addsExactCriteria() {
        Pageable pageable = PageRequest.of(0, 20);
        when(mongoTemplate.find(any(Query.class), eq(BookEntity.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(Query.class), eq(BookEntity.class))).thenReturn(0L);

        adapter.search(new BookSearchCriteria(null, null, BookType.NOVEL, BookState.READING, null, null, null), pageable);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(BookEntity.class));
        String qs = queryCaptor.getValue().toString();
        assertThat(qs).contains("type");
        assertThat(qs).contains("state");
    }
}
