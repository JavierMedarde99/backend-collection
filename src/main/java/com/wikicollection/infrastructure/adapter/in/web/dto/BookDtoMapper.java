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
                .title(request.title())
                .authors(request.authors())
                .isbn(request.isbn())
                .publisher(request.publisher())
                .publishedDate(request.publishedDate())
                .description(request.description())
                .pageCount(request.pageCount())
                .categories(request.categories())
                .coverImage(request.coverImage())
                .language(request.language())
                .status(request.status())
                .userRating(request.userRating())
                .notes(request.notes())
                .tags(request.tags())
                .dateCompleted(request.dateCompleted())
                .externalSource(request.externalSource())
                .externalId(request.externalId())
                .build();
    }

    public BookResponse toResponse(Book book) {
        if (book == null) {
            return null;
        }
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthors(),
                book.getIsbn(),
                book.getPublisher(),
                book.getPublishedDate(),
                book.getDescription(),
                book.getPageCount(),
                book.getCategories(),
                book.getCoverImage(),
                book.getLanguage(),
                book.getStatus(),
                book.getUserRating(),
                book.getNotes(),
                book.getTags(),
                book.getDateAdded(),
                book.getDateCompleted(),
                book.getDateUpdated(),
                book.getExternalSource(),
                book.getExternalId());
    }
}