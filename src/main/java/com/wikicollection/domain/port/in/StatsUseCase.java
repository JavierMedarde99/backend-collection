package com.wikicollection.domain.port.in;

import java.util.Map;

public interface StatsUseCase {

    Map<String, Long> getGlobalCounts();
}
