package com.wikicollection.infrastructure.adapter.out.persistence;

import java.time.LocalDate;

import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;

import org.springframework.data.annotation.Id;
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
