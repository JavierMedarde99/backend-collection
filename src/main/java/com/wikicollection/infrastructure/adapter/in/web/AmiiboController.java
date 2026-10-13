package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;

import com.wikicollection.domain.model.AmiiboSearchCriteria;
import com.wikicollection.domain.model.AmiiboStatus;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.port.in.AmiiboSearchUseCase;
import com.wikicollection.domain.port.in.AmiiboUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.AmiiboDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.AmiiboRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.AmiiboResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.AmiiboSearchResponse;
import com.wikicollection.infrastructure.config.CurrentUser;

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
@RequestMapping("/api/v1/amiibos")
@Validated
@Tag(name = "Amiibo", description = "Gestión del catálogo de amiibo")
public class AmiiboController {

    private final AmiiboUseCase amiiboUseCase;
    private final AmiiboSearchUseCase amiiboSearchUseCase;
    private final AmiiboDtoMapper mapper;

    private final ResponseVisibility visibility;

    public AmiiboController(AmiiboUseCase amiiboUseCase,
                            AmiiboSearchUseCase amiiboSearchUseCase,
                            AmiiboDtoMapper mapper,
                            ResponseVisibility visibility) {
        this.amiiboUseCase = amiiboUseCase;
        this.amiiboSearchUseCase = amiiboSearchUseCase;
        this.mapper = mapper;
        this.visibility = visibility;
    }

    @GetMapping
    @Operation(summary = "Lista amiibo", description = "Devuelve una página de amiibo con filtros opcionales por nombre y estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de amiibo encontrada"),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación o filtros inválidos")
    })
    public Page<AmiiboResponse> list(
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ordenación como campo,asc|desc") @RequestParam(defaultValue = "name,asc") String sort,
            @Parameter(description = "Filtro por nombre (búsqueda parcial, insensible a mayúsculas)") @RequestParam(required = false) String name,
            @Parameter(description = "Filtro por estado") @RequestParam(required = false) AmiiboStatus status,
            @Parameter(description = "Filtro por propiedad: mine|other|all") @RequestParam(defaultValue = "mine") String owner,
            @CurrentUser String viewerId) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        AmiiboSearchCriteria criteria = new AmiiboSearchCriteria(name, status, null, null);
        return amiiboUseCase.search(criteria, pageable, owner, viewerId).map(amiibo -> {
            var response = mapper.toResponse(amiibo);
            return visibility.canSeePrivate(amiibo.getOwnerId(), CollectionType.AMIIBOS) ? response : response.withoutPrivate();
        });
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un amiibo por su id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Amiibo encontrado"),
            @ApiResponse(responseCode = "404", description = "Amiibo no encontrado")
    })
    public AmiiboResponse getById(
            @Parameter(description = "Identificador del amiibo") @PathVariable String id) {
        var amiibo = amiiboUseCase.findById(id);
        var response = mapper.toResponse(amiibo);
        return visibility.canSeePrivate(amiibo.getOwnerId(), CollectionType.AMIIBOS) ? response : response.withoutPrivate();
    }

    @PostMapping
    @Operation(summary = "Añade un amiibo a la colección")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Amiibo creado"),
            @ApiResponse(responseCode = "400", description = "Datos del amiibo inválidos"),
            @ApiResponse(responseCode = "409", description = "El externalId ya existe")
    })
    public ResponseEntity<AmiiboResponse> create(@Valid @RequestBody AmiiboRequest request,
                                                 @CurrentUser String currentUserId,
                                                 UriComponentsBuilder ucb) {
        var saved = amiiboUseCase.save(mapper.toDomain(request), currentUserId);
        URI location = ucb.path("/api/v1/amiibos/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un amiibo existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Amiibo actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos del amiibo inválidos"),
            @ApiResponse(responseCode = "404", description = "Amiibo no encontrado"),
            @ApiResponse(responseCode = "409", description = "El externalId ya existe")
    })
    public AmiiboResponse update(
            @Parameter(description = "Identificador del amiibo") @PathVariable String id,
            @Valid @RequestBody AmiiboRequest request, @CurrentUser String currentUserId) {
        return mapper.toResponse(amiiboUseCase.update(id, mapper.toDomain(request), currentUserId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un amiibo")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Amiibo eliminado"),
            @ApiResponse(responseCode = "404", description = "Amiibo no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del amiibo") @PathVariable String id,
            @CurrentUser String currentUserId) {
        amiiboUseCase.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Busca amiibo en el catálogo externo", description = "Busca en AmiiboAPI por nombre (coincidencia parcial).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda"),
            @ApiResponse(responseCode = "400", description = "El parámetro 'name' es obligatorio")
    })
    public Page<AmiiboSearchResponse> search(
            @Parameter(description = "Nombre a buscar") @RequestParam(required = false) String name,
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Indica 'name' para buscar");
        }
        return amiiboSearchUseCase.search(name, PageRequest.of(page, size)).map(mapper::toSearchResponse);
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
