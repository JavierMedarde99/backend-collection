package com.wikicollection.domain.model;

import java.time.LocalDate;

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
    private String externalId;
    private String title;
    private String descripcion;
    private String author;
    private Integer pages;
    private BookType type;
    private BookState state;
    private String comment;
    private Integer start;
    private LocalDate startDate;
    private LocalDate endDate;
    private String frontpage;
}
