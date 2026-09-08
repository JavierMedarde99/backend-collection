package com.wikicollection.infrastructure.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.wikicollection.domain.model.MagicCardCondition;
import com.wikicollection.domain.model.MagicCardLanguage;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Document(collection = "MAGIC_CARDS")
public class MagicCardEntity {

    @Id
    private String id;

    private String scryfallId;

    private String oracleId;

    private String name;

    private MagicCardLanguage language;

    private String releaseDate;

    private String manaCost;

    private Double convertedManaCost;

    private String type;

    private String text;

    private String power;

    private String toughness;

    private String loyalty;

    private List<String> colors;

    private List<String> colorIdentity;

    private List<String> keywords;

    private String rarity;

    private String setCode;

    private String setName;

    private String artist;

    private String frame;

    private String borderColor;

    private String layout;

    private Map<String, String> legalities;

    private String priceUsd;

    private String priceEur;

    private String imageUrl;

    private String imageLargeUrl;

    private String artCropUrl;

    private MagicCardCondition condition;

    private Boolean isFoil;

    private Integer quantity;

    private String notes;

    private LocalDateTime dateAdded;
}
