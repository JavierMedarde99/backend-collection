package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;

import com.wikicollection.domain.port.in.DeckUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckCardRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckStatusResponse;

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
@RequestMapping("/api/v1/decks")
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
    @Operation(summary = "Lista mazos", description = "Devuelve todos los mazos o filtra por nombre exacto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de mazos")
    })
    public List<DeckResponse> list(
            @Parameter(description = "Filtro por nombre exacto") @RequestParam(required = false) String name) {
        var decks = (name == null || name.isBlank()) ? deckUseCase.findAll() : deckUseCase.findByName(name);
        return decks.stream().map(mapper::toResponse).toList();
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
        URI location = ucb.path("/api/v1/decks/{id}").buildAndExpand(saved.getId()).toUri();
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
}
