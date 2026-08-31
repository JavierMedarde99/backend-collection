package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class BookPersistenceAdapterTest {

    @Mock
    private SpringDataBookRepository springDataBookRepository;

    @Mock
    private BookEntityMapper mapper;

    @InjectMocks
    private BookPersistenceAdapter adapter;

    private Book sampleBook() {
        Book book = new Book();
        book.setId("b1");
        book.setTitle("Cien años de soledad");
        book.setAuthor("Gabriel García Márquez");
        book.setState(BookState.READING);
        return book;
    }

    @Test
    void findAll_mapsEntitiesToDomain() {
        Pageable pageable = PageRequest.of(0, 20);
        BookEntity entity = BookEntity.builder().id("b1").title("Cien años de soledad").build();
        Book expected = sampleBook();
        when(springDataBookRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(mapper.toDomain(entity)).thenReturn(expected);

        Page<Book> result = adapter.findAll(pageable);

        assertThat(result.getContent()).containsExactly(expected);
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
    void findByState_mapsEntity_whenExists() {
        Pageable pageable = PageRequest.of(0, 20);
        BookEntity entity = BookEntity.builder().id("b1").title("Cien años de soledad").state(BookState.READING).build();
        Book expected = sampleBook();
        when(springDataBookRepository.findByState(BookState.READING, pageable)).thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(mapper.toDomain(entity)).thenReturn(expected);

        Page<Book> result = adapter.findByState(BookState.READING, pageable);

        assertThat(result.getContent()).containsExactly(expected);
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
}
