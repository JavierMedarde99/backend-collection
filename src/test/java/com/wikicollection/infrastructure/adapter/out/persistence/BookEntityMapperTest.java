package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;

import org.junit.jupiter.api.Test;

class BookEntityMapperTest {

    private final BookEntityMapper mapper = new BookEntityMapper();

    @Test
    void toEntity_mapsPublisherPublicationYearAndAcquisition() {
        Book book = Book.builder()
                .id("b-1")
                .ownerId("owner-1")
                .title("Cien años de soledad")
                .state(BookState.COMPLETED)
                .type(BookType.NOVEL)
                .publisher("Editorial Debate")
                .publicationYear(1967)
                .acquisitionDate(LocalDate.of(2024, 3, 15))
                .acquisitionPrice(new BigDecimal("12.50"))
                .build();

        BookEntity entity = mapper.toEntity(book);

        assertEquals("Editorial Debate", entity.getPublisher());
        assertEquals(1967, entity.getPublicationYear());
        assertEquals(LocalDate.of(2024, 3, 15), entity.getAcquisitionDate());
        assertEquals(new BigDecimal("12.50"), entity.getAcquisitionPrice());
    }

    @Test
    void toDomain_mapsPublisherPublicationYearAndAcquisition() {
        BookEntity entity = BookEntity.builder()
                .id("b-1")
                .ownerId("owner-1")
                .title("Cien años de soledad")
                .state(BookState.WISHLIST)
                .type(BookType.NOVEL)
                .publisher("Editorial Debate")
                .publicationYear(1967)
                .acquisitionDate(LocalDate.of(2024, 3, 15))
                .acquisitionPrice(new BigDecimal("12.50"))
                .build();

        Book book = mapper.toDomain(entity);

        assertEquals("Editorial Debate", book.getPublisher());
        assertEquals(1967, book.getPublicationYear());
        assertEquals(LocalDate.of(2024, 3, 15), book.getAcquisitionDate());
        assertEquals(new BigDecimal("12.50"), book.getAcquisitionPrice());
        assertEquals(BookState.WISHLIST, book.getState());
    }

    @Test
    void toDomain_readsLegacyDocumentWithoutNewFields_asNull() {
        BookEntity entity = BookEntity.builder()
                .id("b-1")
                .title("Cien años de soledad")
                .build();

        Book book = mapper.toDomain(entity);

        assertNull(book.getPublisher());
        assertNull(book.getPublicationYear());
        assertNull(book.getAcquisitionDate());
        assertNull(book.getAcquisitionPrice());
    }
}
