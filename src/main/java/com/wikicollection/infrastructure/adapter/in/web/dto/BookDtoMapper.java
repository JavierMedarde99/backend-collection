package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.Book;

import org.springframework.stereotype.Component;

@Component
public class BookDtoMapper {

    public Book toDomain(BookRequest request) {
        if (request == null) {
            return null;
        }
        return Book.builder()
                .externalId(request.externalId())
                .title(request.title())
                .descripcion(request.descripcion())
                .author(request.author())
                .pages(request.pages())
                .type(request.type())
                .state(request.state())
                .comment(request.comment())
                .start(request.start())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .frontpage(request.frontpage())
                .build();
    }

    public BookResponse toResponse(Book book) {
        if (book == null) {
            return null;
        }
        return new BookResponse(
                book.getId(),
                book.getExternalId(),
                book.getTitle(),
                book.getDescripcion(),
                book.getAuthor(),
                book.getPages(),
                book.getType(),
                book.getState(),
                book.getComment(),
                book.getStart(),
                book.getStartDate(),
                book.getEndDate(),
                book.getFrontpage());
    }
}
