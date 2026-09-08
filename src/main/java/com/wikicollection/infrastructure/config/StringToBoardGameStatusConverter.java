package com.wikicollection.infrastructure.config;

import com.wikicollection.domain.model.BoardGameStatus;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToBoardGameStatusConverter implements Converter<String, BoardGameStatus> {

    @Override
    public BoardGameStatus convert(String source) {
        return BoardGameStatus.valueOf(source.trim().toUpperCase());
    }
}