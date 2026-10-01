package com.wikicollection.domain.port.out;

import java.util.List;

import com.wikicollection.domain.model.PlatformInfo;

public interface PlatformCatalogClient {

    List<PlatformInfo> getPlatforms();
}