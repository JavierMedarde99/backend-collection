package com.wikicollection.application.service;

import java.util.List;

import com.wikicollection.domain.model.PlatformInfo;
import com.wikicollection.domain.port.in.PlatformCatalogUseCase;
import com.wikicollection.domain.port.out.PlatformCatalogClient;

import org.springframework.stereotype.Service;

@Service
public class PlatformService implements PlatformCatalogUseCase {

    private final PlatformCatalogClient catalogClient;

    public PlatformService(PlatformCatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    @Override
    public List<PlatformInfo> getPlatforms() {
        return catalogClient.getPlatforms();
    }
}