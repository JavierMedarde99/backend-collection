package com.wikicollection.config;

import com.wikicollection.model.BookStatus;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToBookStatusConverter implements Converter<String, BookStatus> {

    @Override
    public BookStatus convert(String source) {
        return BookStatus.valueOf(source.trim().toUpperCase());
    }
}