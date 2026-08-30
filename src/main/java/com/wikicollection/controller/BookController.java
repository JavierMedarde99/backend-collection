package com.wikicollection.controller;

import java.net.URI;
import java.util.List;

import com.wikicollection.dto.GoogleBookResult;
import com.wikicollection.model.Book;
import com.wikicollection.model.BookStatus;
import com.wikicollection.service.BookService;
import com.wikicollection.service.GoogleBooksService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/books")
@Validated
public class BookController {

    private final BookService bookService;
    private final GoogleBooksService googleBooksService;

    public BookController(BookService bookService, GoogleBooksService googleBooksService) {
        this.bookService = bookService;
        this.googleBooksService = googleBooksService;
    }

    @GetMapping
    public Page<Book> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateAdded,desc") String sort,
            @RequestParam(required = false) BookStatus status,
            @RequestParam(required = false) String tag) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        if (status != null) {
            return bookService.findByStatus(status, pageable);
        }
        if (tag != null && !tag.isBlank()) {
            return bookService.findByTag(tag, pageable);
        }
        return bookService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public Book getById(@PathVariable String id) {
        return bookService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Book> create(@Valid @RequestBody Book book, UriComponentsBuilder ucb) {
        Book saved = bookService.save(book);
        URI location = ucb.path("/api/books/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public Book update(@PathVariable String id, @Valid @RequestBody Book book) {
        return bookService.update(id, book);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<GoogleBookResult> search(@RequestParam("q") String query) {
        return googleBooksService.search(query);
    }

    private Sort buildSort(String sort) {
        String field = "dateAdded";
        Sort.Direction direction = Sort.Direction.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length > 0 && !parts[0].isBlank()) {
                field = parts[0].trim();
            }
            if (parts.length > 1 && !parts[1].isBlank()) {
                direction = "asc".equalsIgnoreCase(parts[1].trim())
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;
            }
        }
        return Sort.by(direction, field);
    }
}