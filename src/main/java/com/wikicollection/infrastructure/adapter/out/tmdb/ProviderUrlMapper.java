package com.wikicollection.infrastructure.adapter.out.tmdb;

import org.springframework.stereotype.Component;

@Component
public class ProviderUrlMapper {

    public String buildLogoUrl(String logoPath) {
        if (logoPath == null || logoPath.isBlank()) {
            return null;
        }
        return "https://image.tmdb.org/t/p/original" + logoPath;
    }
}
