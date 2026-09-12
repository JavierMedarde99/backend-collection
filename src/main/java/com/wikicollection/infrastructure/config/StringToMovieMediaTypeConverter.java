package com.wikicollection.infrastructure.config;

import com.wikicollection.domain.model.MovieMediaType;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToMovieMediaTypeConverter implements Converter<String, MovieMediaType> {

    @Override
    public MovieMediaType convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return MovieMediaType.valueOf(source.trim().toUpperCase());
    }
}
