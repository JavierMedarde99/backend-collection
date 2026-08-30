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
                .title(book.getTitle())
                .authors(book.getAuthors())
                .isbn(book.getIsbn())
                .publisher(book.getPublisher())
                .publishedDate(book.getPublishedDate())
                .description(book.getDescription())
                .pageCount(book.getPageCount())
                .categories(book.getCategories())
                .coverImage(book.getCoverImage())
                .language(book.getLanguage())
                .status(book.getStatus())
                .userRating(book.getUserRating())
                .notes(book.getNotes())
                .tags(book.getTags())
                .dateAdded(book.getDateAdded())
                .dateCompleted(book.getDateCompleted())
                .dateUpdated(book.getDateUpdated())
                .externalSource(book.getExternalSource())
                .externalId(book.getExternalId())
                .build();
    }

    public Book toDomain(BookEntity entity) {
        if (entity == null) {
            return null;
        }
        return Book.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .authors(entity.getAuthors())
                .isbn(entity.getIsbn())
                .publisher(entity.getPublisher())
                .publishedDate(entity.getPublishedDate())
                .description(entity.getDescription())
                .pageCount(entity.getPageCount())
                .categories(entity.getCategories())
                .coverImage(entity.getCoverImage())
                .language(entity.getLanguage())
                .status(entity.getStatus())
                .userRating(entity.getUserRating())
                .notes(entity.getNotes())
                .tags(entity.getTags())
                .dateAdded(entity.getDateAdded())
                .dateCompleted(entity.getDateCompleted())
                .dateUpdated(entity.getDateUpdated())
                .externalSource(entity.getExternalSource())
                .externalId(entity.getExternalId())
                .build();
    }
}