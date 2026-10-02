package com.wikicollection.infrastructure.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;

import org.junit.jupiter.api.Test;

/**
 * Decisión de diseño: la serie es metadata pública (como publisher o genres),
 * así que sobrevive a withoutPrivate(). Si el dueño comparte un libro en su
 * perfil, quien lo vea debe poder saber a qué serie pertenece.
 */
class BookResponseSeriesVisibilityTest {

    private static BookResponse fullBook() {
        return new BookResponse("b1", "gb1", "9781234567890", "La piedra filosofal", "desc",
                "J. K. Rowling", java.util.List.of("fantasía"), 300, BookType.NOVEL, BookState.TO_READ,
                "comentario privado", 4, 100, java.time.LocalDate.of(2026, 1, 1), null, "front.jpg",
                "Salamandra", 1997, java.time.LocalDate.of(2026, 2, 1), new java.math.BigDecimal("12.50"),
                "Harry Potter", 1, UserOwnedResponse.from(null));
    }

    @Test
    void withoutPrivate_conservaLaSerie() {
        BookResponse publicView = fullBook().withoutPrivate();

        assertThat(publicView.series()).isEqualTo("Harry Potter");
        assertThat(publicView.seriesOrder()).isEqualTo(1);
    }

    @Test
    void withoutPrivate_ocultaLoPrivado() {
        BookResponse publicView = fullBook().withoutPrivate();

        assertThat(publicView.comment()).isNull();
        assertThat(publicView.start()).isNull();
        assertThat(publicView.pagesRead()).isNull();
        assertThat(publicView.acquisitionDate()).isNull();
        assertThat(publicView.acquisitionPrice()).isNull();
    }

    @Test
    void withoutPrivate_noExponeLaClaveDeAgrupacion() {
        // seriesKey es interna: normaliza para agrupar, no aporta nada al cliente.
        assertThat(BookResponse.class.getRecordComponents())
                .noneMatch(c -> c.getName().equals("seriesKey"));
    }
}
