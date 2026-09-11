package com.wikicollection.infrastructure.config;

import com.wikicollection.domain.model.MovieStatus;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToMovieStatusConverter implements Converter<String, MovieStatus> {

    @Override
    public MovieStatus convert(String source) {
        return MovieStatus.valueOf(source.trim().toUpperCase());
    }
}
