package com.wikicollection.infrastructure.adapter.out.bgg.mapper;

import java.util.List;

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
}