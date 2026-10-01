package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.domain.model.PlatformInfo;
import com.wikicollection.domain.port.out.PlatformCatalogClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlatformServiceTest {

    @Mock
    private PlatformCatalogClient catalogClient;

    @InjectMocks
    private PlatformService platformService;

    @Test
    void getPlatforms_delegatesToCatalogClient() {
        List<PlatformInfo> expected = List.of(new PlatformInfo(1L, "PlayStation 5", "ps5"));
        when(catalogClient.getPlatforms()).thenReturn(expected);

        assertThat(platformService.getPlatforms()).isSameAs(expected);

        verify(catalogClient).getPlatforms();
    }
}