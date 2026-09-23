package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.StreamingProvider;

public record StreamingProviderResponse(
        Integer providerId,
        String providerName,
        String logoUrl,
        String type,
        String deepLinkUrl) {

    public static StreamingProviderResponse from(StreamingProvider provider) {
        if (provider == null) {
            return null;
        }
        return new StreamingProviderResponse(
                provider.getProviderId(),
                provider.getProviderName(),
                provider.getLogoUrl(),
                provider.getType() == null ? null : provider.getType().name(),
                provider.getDeepLinkUrl());
    }
}
