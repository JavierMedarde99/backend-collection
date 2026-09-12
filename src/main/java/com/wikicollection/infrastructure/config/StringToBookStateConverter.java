package com.wikicollection.infrastructure.config;

import com.wikicollection.domain.model.BookState;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToBookStateConverter implements Converter<String, BookState> {

    @Override
    public BookState convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return BookState.valueOf(source.trim().toUpperCase());
    }
}
