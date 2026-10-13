package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;

import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.FunkoSearchCriteria;
import com.wikicollection.domain.model.FunkoStatus;
import com.wikicollection.domain.port.in.FunkoSearchUseCase;
import com.wikicollection.domain.port.in.FunkoUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.FunkoCategoryResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.FunkoDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.FunkoRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.FunkoResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.FunkoSearchResponse;
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
@RequestMapping("/api/v1/funkos")
@Validated
@Tag(name = "Funko", description = "Gestión del catálogo de Funko Pop")
public class FunkoController {

    private final FunkoUseCase funkoUseCase;
    private final FunkoSearchUseCase funkoSearchUseCase;
    private final FunkoDtoMapper mapper;

    private final ResponseVisibility visibility;

    public FunkoController(FunkoUseCase funkoUseCase,
                           FunkoSearchUseCase funkoSearchUseCase,
                           FunkoDtoMapper mapper,
                           ResponseVisibility visibility) {
        this.funkoUseCase = funkoUseCase;
        this.funkoSearchUseCase = funkoSearchUseCase;
        this.mapper = mapper;
        this.visibility = visibility;
    }

    @GetMapping
    @Operation(summary = "Lista funkos", description = "Devuelve una página de funkos con filtros opcionales por nombre y estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de funkos encontrada"),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación o filtros inválidos")
    })
    public Page<FunkoResponse> list(
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ordenación como campo,asc|desc") @RequestParam(defaultValue = "name,asc") String sort,
            @Parameter(description = "Filtro por nombre (búsqueda parcial, insensible a mayúsculas)") @RequestParam(required = false) String name,
            @Parameter(description = "Filtro por estado") @RequestParam(required = false) FunkoStatus status,
            @Parameter(description = "Filtro por propiedad: mine|other|all") @RequestParam(defaultValue = "mine") String owner,
            @CurrentUser String viewerId) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        FunkoSearchCriteria criteria = new FunkoSearchCriteria(name, status, null, null);
        return funkoUseCase.search(criteria, pageable, owner, viewerId).map(funko -> {
            var response = mapper.toResponse(funko);
            return visibility.canSeePrivate(funko.getOwnerId(), CollectionType.FUNKOS) ? response : response.withoutPrivate();
        });
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un funko por su id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funko encontrado"),
            @ApiResponse(responseCode = "404", description = "Funko no encontrado")
    })
    public FunkoResponse getById(
            @Parameter(description = "Identificador del funko") @PathVariable String id) {
        var funko = funkoUseCase.findById(id);
        var response = mapper.toResponse(funko);
        return visibility.canSeePrivate(funko.getOwnerId(), CollectionType.FUNKOS) ? response : response.withoutPrivate();
    }

    @PostMapping
    @Operation(summary = "Añade un funko a la colección")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Funko creado"),
            @ApiResponse(responseCode = "400", description = "Datos del funko inválidos"),
            @ApiResponse(responseCode = "409", description = "El externalId ya existe")
    })
    public ResponseEntity<FunkoResponse> create(@Valid @RequestBody FunkoRequest request,
                                                @CurrentUser String currentUserId,
                                                UriComponentsBuilder ucb) {
        var saved = funkoUseCase.save(mapper.toDomain(request), currentUserId);
        URI location = ucb.path("/api/v1/funkos/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un funko existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Funko actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos del funko inválidos"),
            @ApiResponse(responseCode = "404", description = "Funko no encontrado"),
            @ApiResponse(responseCode = "409", description = "El externalId ya existe")
    })
    public FunkoResponse update(
            @Parameter(description = "Identificador del funko") @PathVariable String id,
            @Valid @RequestBody FunkoRequest request, @CurrentUser String currentUserId) {
        return mapper.toResponse(funkoUseCase.update(id, mapper.toDomain(request), currentUserId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un funko")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Funko eliminado"),
            @ApiResponse(responseCode = "404", description = "Funko no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del funko") @PathVariable String id,
            @CurrentUser String currentUserId) {
        funkoUseCase.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Busca funkos en el catálogo externo", description = "Busca en HobbyQuantHQ por nombre (mínimo 2 caracteres).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda"),
            @ApiResponse(responseCode = "400", description = "El parámetro 'name' es obligatorio o demasiado corto")
    })
    public Page<FunkoSearchResponse> search(
            @Parameter(description = "Nombre a buscar (mínimo 2 caracteres)") @RequestParam(required = false) String name,
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Indica 'name' para buscar");
        }
        return funkoSearchUseCase.search(name, PageRequest.of(page, size)).map(mapper::toSearchResponse);
    }

    @GetMapping("/categories")
    @Operation(summary = "Lista las categorías del catálogo externo")
    @ApiResponse(responseCode = "200", description = "Categorías de HobbyQuantHQ")
    public List<FunkoCategoryResponse> categories() {
        return funkoSearchUseCase.listCategories().stream()
                .map(mapper::toCategoryResponse)
                .toList();
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
