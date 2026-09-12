package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;

import com.wikicollection.domain.model.MovieMediaType;
import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.MovieSearchResult;
import com.wikicollection.domain.model.MovieStatus;
import com.wikicollection.domain.port.in.MovieSearchUseCase;
import com.wikicollection.domain.port.in.MovieShowUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.MovieShowDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.MovieShowRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.MovieShowResponse;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/movieshows")
@Validated
@Tag(name = "Películas y series", description = "Gestión del catálogo de películas y series")
public class MovieShowController {

    private final MovieShowUseCase movieShowUseCase;
    private final MovieSearchUseCase movieSearchUseCase;
    private final MovieShowDtoMapper mapper;

    public MovieShowController(MovieShowUseCase movieShowUseCase,
                               MovieSearchUseCase movieSearchUseCase,
                               MovieShowDtoMapper mapper) {
        this.movieShowUseCase = movieShowUseCase;
        this.movieSearchUseCase = movieSearchUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Lista películas y series", description = "Devuelve una página con filtros opcionales por título, estado y tipo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página encontrada"),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación o filtros inválidos")
    })
    public Page<MovieShowResponse> list(
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ordenación como campo,asc|desc") @RequestParam(defaultValue = "title,asc") String sort,
            @Parameter(description = "Filtro por título (búsqueda parcial, insensible a mayúsculas)") @RequestParam(required = false) String name,
            @Parameter(description = "Filtro por estado") @RequestParam(required = false) MovieStatus status,
            @Parameter(description = "Filtro por tipo") @RequestParam(required = false) MovieMediaType mediaType) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        MovieSearchCriteria criteria = new MovieSearchCriteria(name, status, mediaType);
        return movieShowUseCase.search(criteria, pageable).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una película/serie por su id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encontrada"),
            @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    public MovieShowResponse getById(
            @Parameter(description = "Identificador") @PathVariable String id) {
        return mapper.toResponse(movieShowUseCase.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crea una película/serie")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "Ya existe con ese externalId")
    })
    public ResponseEntity<MovieShowResponse> create(@Valid @RequestBody MovieShowRequest request, UriComponentsBuilder ucb) {
        var saved = movieShowUseCase.save(mapper.toDomain(request));
        URI location = ucb.path("/api/v1/movieshows/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una película/serie existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "No encontrada"),
            @ApiResponse(responseCode = "409", description = "Ya existe con ese externalId")
    })
    public MovieShowResponse update(
            @Parameter(description = "Identificador") @PathVariable String id,
            @Valid @RequestBody MovieShowRequest request) {
        return mapper.toResponse(movieShowUseCase.update(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una película/serie")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Eliminada"),
            @ApiResponse(responseCode = "404", description = "No encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador") @PathVariable String id) {
        movieShowUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Busca películas y series en el catálogo externo", description = "Busca en TMDB por título, opcionalmente filtrando por tipo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda"),
            @ApiResponse(responseCode = "400", description = "El parámetro 'name' es obligatorio")
    })
    public List<MovieSearchResult> search(
            @Parameter(description = "Título a buscar") @RequestParam("name") String name,
            @Parameter(description = "Tipo: MOVIE o TV (por defecto ambos)") @RequestParam(required = false) MovieMediaType mediaType) {
        return movieSearchUseCase.search(name, mediaType);
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
