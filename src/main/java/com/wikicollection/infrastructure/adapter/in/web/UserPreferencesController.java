package com.wikicollection.infrastructure.adapter.in.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.CollectionVisibility;
import com.wikicollection.domain.port.in.UserPreferencesUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.ActiveCollectionsRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.CollectionVisibilityRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.UserPreferencesRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.UserPreferencesResponse;
import com.wikicollection.infrastructure.config.CurrentUser;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/preferences")
@Validated
@Tag(name = "Preferencias", description = "Preferencias de colecciones del usuario autenticado")
public class UserPreferencesController {

    private final UserPreferencesUseCase preferencesUseCase;

    public UserPreferencesController(UserPreferencesUseCase preferencesUseCase) {
        this.preferencesUseCase = preferencesUseCase;
    }

    @GetMapping
    @Operation(summary = "Obtiene las preferencias del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferencias encontradas"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public UserPreferencesResponse get(@CurrentUser String currentUserId) {
        return UserPreferencesResponse.from(preferencesUseCase.getPreferences(currentUserId));
    }

    @PutMapping
    @Operation(summary = "Reemplaza las preferencias completas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferencias actualizadas"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public UserPreferencesResponse update(@Valid @RequestBody UserPreferencesRequest request,
                                          @CurrentUser String currentUserId) {
        return UserPreferencesResponse.from(preferencesUseCase.updatePreferences(
                currentUserId, request.activeCollections(), parseVisibility(request.collectionVisibility())));
    }

    @PatchMapping("/active-collections")
    @Operation(summary = "Activa o desactiva colecciones")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Colecciones actualizadas"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public UserPreferencesResponse patchActive(@Valid @RequestBody ActiveCollectionsRequest request,
                                               @CurrentUser String currentUserId) {
        return UserPreferencesResponse.from(
                preferencesUseCase.setActiveCollections(currentUserId, request.collections()));
    }

    @PatchMapping("/collection-visibility")
    @Operation(summary = "Cambia la visibilidad de colecciones")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visibilidad actualizada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public UserPreferencesResponse patchVisibility(@Valid @RequestBody CollectionVisibilityRequest request,
                                                   @CurrentUser String currentUserId) {
        return UserPreferencesResponse.from(
                preferencesUseCase.setCollectionVisibility(currentUserId, parseVisibility(request.visibility())));
    }

    @GetMapping("/active-collections")
    @Operation(summary = "Obtiene solo las colecciones activas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Colecciones activas"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public List<CollectionType> active(@CurrentUser String currentUserId) {
        return preferencesUseCase.getActiveCollections(currentUserId);
    }

    private static Map<String, CollectionVisibility> parseVisibility(Map<String, String> visibility) {
        return visibility.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> CollectionVisibility.valueOf(entry.getValue())));
    }
}
