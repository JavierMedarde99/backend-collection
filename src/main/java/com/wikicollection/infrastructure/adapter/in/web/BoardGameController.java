package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;

import com.wikicollection.domain.model.BoardGameSearchCriteria;
import com.wikicollection.domain.model.BoardGameStatus;
import com.wikicollection.domain.port.in.BoardGameSearchUseCase;
import com.wikicollection.domain.port.in.BoardGameUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.BoardGameDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.BoardGameRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.BoardGameResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.BoardGameSearchResponse;

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

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/boardgames")
@Validated
@Tag(name = "Juegos de mesa", description = "Gestión del catálogo de juegos de mesa")
public class BoardGameController {

    private final BoardGameUseCase boardGameUseCase;
    private final BoardGameSearchUseCase boardGameSearchUseCase;
    private final BoardGameDtoMapper mapper;

    public BoardGameController(BoardGameUseCase boardGameUseCase,
                               BoardGameSearchUseCase boardGameSearchUseCase,
                               BoardGameDtoMapper mapper) {
        this.boardGameUseCase = boardGameUseCase;
        this.boardGameSearchUseCase = boardGameSearchUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Lista juegos de mesa", description = "Devuelve una página de juegos de mesa con filtros opcionales por nombre y estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de juegos de mesa encontrada"),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación o filtros inválidos")
    })
    public Page<BoardGameResponse> list(
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ordenación como campo,asc|desc") @RequestParam(defaultValue = "title,asc") String sort,
            @Parameter(description = "Filtro por título (búsqueda parcial, insensible a mayúsculas)") @RequestParam(required = false) @Size(max = 100, message = "La búsqueda no puede superar los 100 caracteres") String name,
            @Parameter(description = "Filtro por estado") @RequestParam(required = false) BoardGameStatus status) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        BoardGameSearchCriteria criteria = new BoardGameSearchCriteria(name, status);
        return boardGameUseCase.search(criteria, pageable).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un juego de mesa por su id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Juego de mesa encontrado"),
            @ApiResponse(responseCode = "404", description = "Juego de mesa no encontrado")
    })
    public BoardGameResponse getById(
            @Parameter(description = "Identificador del juego de mesa") @PathVariable String id) {
        return mapper.toResponse(boardGameUseCase.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crea un juego de mesa")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Juego de mesa creado"),
            @ApiResponse(responseCode = "400", description = "Datos del juego de mesa inválidos")
    })
    public ResponseEntity<BoardGameResponse> create(@Valid @RequestBody BoardGameRequest request, UriComponentsBuilder ucb) {
        var saved = boardGameUseCase.save(mapper.toDomain(request));
        URI location = ucb.path("/api/boardgames/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un juego de mesa existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Juego de mesa actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos del juego de mesa inválidos"),
            @ApiResponse(responseCode = "404", description = "Juego de mesa no encontrado")
    })
    public BoardGameResponse update(
            @Parameter(description = "Identificador del juego de mesa") @PathVariable String id,
            @Valid @RequestBody BoardGameRequest request) {
        return mapper.toResponse(boardGameUseCase.update(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un juego de mesa")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Juego de mesa eliminado"),
            @ApiResponse(responseCode = "404", description = "Juego de mesa no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del juego de mesa") @PathVariable String id) {
        boardGameUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Busca juegos de mesa en el catálogo externo", description = "Busca en BoardGameGeek (JSON primario, XML fallback) por título.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda"),
            @ApiResponse(responseCode = "400", description = "El parámetro 'name' es obligatorio")
    })
    public BoardGameSearchResponse search(
            @Parameter(description = "Título a buscar") @RequestParam("name") @Size(max = 100, message = "La búsqueda no puede superar los 100 caracteres") String name) {
        return new BoardGameSearchResponse(name, boardGameSearchUseCase.search(name));
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