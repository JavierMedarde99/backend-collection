package com.wikicollection.infrastructure.adapter.out.bgg.mapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.wikicollection.domain.model.BoardGameExpansionInfo;
import com.wikicollection.domain.model.BoardGameSearchResult;
import com.wikicollection.domain.model.BoardGameType;

import org.springframework.stereotype.Component;

@Component
public class BoardGameXmlMapper {

    private static final String EXTERNAL_SOURCE = "BGG";
    private static final String EXPANSION_LINK = "boardgameexpansion";

    private final XmlMapper xmlMapper;

    public BoardGameXmlMapper() {
        this.xmlMapper = new XmlMapper();
    }

    public List<BoardGameSearchResult> map(String xml) {
        if (xml == null || xml.isBlank()) {
            return List.of();
        }
        try {
            BggXmlItems items = xmlMapper.readValue(xml, BggXmlItems.class);
            if (items == null || items.items == null) {
                return List.of();
            }
            return items.items.stream()
                    .map(this::toResult)
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo parsear la respuesta XML de BGG", e);
        }
    }

    public Map<String, BggXmlItem> mapThing(String xml) {
        if (xml == null || xml.isBlank()) {
            return Map.of();
        }
        try {
            BggXmlItems items = xmlMapper.readValue(xml, BggXmlItems.class);
            if (items == null || items.items == null) {
                return Map.of();
            }
            return items.items.stream()
                    .collect(Collectors.toMap(item -> item.id, item -> item, (a, b) -> b));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo parsear la respuesta XML thing de BGG", e);
        }
    }

    public BoardGameSearchResult enrich(BoardGameSearchResult result, BggXmlItem thing) {
        if (thing == null) {
            return result;
        }
        return new BoardGameSearchResult(
                result.bggId(),
                result.title(),
                thing.description,
                result.yearPublished() != null ? result.yearPublished() : thing.yearPublished != null ? thing.yearPublished.value : null,
                thing.minPlayers != null ? thing.minPlayers.value : null,
                thing.maxPlayers != null ? thing.maxPlayers.value : null,
                thing.minPlaytime != null ? thing.minPlaytime.value : null,
                thing.maxPlaytime != null ? thing.maxPlaytime.value : null,
                extractFirstLink(thing.links, "boardgamepublisher"),
                extractLinkValues(thing.links, "boardgamedesigner"),
                extractLinkValues(thing.links, "boardgamecategory"),
                extractLinkValues(thing.links, "boardgamemechanic"),
                result.imageUrl() != null ? result.imageUrl() : thing.image,
                result.thumbnailUrl() != null ? result.thumbnailUrl() : thing.thumbnail,
                thing.statistics != null && thing.statistics.ratings != null
                        && thing.statistics.ratings.average != null ? thing.statistics.ratings.average.value : null,
                result.externalSource());
    }

    /**
     * Ids de las expansiones que cuelgan de este item: los links
     * {@code boardgameexpansion} <b>sin</b> {@code inbound}. Leerlos sin filtrar
     * invierte la relación, porque en la ficha de una expansión sus links
     * apuntan hacia su base y llevan {@code inbound="true"}.
     */
    public List<String> expansionIdsOf(BggXmlItem item) {
        if (item == null || item.links == null) {
            return List.of();
        }
        return item.links.stream()
                .filter(link -> EXPANSION_LINK.equals(link.type))
                .filter(link -> !isInbound(link))
                .map(link -> link.id)
                .filter(id -> id != null)
                .toList();
    }

    /**
     * Ids de los juegos base de los que depende este item: los links
     * {@code boardgameexpansion} con {@code inbound="true"}.
     */
    public List<String> baseIdsOf(BggXmlItem item) {
        if (item == null || item.links == null) {
            return List.of();
        }
        return item.links.stream()
                .filter(link -> EXPANSION_LINK.equals(link.type))
                .filter(link -> isInbound(link))
                .map(link -> link.id)
                .filter(id -> id != null)
                .toList();
    }

