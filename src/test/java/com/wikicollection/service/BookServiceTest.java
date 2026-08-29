package com.wikicollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.wikicollection.model.Book;
import com.wikicollection.model.BookStatus;
import com.wikicollection.repository.BookRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book sampleBook() {
        Book book = new Book();
        book.setTitle("Cien años de soledad");
        book.setAuthors(List.of("Gabriel García Márquez"));
        book.setStatus(BookStatus.WISHLIST);
        return book;
    }

    @Test
    void findAll_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        when(bookRepository.findAll(pageable)).thenReturn(Page.empty());

        Page<Book> result = bookService.findAll(pageable);

        assertThat(result).isEmpty();
        verify(bookRepository).findAll(pageable);
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
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void save_setsDateAdded_whenNull() {
        Book book = sampleBook();
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bookService.save(book);

        assertThat(book.getDateAdded()).isNotNull();
    }

    @Test
    void save_marksDateCompleted_whenCompleted() {
        Book book = sampleBook();
        book.setStatus(BookStatus.COMPLETED);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bookService.save(book);

        assertThat(book.getDateCompleted()).isNotNull();
    }

    @Test
    void save_clearsDateCompleted_whenNotCompleted() {
        Book book = sampleBook();
        book.setStatus(BookStatus.READING);
        book.setDateCompleted(LocalDateTime.now());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bookService.save(book);

        assertThat(book.getDateCompleted()).isNull();
    }

    @Test
    void save_throwsConflict_whenIsbnAlreadyUsed() {
        Book book = sampleBook();
        book.setIsbn("9780307474728");
        Book existing = sampleBook();
        existing.setId("other");
        when(bookRepository.findByIsbn("9780307474728")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> bookService.save(book))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(bookRepository).findByIsbn("9780307474728");
    }

    @Test
    void update_throwsNotFound_whenMissing() {
        when(bookRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update("nope", sampleBook()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void update_appliesFieldsAndKeepsId_andDateAdded() {
        Book existing = sampleBook();
        existing.setId("b1");
        existing.setDateAdded(LocalDateTime.now().minusDays(5));
        Book updates = sampleBook();
        updates.setTitle("Nuevo título");
        when(bookRepository.findById("b1")).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book result = bookService.update("b1", updates);

        assertThat(result.getId()).isEqualTo("b1");
        assertThat(result.getTitle()).isEqualTo("Nuevo título");
        assertThat(result.getDateAdded()).isNotNull();

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Nuevo título");
    }

    @Test
    void delete_deletesBook_whenExists() {
        Book book = sampleBook();
        book.setId("b1");
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));

        bookService.delete("b1");

        verify(bookRepository).delete(book);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(bookRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.delete("nope"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}