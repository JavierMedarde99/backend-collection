package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;

import com.wikicollection.domain.model.GameSearchResult;
import com.wikicollection.domain.model.GameStatus;
import com.wikicollection.domain.port.in.GameSearchUseCase;
import com.wikicollection.domain.port.in.GameUseCase;
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

@RestController
@RequestMapping("/api/games")
@Validated
public class GameController {

    private final GameUseCase gameUseCase;
    private final GameSearchUseCase gameSearchUseCase;
    private final GameDtoMapper mapper;

    public GameController(GameUseCase gameUseCase, GameSearchUseCase gameSearchUseCase, GameDtoMapper mapper) {
        this.gameUseCase = gameUseCase;
        this.gameSearchUseCase = gameSearchUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public Page<GameResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title,asc") String sort,
            @RequestParam(required = false) GameStatus status) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        Page<GameResponse> responsePage = status == null
                ? gameUseCase.findAll(pageable).map(mapper::toResponse)
                : gameUseCase.findByStatus(status, pageable).map(mapper::toResponse);
        return responsePage;
    }

    @GetMapping("/{id}")
    public GameResponse getById(@PathVariable String id) {
        return mapper.toResponse(gameUseCase.findById(id));
    }

    @PostMapping
    public ResponseEntity<GameResponse> create(@Valid @RequestBody GameRequest request, UriComponentsBuilder ucb) {
        var saved = gameUseCase.save(mapper.toDomain(request));
        URI location = ucb.path("/api/games/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    public GameResponse update(@PathVariable String id, @Valid @RequestBody GameRequest request) {
        return mapper.toResponse(gameUseCase.update(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        gameUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<GameSearchResult> search(@RequestParam("name") String name) {
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