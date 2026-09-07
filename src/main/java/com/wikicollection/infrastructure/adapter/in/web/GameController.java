package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;

import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.model.GameSearchResult;
import com.wikicollection.domain.model.GameStatus;
import com.wikicollection.domain.port.in.GameAchievementsUseCase;
import com.wikicollection.domain.port.in.GameSearchUseCase;
import com.wikicollection.domain.port.in.GameUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.GameAchievementMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.GameAchievementResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.GameDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.GameRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.GameResponse;

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
@RequestMapping("/api/games")
@Validated
@Tag(name = "Juegos", description = "Gestión del catálogo de juegos")
public class GameController {

    private final GameUseCase gameUseCase;
    private final GameSearchUseCase gameSearchUseCase;
    private final GameAchievementsUseCase gameAchievementsUseCase;
    private final GameDtoMapper mapper;
    private final GameAchievementMapper achievementMapper;

    public GameController(GameUseCase gameUseCase, GameSearchUseCase gameSearchUseCase,
            GameAchievementsUseCase gameAchievementsUseCase, GameDtoMapper mapper,
            GameAchievementMapper achievementMapper) {
        this.gameUseCase = gameUseCase;
        this.gameSearchUseCase = gameSearchUseCase;
        this.gameAchievementsUseCase = gameAchievementsUseCase;
        this.mapper = mapper;
        this.achievementMapper = achievementMapper;
    }

    @GetMapping
    @Operation(summary = "Lista juegos", description = "Devuelve una página de juegos con filtros opcionales por nombre, plataforma y estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de juegos encontrada"),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación o filtros inválidos")
    })
    public Page<GameResponse> list(
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ordenación como campo,asc|desc") @RequestParam(defaultValue = "title,asc") String sort,
            @Parameter(description = "Filtro por título (búsqueda parcial, insensible a mayúsculas)") @RequestParam(required = false) String name,
            @Parameter(description = "Filtro por plataforma") @RequestParam(required = false) GamePlatform platform,
            @Parameter(description = "Filtro por estado") @RequestParam(required = false) GameStatus status) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        GameSearchCriteria criteria = new GameSearchCriteria(name, platform, status);
        return gameUseCase.search(criteria, pageable).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un juego por su id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Juego encontrado"),
            @ApiResponse(responseCode = "404", description = "Juego no encontrado")
    })
    public GameResponse getById(
            @Parameter(description = "Identificador del juego") @PathVariable String id) {
        return mapper.toResponse(gameUseCase.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crea un juego")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Juego creado"),
            @ApiResponse(responseCode = "400", description = "Datos del juego inválidos")
    })
    public ResponseEntity<GameResponse> create(@Valid @RequestBody GameRequest request, UriComponentsBuilder ucb) {
        boolean obtainPlatinum = Boolean.TRUE.equals(request.obtainPlatinum());
        var saved = gameUseCase.save(mapper.toDomain(request), obtainPlatinum);
        URI location = ucb.path("/api/games/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un juego existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Juego actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos del juego inválidos"),
            @ApiResponse(responseCode = "404", description = "Juego no encontrado")
    })
    public GameResponse update(
            @Parameter(description = "Identificador del juego") @PathVariable String id,
            @Valid @RequestBody GameRequest request) {
        boolean obtainPlatinum = Boolean.TRUE.equals(request.obtainPlatinum());
        return mapper.toResponse(gameUseCase.update(id, mapper.toDomain(request), obtainPlatinum));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un juego")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Juego eliminado"),
            @ApiResponse(responseCode = "404", description = "Juego no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del juego") @PathVariable String id) {
        gameUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/achievements")
    @Operation(summary = "Obtiene los logros de un juego de Steam", description = "Combina el esquema de logros del juego con el progreso del jugador indicado por steamaId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logros obtenidos"),
            @ApiResponse(responseCode = "400", description = "Falta steamId o el juego no está vinculado a Steam"),
            @ApiResponse(responseCode = "404", description = "Juego no encontrado")
    })
    public List<GameAchievementResponse> getAchievements(
            @Parameter(description = "Identificador del juego") @PathVariable String id,
            @Parameter(description = "SteamID del jugador") @RequestParam("steamId") String steamId) {
        return gameAchievementsUseCase.getAchievements(id, steamId).stream()
                .map(achievementMapper::toResponse)
                .toList();
    }

    @GetMapping("/search")
    @Operation(summary = "Busca juegos en el catálogo externo", description = "Busca en RAWG y FreeToGame por título.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultados de búsqueda"),
            @ApiResponse(responseCode = "400", description = "El parámetro 'name' es obligatorio"),
            @ApiResponse(responseCode = "502", description = "El catálogo externo devolvió un error"),
            @ApiResponse(responseCode = "503", description = "El catálogo externo no está disponible")
    })
    public List<GameSearchResult> search(
            @Parameter(description = "Título a buscar") @RequestParam("name") String name) {
        return gameSearchUseCase.search(name);
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