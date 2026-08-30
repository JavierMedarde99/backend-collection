package com.wikicollection.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private String title;
    private List<String> authors;
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
    private LocalDateTime dateAdded;
    private LocalDateTime dateCompleted;
    private LocalDateTime dateUpdated;
    private String externalSource;
    private String externalId;
}