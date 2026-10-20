package com.wikicollection.infrastructure.config;

import com.wikicollection.domain.model.GameTypeFilter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * El filtro {@code GET /games?type=base|dlc|all} llega en minúsculas, pero el conversor por
 * defecto de Spring hace {@code Enum.valueOf} sin cambiar mayúsculas y respondería 400. Mismo
 * patrón que el resto de {@code StringToXxxConverter} del paquete.
 */
@Component
public class StringToGameTypeFilterConverter implements Converter<String, GameTypeFilter> {

    @Override
    public GameTypeFilter convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return GameTypeFilter.valueOf(source.trim().toUpperCase());
    }
}
