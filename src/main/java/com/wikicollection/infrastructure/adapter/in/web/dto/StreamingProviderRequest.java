package com.wikicollection.infrastructure.adapter.in.web.dto;

public record StreamingProviderRequest(
        Integer providerId,
        String providerName,
        String logoUrl,
        String type,
        String deepLinkUrl) {
}
