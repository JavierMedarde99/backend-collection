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
                .isbn(request.isbn())
                .title(request.title())
                .descripcion(request.descripcion())
                .author(request.author())
                .genres(request.genres())
                .pages(request.pages())
                .type(request.type())
                .state(request.state())
                .comment(request.comment())
                .start(request.start())
                .pagesRead(request.pagesRead())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .frontpage(request.frontpage())
                .publisher(request.publisher())
                .publicationYear(request.publicationYear())
                .acquisitionDate(request.acquisitionDate())
                .acquisitionPrice(request.acquisitionPrice())
                .build();
    }

    public BookResponse toResponse(Book book) {
        if (book == null) {
            return null;
        }
        return new BookResponse(
                book.getId(),
                book.getExternalId(),
                book.getIsbn(),
                book.getTitle(),
                book.getDescripcion(),
                book.getAuthor(),
                book.getGenres(),
                book.getPages(),
                book.getType(),
                book.getState(),
                book.getComment(),
                book.getStart(),
                book.getPagesRead(),
                book.getStartDate(),
                book.getEndDate(),
                book.getFrontpage(),
                book.getPublisher(),
                book.getPublicationYear(),
                book.getAcquisitionDate(),
                book.getAcquisitionPrice(),
                UserOwnedResponse.from(book.getUserOwned()));
    }
}
