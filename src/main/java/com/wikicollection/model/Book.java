package com.wikicollection.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Document(collection = "books")
public class Book {

    @Id
    private String id;

    @NotBlank(message = "El título es obligatorio")
    private String title;

    @NotEmpty(message = "Debe contener al menos un autor")
    private List<String> authors;

    @Indexed(unique = true, sparse = true)
    private String isbn;

    private String publisher;

    private LocalDate publishedDate;

    private String description;

    @Min(value = 0, message = "El número de páginas no puede ser negativo")
    private Integer pageCount;

    private List<String> categories;

    private String coverImage;

    private String language;

    @NotNull(message = "El estado es obligatorio")
    private BookStatus status;

    @Min(value = 1, message = "La valoración mínima es 1")
    @Max(value = 5, message = "La valoración máxima es 5")
    private Integer userRating;

    private String notes;

    private List<String> tags;

    @CreatedDate
    private LocalDateTime dateAdded;

    private LocalDateTime dateCompleted;

    private String externalSource;

    private String externalId;

    @LastModifiedDate
    private LocalDateTime dateUpdated;

}