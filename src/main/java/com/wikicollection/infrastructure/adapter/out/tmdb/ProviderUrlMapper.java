package com.wikicollection.infrastructure.adapter.out.tmdb;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ProviderUrlMapper {

    private static final Map<Integer, String> PROVIDER_URLS = Map.ofEntries(
            Map.entry(10, "https://www.netflix.com/search?q={title}"),
            Map.entry(22, "https://www.max.com/search?q={title}"),
            Map.entry(51, "https://www.disneyplus.com/search?q={title}"),
            Map.entry(110, "https://www.primevideo.com/search?q={title}"),
            Map.entry(103, "https://tv.apple.com/search?q={title}"),
            Map.entry(121, "https://play.google.com/store/movies?q={title}"),
            Map.entry(128, "https://www.rakuten.com/search?q={title}"),
            Map.entry(153, "https://www.filmin.com/search?q={title}"),
            Map.entry(136, "https://mubi.com/en/search?q={title}"),
            Map.entry(332, "https://www.paramountplus.com/search?q={title}"),
            Map.entry(368, "https://www.starplus.com/search?q={title}"),
            Map.entry(449, "https://plus.movistar.es/search?q={title}"));

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
