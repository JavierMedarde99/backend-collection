package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;

import com.wikicollection.domain.model.BookSearchCriteria;
import com.wikicollection.domain.model.BookSearchResult;
import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;
import com.wikicollection.domain.port.in.BookSearchUseCase;
import com.wikicollection.domain.port.in.BookUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.BookDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.BookRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.BookResponse;

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

    private final BookUseCase bookUseCase;
    private final BookSearchUseCase bookSearchUseCase;
    private final BookDtoMapper mapper;

    public BookController(BookUseCase bookUseCase, BookSearchUseCase bookSearchUseCase, BookDtoMapper mapper) {
        this.bookUseCase = bookUseCase;
        this.bookSearchUseCase = bookSearchUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public Page<BookResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title,asc") String sort,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) BookType type,
            @RequestParam(required = false) BookState state) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        BookSearchCriteria criteria = new BookSearchCriteria(name, author, type, state);
        return bookUseCase.search(criteria, pageable).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    public BookResponse getById(@PathVariable String id) {
        return mapper.toResponse(bookUseCase.findById(id));
    }

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request, UriComponentsBuilder ucb) {
        var saved = bookUseCase.save(mapper.toDomain(request));
        URI location = ucb.path("/api/books/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    public BookResponse update(@PathVariable String id, @Valid @RequestBody BookRequest request) {
        return mapper.toResponse(bookUseCase.update(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        bookUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<BookSearchResult> search(@RequestParam("name") String name) {
        return bookSearchUseCase.search(name);
    }

    private Sort buildSort(String sort) {
        String field = "title";
        Sort.Direction direction = Sort.Direction.ASC;
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