package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;

import com.wikicollection.domain.model.BookSearchCriteria;
import com.wikicollection.domain.model.BookSearchResult;
import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.port.in.BookSearchUseCase;
import com.wikicollection.domain.port.in.BookUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.BookDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.BookRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.BookResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.ProgressUpdateRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.wikicollection.infrastructure.config.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.validation.Valid;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/books")
@Validated
@Tag(name = "Libros", description = "Gestión del catálogo de libros")
public class BookController {

    private final BookUseCase bookUseCase;
    private final BookSearchUseCase bookSearchUseCase;
    private final BookDtoMapper mapper;

    private final ResponseVisibility visibility;

    public BookController(BookUseCase bookUseCase, BookSearchUseCase bookSearchUseCase, BookDtoMapper mapper,
                               ResponseVisibility visibility) {
        this.bookUseCase = bookUseCase;
        this.bookSearchUseCase = bookSearchUseCase;
        this.mapper = mapper;
        this.visibility = visibility;
    }

    @GetMapping
    @Operation(summary = "Lista libros", description = "Devuelve una página de libros con filtros opcionales por nombre, autor, tipo y estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de libros encontrada"),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación o filtros inválidos")
    })
    public Page<BookResponse> list(
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ordenación como campo,asc|desc") @RequestParam(defaultValue = "title,asc") String sort,
            @Parameter(description = "Filtro por título (búsqueda parcial, insensible a mayúsculas)") @RequestParam(required = false) @Size(max = 100, message = "La búsqueda no puede superar los 100 caracteres") String name,
            @Parameter(description = "Filtro por autor") @RequestParam(required = false) String author,
            @Parameter(description = "Filtro por tipo de libro") @RequestParam(required = false) BookType type,
            @Parameter(description = "Filtro por estado de lectura") @RequestParam(required = false) BookState state,
            @Parameter(description = "Filtro por género (coincide con cualquiera de la lista)") @RequestParam(required = false) String genre,
            @Parameter(description = "Filtro por propiedad: mine|other|all") @RequestParam(defaultValue = "mine") String owner,
            @CurrentUser String viewerId) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        BookSearchCriteria criteria = new BookSearchCriteria(name, author, type, state, genre, null, null);
        return bookUseCase.search(criteria, pageable, owner, viewerId).map(book -> {
            var response = mapper.toResponse(book);
            return visibility.canSeePrivate(book.getOwnerId(), CollectionType.BOOKS) ? response : response.withoutPrivate();
        });
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un libro por su id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro encontrado"),
            @ApiResponse(responseCode = "404", description = "Libro no encontrado")
    })
    public BookResponse getById(
            @Parameter(description = "Identificador del libro") @PathVariable String id) {
        var book = bookUseCase.findById(id);
        var response = mapper.toResponse(book);
        return visibility.canSeePrivate(book.getOwnerId(), CollectionType.BOOKS) ? response : response.withoutPrivate();
    }

    @PostMapping
    @Operation(summary = "Crea un libro")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Libro creado"),
            @ApiResponse(responseCode = "400", description = "Datos del libro inválidos")
    })
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request, @CurrentUser String currentUserId, UriComponentsBuilder ucb) {
        var saved = bookUseCase.save(mapper.toDomain(request), currentUserId);
        URI location = ucb.path("/api/v1/books/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un libro existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Libro actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos del libro inválidos"),
            @ApiResponse(responseCode = "404", description = "Libro no encontrado")
    })
    public BookResponse update(
            @Parameter(description = "Identificador del libro") @PathVariable String id,
            @Valid @RequestBody BookRequest request, @CurrentUser String currentUserId) {
        return mapper.toResponse(bookUseCase.update(id, mapper.toDomain(request), currentUserId));
    }

    @PatchMapping("/{id}/progress")
    @Operation(summary = "Actualiza solo el progreso de lectura")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Progreso actualizado"),
            @ApiResponse(responseCode = "400", description = "Progreso inválido"),
            @ApiResponse(responseCode = "404", description = "Libro no encontrado")
    })
    public BookResponse updateProgress(
            @Parameter(description = "Identificador del libro") @PathVariable String id,
            @Valid @RequestBody ProgressUpdateRequest request, @CurrentUser String currentUserId) {
        var book = bookUseCase.findById(id);
        book.setPagesRead(request.pagesRead());
        return mapper.toResponse(bookUseCase.update(id, book, currentUserId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un libro")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Libro eliminado"),
            @ApiResponse(responseCode = "404", description = "Libro no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del libro") @PathVariable String id, @CurrentUser String currentUserId) {
        bookUseCase.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Busca libros en el catálogo externo", description = "Busca en Google Books por título (name) o por ISBN (isbn). Si vienen ambos, manda isbn.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda"),
            @ApiResponse(responseCode = "400", description = "Falta 'name' o 'isbn'"),
            @ApiResponse(responseCode = "502", description = "El catálogo externo devolvió un error"),
            @ApiResponse(responseCode = "503", description = "El catálogo externo no está disponible")
    })
    public Page<BookSearchResult> search(
            @Parameter(description = "Título a buscar") @RequestParam(value = "name", required = false) @Size(max = 100, message = "La búsqueda no puede superar los 100 caracteres") String name,
            @Parameter(description = "ISBN a buscar (con o sin guiones)") @RequestParam(value = "isbn", required = false) @Size(max = 20, message = "El ISBN no puede superar los 20 caracteres") String isbn,
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size) {
        if (isbn != null && !isbn.isBlank()) {
            return bookSearchUseCase.searchByIsbn(isbn, PageRequest.of(page, size));
        }
        if (name != null && !name.isBlank()) {
            return bookSearchUseCase.search(name, PageRequest.of(page, size));
        }
        throw new IllegalArgumentException("Indica 'name' o 'isbn' para buscar");
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