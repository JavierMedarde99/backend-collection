package com.wikicollection.domain.model;

import java.time.LocalDate;
import java.util.List;

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
public class Book {

    private String id;
    private String ownerId;
    private UserOwned userOwned;
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

    /** Nombre visible de la serie, texto libre introducido por el usuario. */
    private String series;
    /** Clave normalizada de la serie; se calcula en el servicio, nunca desde el cliente. */
    private String seriesKey;
    /** Posición dentro de la serie. */
    private Integer seriesOrder;
}
