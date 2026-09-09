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
import com.wikicollection.domain.model.BoardGameSearchResult;

import org.springframework.stereotype.Component;

@Component
public class BoardGameXmlMapper {

    private static final String EXTERNAL_SOURCE = "BGG";

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
                thing.stats != null && thing.stats.rating != null ? thing.stats.rating.average : null,
                result.externalSource());
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

        @JacksonXmlProperty(localName = "stats")
        public BggThingStats stats;

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
        @JacksonXmlProperty(localName = "rating")
        public BggThingRating rating;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BggThingRating {
        @JacksonXmlProperty(isAttribute = true)
        public BigDecimal average;

        @JacksonXmlProperty(isAttribute = true, localName = "bayesaverage")
        public BigDecimal bayesAverage;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BggLink {
        @JacksonXmlProperty(isAttribute = true)
        public String type;

        @JacksonXmlProperty(isAttribute = true)
        public String value;
    }
}
