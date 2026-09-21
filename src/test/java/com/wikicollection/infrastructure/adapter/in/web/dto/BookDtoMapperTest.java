package com.wikicollection.infrastructure.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;

import org.junit.jupiter.api.Test;

class BookDtoMapperTest {

    private final BookDtoMapper mapper = new BookDtoMapper();

<<<<<<< HEAD
    @Test
    void mapsIsbn_requestToResponse() {
        BookRequest request = new BookRequest("ext1", "9788498382671", "Dune", null, "Herbert", 412,
                BookType.NOVEL, BookState.TO_READ, null, null, null, null, null, null);

        BookResponse response = mapper.toResponse(mapper.toDomain(request));

        assertThat(response.isbn()).isEqualTo("9788498382671");
    }

    @Test
    void mapsNullIsbn_whenAbsent() {
        BookRequest request = new BookRequest("ext1", null, "Dune", null, "Herbert", 412,
                BookType.NOVEL, BookState.TO_READ, null, null, null, null, null, null);

        BookResponse response = mapper.toResponse(mapper.toDomain(request));

        assertThat(response.isbn()).isNull();
    }

    private BookRequest sampleRequest() {
        return new BookRequest("ext1", "9788498382671", "Dune", "Ciencia ficción", "Herbert", 412,
                BookType.NOVEL, BookState.READING, "Muy bueno", 5, 120,
                LocalDate.of(2024, 1, 1), null, "http://front");
    }

    @Test
    void toDomain_mapsAllFields_includingPagesRead() {
        Book book = mapper.toDomain(sampleRequest());

        assertThat(book.getExternalId()).isEqualTo("ext1");
        assertThat(book.getTitle()).isEqualTo("Dune");
        assertThat(book.getAuthor()).isEqualTo("Herbert");
        assertThat(book.getPages()).isEqualTo(412);
        assertThat(book.getState()).isEqualTo(BookState.READING);
        assertThat(book.getStart()).isEqualTo(5);
        assertThat(book.getPagesRead()).isEqualTo(120);
        assertThat(book.getFrontpage()).isEqualTo("http://front");
    }

    @Test
    void toResponse_mapsAllFields_includingPagesRead() {
        BookResponse response = mapper.toResponse(mapper.toDomain(sampleRequest()));

        assertThat(response.title()).isEqualTo("Dune");
        assertThat(response.pagesRead()).isEqualTo(120);
        assertThat(response.start()).isEqualTo(5);
    }

    @Test
    void toDomain_returnsNull_whenRequestNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toResponse_returnsNull_whenBookNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void withoutPrivate_hidesPagesRead() {
        BookResponse response = mapper.toResponse(mapper.toDomain(sampleRequest()));

        BookResponse stripped = response.withoutPrivate();

        assertThat(stripped.pagesRead()).isNull();
        assertThat(stripped.title()).isEqualTo("Dune");
    }
}
