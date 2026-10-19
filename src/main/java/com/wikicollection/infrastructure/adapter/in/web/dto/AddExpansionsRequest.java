package com.wikicollection.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.wikicollection.domain.model.BoardGameExpansionSeed;
import com.wikicollection.domain.model.BoardGameStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body de {@code POST /boardgames/{id}/expansions}: las expansiones de BGG a dar
 * de alta, o bien por sus bggIds (forma legada: todas en OWNED, sin precio y con
 * la fecha de hoy), o bien una a una con el estado en el que nacen. El límite de
 * 20 es el de la propia API de BGG por llamada. Hay que enviar exactamente una
 * de las dos formas.
 */
public record AddExpansionsRequest(
        @Size(max = 20, message = "No se pueden añadir más de 20 expansiones por petición")
        List<@NotBlank(message = "El bggId no puede estar en blanco") String> bggIds,
        @Size(max = 20, message = "No se pueden añadir más de 20 expansiones por petición")
        @Valid List<AddExpansionItem> expansions) {

    /**
     * Una expansión a dar de alta con el estado en el que nace. En posesión
     * (OWNED) exige precio de adquisición y fecha de adición; en lista de deseos
     * (WISHLIST) nace sin precio ni fecha.
     */
    public record AddExpansionItem(
            @NotBlank(message = "El bggId no puede estar en blanco") String bggId,
            @NotNull(message = "El estado es obligatorio") BoardGameStatus status,
            @DecimalMin(value = "0.0", message = "El precio mínimo es 0") BigDecimal acquisitionPrice,
            LocalDate dateAdded) {
    }

    /**
     * Normaliza el body a las semillas de alta. Exige exactamente una de las dos
     * formas y, por expansión, los datos que su estado requiere (400 en caso
     * contrario, vía {@link IllegalArgumentException}).
     */
    public List<BoardGameExpansionSeed> toSeeds() {
        if ((bggIds == null) == (expansions == null)) {
            throw new IllegalArgumentException("Envía exactamente uno de los campos 'bggIds' o 'expansions'");
        }
        if (bggIds != null) {
            return bggIds.stream()
                    .filter(bggId -> bggId != null && !bggId.isBlank())
                    .map(bggId -> new BoardGameExpansionSeed(bggId, BoardGameStatus.OWNED, null, null))
                    .toList();
        }
        List<BoardGameExpansionSeed> seeds = new ArrayList<>();
        for (AddExpansionItem item : expansions) {
            if (item.status() == BoardGameStatus.OWNED && item.acquisitionPrice() == null) {
                throw new IllegalArgumentException(
                        "La expansión " + item.bggId() + " está en posesión y necesita precio de adquisición");
            }
            if (item.status() == BoardGameStatus.OWNED && item.dateAdded() == null) {
                throw new IllegalArgumentException(
                        "La expansión " + item.bggId() + " está en posesión y necesita fecha de adición");
            }
            seeds.add(new BoardGameExpansionSeed(item.bggId(), item.status(), item.acquisitionPrice(), item.dateAdded()));
        }
        return seeds;
    }
}
