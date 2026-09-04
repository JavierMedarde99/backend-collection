package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.wikicollection.application.exception.BookNotFoundException;
import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookSearchCriteria;
import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.port.out.BookRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book sampleBook() {
        Book book = new Book();
        book.setTitle("Cien años de soledad");
        book.setAuthor("Gabriel García Márquez");
        book.setState(BookState.TO_READ);
        return book;
    }

    @Test
    void findAll_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        BookSearchCriteria criteria = new BookSearchCriteria(null, null, null, null);
        when(bookRepository.search(criteria, pageable)).thenReturn(Page.empty());

        Page<Book> result = bookService.search(criteria, pageable);

        assertThat(result).isEmpty();
        verify(bookRepository).search(criteria, pageable);
    }

    @Test
    void findById_returnsBook_whenExists() {
        Book book = sampleBook();
        book.setId("b1");
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));

        Book result = bookService.findById("b1");

        assertThat(result).isSameAs(book);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(bookRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById("nope"))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void save_delegatesToRepository() {
        Book book = sampleBook();
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book result = bookService.save(book);

        assertThat(result).isSameAs(book);
        verify(bookRepository).save(book);
    }

    @Test
    void update_throwsNotFound_whenMissing() {
        when(bookRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update("nope", sampleBook()))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void update_appliesFieldsAndKeepsId() {
        Book existing = sampleBook();
        existing.setId("b1");
        Book updates = sampleBook();
        updates.setTitle("Nuevo título");
        updates.setAuthor("Otro autor");
        updates.setPages(300);
        updates.setState(BookState.COMPLETED);
        when(bookRepository.findById("b1")).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book result = bookService.update("b1", updates);

        assertThat(result.getId()).isEqualTo("b1");
        assertThat(result.getTitle()).isEqualTo("Nuevo título");
        assertThat(result.getAuthor()).isEqualTo("Otro autor");
        assertThat(result.getPages()).isEqualTo(300);
        assertThat(result.getState()).isEqualTo(BookState.COMPLETED);

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Nuevo título");
        assertThat(captor.getValue().getId()).isEqualTo("b1");
    }

    @Test
    void delete_deletesBook_whenExists() {
        Book book = sampleBook();
        book.setId("b1");
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));

        bookService.delete("b1");

        verify(bookRepository).deleteById("b1");
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(bookRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.delete("nope"))
                .isInstanceOf(BookNotFoundException.class)
                .hasMessageContaining("nope");
    }
}
