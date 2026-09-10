package com.wikicollection.domain.model;

import java.util.List;

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
public class DeckCard {

    private String cardName;
    private Integer quantity;
    private Boolean inCollection;
    private Boolean isProxy;
    private String manaCost;
    private String typeLine;
    private List<String> colorIdentity;
    private String imageUrl;
    private String scryfallId;
}
