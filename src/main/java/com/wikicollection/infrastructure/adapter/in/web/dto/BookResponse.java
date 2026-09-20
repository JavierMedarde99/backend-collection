package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.time.LocalDate;

import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;

public record BookResponse(
        String id,
        String externalId,
        String isbn,
        String title,
        String descripcion,
        String author,
        Integer pages,
        BookType type,
        BookState state,
        String comment,
        Integer start,
        LocalDate startDate,
        LocalDate endDate,
        String frontpage,
        UserOwnedResponse userOwned) {


    public BookResponse withoutPrivate() {
        return new BookResponse(id, externalId, isbn, title, descripcion, author, pages, type, state,
                null, null, null, null, frontpage, userOwned);
    }
}
