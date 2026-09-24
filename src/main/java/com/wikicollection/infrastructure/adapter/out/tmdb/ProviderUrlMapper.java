package com.wikicollection.infrastructure.adapter.out.tmdb;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ProviderUrlMapper {

    /**
     * IDs verificados contra la doc de TMDB y TMDbLib (WatchProvider.cs, 2026-08-19).
     * Los IDs no listados (Filmin, Movistar Plus, Star+, ...) quedan sin deep link
     * a propósito: el frontend muestra solo el logo hasta confirmar su ID real en ES.
     */
    private static final Map<Integer, String> PROVIDER_URLS = Map.ofEntries(
            Map.entry(8, "https://www.netflix.com/search?q={title}"),
            Map.entry(175, "https://www.netflix.com/search?q={title}"),
            Map.entry(1796, "https://www.netflix.com/search?q={title}"),
            Map.entry(9, "https://www.primevideo.com/search?q={title}"),
            Map.entry(10, "https://www.primevideo.com/search?q={title}"),
            Map.entry(119, "https://www.primevideo.com/search?q={title}"),
            Map.entry(122, "https://www.disneyplus.com/search?q={title}"),
            Map.entry(337, "https://www.disneyplus.com/search?q={title}"),
            Map.entry(1899, "https://www.max.com/search?q={title}"),
            Map.entry(2, "https://tv.apple.com/search?q={title}"),
            Map.entry(350, "https://tv.apple.com/search?q={title}"),
            Map.entry(3, "https://play.google.com/store/movies?q={title}"),
            Map.entry(35, "https://www.rakuten.com/search?q={title}"),
            Map.entry(11, "https://mubi.com/en/search?q={title}"),
            Map.entry(531, "https://www.paramountplus.com/search?q={title}"),
            Map.entry(2303, "https://www.paramountplus.com/search?q={title}"),
            Map.entry(2616, "https://www.paramountplus.com/search?q={title}"));

    public String buildDeepLink(Integer providerId, String title) {
        String template = providerId == null ? null : PROVIDER_URLS.get(providerId);
        if (template == null || title == null || title.isBlank()) {
            return null;
        }
        String encoded = URLEncoder.encode(title, StandardCharsets.UTF_8).replace("+", "%20");
        return template.replace("{title}", encoded);
    }

    public String buildLogoUrl(String logoPath) {
        if (logoPath == null || logoPath.isBlank()) {
            return null;
        }
        return "https://image.tmdb.org/t/p/original" + logoPath;
    }
}
