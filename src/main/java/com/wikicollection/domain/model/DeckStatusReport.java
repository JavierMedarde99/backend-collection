package com.wikicollection.domain.model;

import java.util.List;

public record DeckStatusReport(
        DeckStatus status,
        List<String> reasons) {
}
