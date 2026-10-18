package com.wikicollection.infrastructure.config;

import com.wikicollection.domain.model.BoardGameTypeFilter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * El filtro {@code GET /boardgames?type=base|expansion|all} llega en minúsculas,
 * pero el conversor por defecto de Spring hace {@code Enum.valueOf} sin cambiar
 * mayúsculas y respondería 400. Mismo patrón que el resto de
 * {@code StringToXxxConverter} del paquete.
 */
@Component
public class StringToBoardGameTypeFilterConverter implements Converter<String, BoardGameTypeFilter> {

    @Override
    public BoardGameTypeFilter convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return BoardGameTypeFilter.valueOf(source.trim().toUpperCase());
    }
}
