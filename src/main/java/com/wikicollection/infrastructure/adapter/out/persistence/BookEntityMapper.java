package com.wikicollection.infrastructure.adapter.out.persistence;

import com.wikicollection.domain.model.Book;

import org.springframework.stereotype.Component;

@Component
public class BookEntityMapper {

    public BookEntity toEntity(Book book) {
        if (book == null) {
            return null;
        }
        return BookEntity.builder()
                .id(book.getId())
                .externalId(book.getExternalId())
                .title(book.getTitle())
                .descripcion(book.getDescripcion())
                .author(book.getAuthor())
                .pages(book.getPages())
                .type(book.getType())
                .state(book.getState())
                .comment(book.getComment())
                .start(book.getStart())
                .startDate(book.getStartDate())
                .endDate(book.getEndDate())
                .frontpage(book.getFrontpage())
                .build();
    }

    public Book toDomain(BookEntity entity) {
        if (entity == null) {
            return null;
        }
        return Book.builder()
                .id(entity.getId())
                .externalId(entity.getExternalId())
                .title(entity.getTitle())
                .descripcion(entity.getDescripcion())
                .author(entity.getAuthor())
                .pages(entity.getPages())
                .type(entity.getType())
                .state(entity.getState())
                .comment(entity.getComment())
                .start(entity.getStart())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .frontpage(entity.getFrontpage())
                .build();
    }
}
