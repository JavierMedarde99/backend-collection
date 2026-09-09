package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;

import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.port.in.MagicCardSearchUseCase;
import com.wikicollection.domain.port.in.MagicCardUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.MagicCardDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.MagicCardRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.MagicCardResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.MagicCardSearchResponse;

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
@RequestMapping("/api/magic")
@Validated
@Tag(name = "Cartas Magic", description = "Gestión del catálogo de cartas Magic: The Gathering")
public class MagicCardController {

    private final MagicCardUseCase magicCardUseCase;
    private final MagicCardSearchUseCase magicCardSearchUseCase;
    private final MagicCardDtoMapper mapper;

    public MagicCardController(MagicCardUseCase magicCardUseCase,
                               MagicCardSearchUseCase magicCardSearchUseCase,
                               MagicCardDtoMapper mapper) {
        this.magicCardUseCase = magicCardUseCase;
        this.magicCardSearchUseCase = magicCardSearchUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Lista cartas Magic", description = "Devuelve una página de cartas de la colección local con filtros opcionales por nombre, rareza, color, tipo y coste de maná convertido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de cartas encontrada"),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación o filtros inválidos")
    })
    public Page<MagicCardResponse> list(
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ordenación como campo,asc|desc") @RequestParam(defaultValue = "name,asc") String sort,
            @Parameter(description = "Filtro por nombre (búsqueda parcial, insensible a mayúsculas)") @RequestParam(required = false) String name,
            @Parameter(description = "Filtro por rareza") @RequestParam(required = false) String rarity,
            @Parameter(description = "Filtro por color") @RequestParam(required = false) String color,
            @Parameter(description = "Filtro por tipo") @RequestParam(required = false) String type,
            @Parameter(description = "Filtro por coste de maná convertido") @RequestParam(required = false) Double convertedManaCost) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        MagicCardSearchCriteria criteria = new MagicCardSearchCriteria(name, rarity, color, type, convertedManaCost);
        return magicCardUseCase.search(criteria, pageable).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una carta Magic por su id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carta encontrada"),
            @ApiResponse(responseCode = "404", description = "Carta no encontrada")
    })
    public MagicCardResponse getById(
            @Parameter(description = "Identificador de la carta") @PathVariable String id) {
        return mapper.toResponse(magicCardUseCase.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crea una carta Magic en la colección")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Carta creada"),
            @ApiResponse(responseCode = "400", description = "Datos de la carta inválidos")
    })
    public ResponseEntity<MagicCardResponse> create(@Valid @RequestBody MagicCardRequest request, UriComponentsBuilder ucb) {
        var saved = magicCardUseCase.save(mapper.toDomain(request));
        URI location = ucb.path("/api/magic/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza una carta Magic existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carta actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos de la carta inválidos"),
            @ApiResponse(responseCode = "404", description = "Carta no encontrada")
    })
    public MagicCardResponse update(
            @Parameter(description = "Identificador de la carta") @PathVariable String id,
            @Valid @RequestBody MagicCardRequest request) {
        return mapper.toResponse(magicCardUseCase.update(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una carta Magic")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Carta eliminada"),
            @ApiResponse(responseCode = "404", description = "Carta no encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador de la carta") @PathVariable String id) {
        magicCardUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Busca cartas Magic en el catálogo externo", description = "Busca en Scryfall por nombre.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda"),
            @ApiResponse(responseCode = "400", description = "El parámetro 'name' es obligatorio")
    })
    public MagicCardSearchResponse search(
            @Parameter(description = "Nombre a buscar") @RequestParam("name") String name) {
        return new MagicCardSearchResponse(name, magicCardSearchUseCase.search(name));
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
