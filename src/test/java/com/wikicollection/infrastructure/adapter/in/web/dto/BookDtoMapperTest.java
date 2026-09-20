package com.wikicollection.infrastructure.adapter.in.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;

import org.junit.jupiter.api.Test;

class BookDtoMapperTest {

    private final BookDtoMapper mapper = new BookDtoMapper();

    @Test
    void mapsIsbn_requestToResponse() {
        BookRequest request = new BookRequest("ext1", "9788498382671", "Dune", null, "Herbert", 412,
                BookType.NOVEL, BookState.TO_READ, null, null, null, null, null);

        BookResponse response = mapper.toResponse(mapper.toDomain(request));

        assertThat(response.isbn()).isEqualTo("9788498382671");
    }

    @Test
    void mapsNullIsbn_whenAbsent() {
        BookRequest request = new BookRequest("ext1", null, "Dune", null, "Herbert", 412,
                BookType.NOVEL, BookState.TO_READ, null, null, null, null, null);

        BookResponse response = mapper.toResponse(mapper.toDomain(request));

        assertThat(response.isbn()).isNull();
    }
}
