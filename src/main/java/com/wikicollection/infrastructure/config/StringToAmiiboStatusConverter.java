package com.wikicollection.infrastructure.config;

import com.wikicollection.domain.model.AmiiboStatus;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToAmiiboStatusConverter implements Converter<String, AmiiboStatus> {

    @Override
    public AmiiboStatus convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return AmiiboStatus.valueOf(source.trim().toUpperCase());
    }
}
