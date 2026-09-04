package com.wikicollection.infrastructure.config;

import com.wikicollection.domain.model.GameStatus;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToGameStatusConverter implements Converter<String, GameStatus> {

    @Override
    public GameStatus convert(String source) {
        return GameStatus.valueOf(source.trim().toUpperCase());
    }
}