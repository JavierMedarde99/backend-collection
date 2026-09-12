package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;

import com.wikicollection.domain.port.in.DeckUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckCardRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckStatusResponse;

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
@RequestMapping("/api/decks")
@Validated
@Tag(name = "Mazos Commander", description = "Gestión de mazos Commander de Magic: The Gathering")
public class DeckController {

    private final DeckUseCase deckUseCase;
    private final DeckDtoMapper mapper;

    public DeckController(DeckUseCase deckUseCase, DeckDtoMapper mapper) {
        this.deckUseCase = deckUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Lista mazos", description = "Devuelve una página de mazos, opcionalmente filtrada por nombre exacto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de mazos"),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación inválidos")
    })
    public Page<DeckResponse> list(
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ordenación como campo,asc|desc") @RequestParam(defaultValue = "name,asc") String sort,
            @Parameter(description = "Filtro por nombre exacto") @RequestParam(required = false) @Size(max = 100, message = "La búsqueda no puede superar los 100 caracteres") String name) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        var decks = (name == null || name.isBlank())
                ? deckUseCase.findAll(pageable)
                : deckUseCase.findByName(name, pageable);
        return decks.map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un mazo por su id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mazo encontrado"),
            @ApiResponse(responseCode = "404", description = "Mazo no encontrado")
    })
    public DeckResponse getById(
            @Parameter(description = "Identificador del mazo") @PathVariable String id) {
        return mapper.toResponse(deckUseCase.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crea un mazo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mazo creado"),
            @ApiResponse(responseCode = "400", description = "Datos del mazo inválidos")
    })
    public ResponseEntity<DeckResponse> create(@Valid @RequestBody DeckRequest request, UriComponentsBuilder ucb) {
        var saved = deckUseCase.save(mapper.toDomain(request));
        URI location = ucb.path("/api/decks/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un mazo existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mazo actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos del mazo inválidos"),
            @ApiResponse(responseCode = "404", description = "Mazo no encontrado")
    })
    public DeckResponse update(
            @Parameter(description = "Identificador del mazo") @PathVariable String id,
            @Valid @RequestBody DeckRequest request) {
        return mapper.toResponse(deckUseCase.update(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un mazo")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mazo eliminado"),
            @ApiResponse(responseCode = "404", description = "Mazo no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del mazo") @PathVariable String id) {
        deckUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cards")
    @Operation(summary = "Añade una carta al mazo desde Scryfall")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carta añadida"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Mazo o carta no encontrados")
    })
    public DeckResponse addCard(
            @Parameter(description = "Identificador del mazo") @PathVariable String id,
            @Valid @RequestBody DeckCardRequest request) {
        return mapper.toResponse(deckUseCase.addCard(id, request.scryfallId(), request.quantity()));
    }

    @DeleteMapping("/{id}/cards/{scryfallId}")
    @Operation(summary = "Quita una carta del mazo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carta eliminada del mazo"),
            @ApiResponse(responseCode = "400", description = "La carta no está en el mazo"),
            @ApiResponse(responseCode = "404", description = "Mazo no encontrado")
    })
    public DeckResponse removeCard(
            @Parameter(description = "Identificador del mazo") @PathVariable String id,
            @Parameter(description = "Identificador Scryfall de la carta") @PathVariable String scryfallId) {
        return mapper.toResponse(deckUseCase.removeCard(id, scryfallId));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Estado del mazo según reglas Commander")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado calculado"),
            @ApiResponse(responseCode = "404", description = "Mazo no encontrado")
    })
    public DeckStatusResponse status(
            @Parameter(description = "Identificador del mazo") @PathVariable String id) {
        return mapper.toStatusResponse(deckUseCase.getStatusReport(id));
    }

    private Sort buildSort(String sort) {
        String field = "name";
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
