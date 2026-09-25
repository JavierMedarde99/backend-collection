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
                .ownerId(book.getOwnerId())
                .userOwned(UserOwnedMapping.toEntity(book.getUserOwned()))
                .externalId(book.getExternalId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .descripcion(book.getDescripcion())
                .author(book.getAuthor())
                .genres(book.getGenres())
                .pages(book.getPages())
                .type(book.getType())
                .state(book.getState())
                .comment(book.getComment())
                .start(book.getStart())
                .pagesRead(book.getPagesRead())
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
                .ownerId(entity.getOwnerId())
                .userOwned(UserOwnedMapping.toDomain(entity.getUserOwned()))
                .externalId(entity.getExternalId())
                .isbn(entity.getIsbn())
                .title(entity.getTitle())
                .descripcion(entity.getDescripcion())
                .author(entity.getAuthor())
                .genres(entity.getGenres())
                .pages(entity.getPages())
                .type(entity.getType())
                .state(entity.getState())
                .comment(entity.getComment())
                .start(entity.getStart())
                .pagesRead(entity.getPagesRead())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .frontpage(entity.getFrontpage())
                .build();
    }
}
