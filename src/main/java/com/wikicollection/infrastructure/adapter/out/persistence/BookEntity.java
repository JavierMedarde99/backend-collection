package com.wikicollection.infrastructure.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.wikicollection.domain.model.BookStatus;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
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

    private String title;

    private List<String> authors;

    @Indexed(unique = true, sparse = true)
    private String isbn;

    private String publisher;

    private LocalDate publishedDate;

    private String description;

    private Integer pageCount;

    private List<String> categories;

    private String coverImage;

    private String language;

    private BookStatus status;

    private Integer userRating;

    private String notes;

    private List<String> tags;

    @CreatedDate
    private LocalDateTime dateAdded;

    private LocalDateTime dateCompleted;

    @LastModifiedDate
    private LocalDateTime dateUpdated;

    private String externalSource;

    private String externalId;
}