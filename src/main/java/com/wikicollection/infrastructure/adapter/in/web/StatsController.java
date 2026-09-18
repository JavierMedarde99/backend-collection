package com.wikicollection.infrastructure.adapter.in.web;

import com.wikicollection.domain.port.in.StatsUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.GlobalStatsResponse;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/stats")
@Validated
@Tag(name = "Estadísticas", description = "Conteos globales por colección")
public class StatsController {

    private final StatsUseCase statsUseCase;

    public StatsController(StatsUseCase statsUseCase) {
        this.statsUseCase = statsUseCase;
    }

    @GetMapping
    @Operation(summary = "Cuántos items hay en cada colección (global)")
    @ApiResponse(responseCode = "200", description = "Conteos por colección")
    public GlobalStatsResponse global() {
        return new GlobalStatsResponse(statsUseCase.getGlobalCounts());
    }
}
