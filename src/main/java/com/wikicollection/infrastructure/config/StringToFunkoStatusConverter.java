package com.wikicollection.infrastructure.config;

import com.wikicollection.domain.model.FunkoStatus;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToFunkoStatusConverter implements Converter<String, FunkoStatus> {

    @Override
    public FunkoStatus convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return FunkoStatus.valueOf(source.trim().toUpperCase());
    }
}
