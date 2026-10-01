package com.wikicollection.domain.port.in;

import java.util.List;

import com.wikicollection.domain.model.PlatformInfo;

public interface PlatformCatalogUseCase {

    List<PlatformInfo> getPlatforms();
}