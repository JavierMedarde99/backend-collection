package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookSearchCriteria;
import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;
import com.wikicollection.domain.port.out.BookRepository;
import com.wikicollection.domain.port.out.ExternalBookCatalogClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * seriesKey se calcula en el servicio a partir de series, nunca se acepta del
 * cliente: es la clave de agrupación y si el cliente la controllable podría
 * romper la serie de otro libro.
 */
@ExtendWith(MockitoExtension.class)
class BookSeriesServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ExternalBookCatalogClient catalogClient;

    @Spy
    private DateRangeValidator dateRangeValidator = new DateRangeValidator();

    @Mock
    private OwnershipValidator ownershipValidator;

    @Mock
    private OwnerScopeResolver ownerScopeResolver;

    @Mock
    private OwnerResolver ownerResolver;

    @InjectMocks
    private BookService service;

    private static Book bookWithSeries(String series, Integer order) {
        return Book.builder()
                .title("La piedra filosofal")
                .author("J. K. Rowling")
                .type(BookType.NOVEL)
                .state(BookState.TO_READ)
                .series(series)
                .seriesOrder(order)
                .build();
    }

    @Test
    void save_normalizaSeriesKey() {
        Book saved = Book.builder().id("b1").title("t").build();
        when(bookRepository.save(any(Book.class))).thenReturn(saved);

        Book result = service.save(bookWithSeries("Harry Potter", 1), "u1");

        Book captured = capture();
        assertThat(captured.getSeriesKey()).isEqualTo("harry potter");
        assertThat(captured.getSeries()).isEqualTo("Harry Potter");
        assertThat(captured.getSeriesOrder()).isEqualTo(1);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void save_limpiaSerieYPosicion_siLaSerieEstaEnBlanco() {
        when(bookRepository.save(any(Book.class))).thenReturn(Book.builder().id("b1").build());

        service.save(bookWithSeries("   ", 3), "u1");

        Book captured = capture();
        assertThat(captured.getSeriesKey()).isNull();
        assertThat(captured.getSeries()).isNull();
        assertThat(captured.getSeriesOrder()).isNull();
    }

    @Test
    void save_descartaSeriesKeyEnviadoPorElCliente() {
        when(bookRepository.save(any(Book.class))).thenReturn(Book.builder().id("b1").build());

        Book book = bookWithSeries("Dune", 1);
        book.setSeriesKey("clave-falsa-del-cliente");
        service.save(book, "u1");

        assertThat(capture().getSeriesKey()).isEqualTo("dune");
    }

    @Test
    void update_recalculaSeriesKey() {
        Book existing = bookWithSeries(null, null);
        existing.setId("b1");
        existing.setOwnerId("u1");
        when(bookRepository.findById("b1")).thenReturn(java.util.Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenReturn(existing);

        service.update("b1", bookWithSeries("The  Hobbit", 1), "u1");

        assertThat(capture().getSeriesKey()).isEqualTo("the hobbit");
    }

    @Test
    void update_borraLaSerieSiElClienteLaDejaVacia() {
        Book existing = bookWithSeries("Harry Potter", 1);
        existing.setId("b1");
        existing.setOwnerId("u1");
        when(bookRepository.findById("b1")).thenReturn(java.util.Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenReturn(existing);

        service.update("b1", bookWithSeries(null, null), "u1");

        Book captured = capture();
        assertThat(captured.getSeriesKey()).isNull();
        assertThat(captured.getSeriesOrder()).isNull();
    }

    private Book capture() {
        org.mockito.ArgumentCaptor<Book> captor = org.mockito.ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        return captor.getValue();
    }
}