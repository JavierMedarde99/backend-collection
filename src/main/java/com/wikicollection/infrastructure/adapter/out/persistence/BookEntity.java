package com.wikicollection.infrastructure.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;

import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Document(collection = "books")
public class BookEntity {

    @Id
    private String id;
    @Indexed
    private String ownerId;
    private UserOwnedEntity userOwned;

    private String externalId;

    private String isbn;

    private String title;

    private String descripcion;

    private String author;

    private List<String> genres;

    private Integer pages;

    private BookType type;

    private BookState state;

    private String comment;

    private Integer start;

    private Integer pagesRead;

    private LocalDate startDate;

    private LocalDate endDate;

    private String frontpage;

    private String publisher;

    private Integer publicationYear;

    private LocalDate acquisitionDate;

    private java.math.BigDecimal acquisitionPrice;

    /** Sin @Indexed: el filtro por serie es regex sin anclar, que Mongo no indexa (igual que title o author). */
    private String series;

    /** Clave de agrupación calculada por el servicio; se usará en la agregación de progreso. */
    private String seriesKey;

    private Integer seriesOrder;
}
