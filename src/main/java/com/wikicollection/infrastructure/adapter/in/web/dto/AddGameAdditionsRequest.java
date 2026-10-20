package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.wikicollection.domain.model.GameAdditionSeed;
import com.wikicollection.domain.model.GameStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body de {@code POST /games/{id}/additions}: las adiciones de RAWG a dar de
 * alta, o bien por sus externalIds (forma legada: todas con el mismo estado y
 * sin datos de adquisición), o bien una a una con su estado y sus datos. El
 * límite de 40 es el de la página de adiciones de RAWG. Hay que enviar
 * exactamente una de las dos formas.
 */
public record AddGameAdditionsRequest(
        @Size(max = 40, message = "No se pueden añadir más de 40 adiciones por petición")
        List<@NotBlank(message = "El externalId no puede estar en blanco") String> externalIds,
        GameStatus status,
        @Size(max = 40, message = "No se pueden añadir más de 40 adiciones por petición")
        @Valid List<AddGameAdditionItem> additions) {

    /**
     * Una adición a dar de alta con el estado en el que nace. Fuera de WISHLIST
     * exige fecha de obtención y precio; PLAYING exige además fecha de inicio y
     * COMPLETED, fecha de fin.
     */
    public record AddGameAdditionItem(
            @NotBlank(message = "El externalId no puede estar en blanco") String externalId,
            @NotNull(message = "El estado es obligatorio") GameStatus status,
            Boolean link,
            LocalDate acquisitionDate,
            @DecimalMin(value = "0", message = "El precio de adquisición no puede ser negativo")
            @Digits(integer = 8, fraction = 2, message = "El precio admite como máximo 8 dígitos y 2 decimales")
            BigDecimal acquisitionPrice,
            LocalDate dateAdded,
            LocalDate dateCompleted) {
    }

    /**
     * Normaliza el body a las semillas de alta. Exige exactamente una de las dos
     * formas y, por adición, los datos que su estado requiere (400 en caso
     * contrario, vía {@link IllegalArgumentException}).
     */
    public List<GameAdditionSeed> toSeeds() {
        if ((externalIds == null) == (additions == null)) {
            throw new IllegalArgumentException("Envía exactamente uno de los campos 'externalIds' o 'additions'");
        }
        if (externalIds != null) {
            if (status == null) {
                throw new IllegalArgumentException("El estado es obligatorio");
            }
            return externalIds.stream()
                    .map(externalId -> new GameAdditionSeed(externalId, status, null, null, null, null, false))
                    .toList();
        }
        List<GameAdditionSeed> seeds = new ArrayList<>();
        for (AddGameAdditionItem item : additions) {
            if (item.status() == null) {
                throw new IllegalArgumentException("El estado es obligatorio");
            }
            if (Boolean.TRUE.equals(item.link())) {
                // Ya está en la colección: solo se enlaza y conserva sus datos.
                seeds.add(new GameAdditionSeed(item.externalId(), item.status(), null, null, null, null, true));
                continue;
            }
            if (item.status() == GameStatus.WISHLIST) {
                seeds.add(new GameAdditionSeed(item.externalId(), item.status(), null, null, null, null, false));
                continue;
            }
            if (item.acquisitionDate() == null) {
                throw new IllegalArgumentException(
                        "La adición " + item.externalId() + " necesita fecha de obtención");
            }
            if (item.acquisitionPrice() == null) {
                throw new IllegalArgumentException(
                        "La adición " + item.externalId() + " necesita precio de adquisición");
            }
            LocalDate dateAdded = null;
            if (item.status() == GameStatus.PLAYING || item.status() == GameStatus.COMPLETED) {
                if (item.dateAdded() == null) {
                    throw new IllegalArgumentException(
                            "La adición " + item.externalId() + " necesita fecha de inicio");
                }
                dateAdded = item.dateAdded();
            }
            LocalDate dateCompleted = null;
            if (item.status() == GameStatus.COMPLETED) {
                if (item.dateCompleted() == null) {
                    throw new IllegalArgumentException(
                            "La adición " + item.externalId() + " necesita fecha de fin");
                }
                dateCompleted = item.dateCompleted();
            }
            seeds.add(new GameAdditionSeed(item.externalId(), item.status(), item.acquisitionDate(),
                    item.acquisitionPrice(), dateAdded, dateCompleted, false));
        }
        return seeds;
    }
}