    public BoardGameExpansionInfo toExpansionInfo(BggXmlItem item) {
        if (item == null) {
            return null;
        }
        // Mismo bloque de campos que rellena la búsqueda: se reutiliza enrich().
        BoardGameSearchResult result = enrich(toResult(item), item);
        return new BoardGameExpansionInfo(
                result.bggId(),
                result.title(),
                result.description(),
                result.yearPublished(),
                result.minPlayers(),
                result.maxPlayers(),
                result.minPlaytime(),
                result.maxPlaytime(),
                result.publisher(),
                result.designers(),
                result.categories(),
                result.mechanics(),
                result.imageUrl(),
                result.thumbnailUrl(),
                result.bggRating(),
                normalizeType(item.type),
                baseIdsOf(item));
    }

    private boolean isInbound(BggLink link) {
        return "true".equalsIgnoreCase(link.inbound);
    }

    /**
     * BGG tipa entradas que este modelo no conoce ({@code boardgameaccessory}…).
     * Mapearlas a {@code null} en vez de fallar deja que la validación de alta
     * las rechace como «no es una expansión».
     */
    private BoardGameType normalizeType(String type) {
        try {
            return BoardGameType.fromKey(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BoardGameSearchResult toResult(BggXmlItem item) {
        return new BoardGameSearchResult(
                item.id,
                item.name != null ? item.name.value : null,
                null,
                item.yearPublished != null ? item.yearPublished.value : null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                item.image,
                item.thumbnail,
                null,
                EXTERNAL_SOURCE);
    }

    private String extractFirstLink(List<BggLink> links, String type) {
        if (links == null) {
            return null;
        }
        return links.stream()
                .filter(link -> type.equals(link.type))
                .map(link -> link.value)
                .findFirst()
                .orElse(null);
    }

    private List<String> extractLinkValues(List<BggLink> links, String type) {
        if (links == null) {
            return List.of();
        }
        return links.stream()
                .filter(link -> type.equals(link.type))
                .map(link -> link.value)
                .toList();
    }

    @JacksonXmlRootElement(localName = "items")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BggXmlItems {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        public List<BggXmlItem> items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BggXmlItem {
        @JacksonXmlProperty(isAttribute = true)
        public String id;

        @JacksonXmlProperty(isAttribute = true)
        public String type;

        @JacksonXmlProperty(localName = "name")
        public BggXmlName name;

        @JacksonXmlProperty(localName = "yearpublished")
        public BggXmlValue yearPublished;

        @JacksonXmlProperty(localName = "image")
        public String image;

        @JacksonXmlProperty(localName = "thumbnail")
        public String thumbnail;

        @JacksonXmlProperty(localName = "description")
        public String description;

        @JacksonXmlProperty(localName = "minplayers")
        public BggXmlIntValue minPlayers;

        @JacksonXmlProperty(localName = "maxplayers")
        public BggXmlIntValue maxPlayers;

        @JacksonXmlProperty(localName = "minplaytime")
        public BggXmlIntValue minPlaytime;

        @JacksonXmlProperty(localName = "maxplaytime")
        public BggXmlIntValue maxPlaytime;

        @JacksonXmlProperty(localName = "statistics")
        public BggThingStats statistics;

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "link")
        public List<BggLink> links;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BggXmlName {
        @JacksonXmlProperty(isAttribute = true)
        public String value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BggXmlValue {
        @JacksonXmlProperty(isAttribute = true)
        public Integer value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BggXmlIntValue {
        @JacksonXmlProperty(isAttribute = true)
        public Integer value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BggThingStats {
        @JacksonXmlProperty(localName = "ratings")
        public BggThingRatings ratings;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BggThingRatings {
        @JacksonXmlProperty(localName = "average")
        public BggXmlRatingValue average;

        @JacksonXmlProperty(localName = "bayesaverage")
        public BggXmlRatingValue bayesAverage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BggXmlRatingValue {
        @JacksonXmlProperty(isAttribute = true)
        public BigDecimal value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BggLink {
        @JacksonXmlProperty(isAttribute = true)
        public String type;

        @JacksonXmlProperty(isAttribute = true)
        public String id;

        @JacksonXmlProperty(isAttribute = true)
        public String value;

        @JacksonXmlProperty(isAttribute = true)
        public String inbound;
    }
}
