package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;

import com.wikicollection.domain.model.AmiiboSearchResult;
import com.wikicollection.domain.port.out.ExternalAmiiboCatalogClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AmiiboSearchServiceTest {

    @Mock
    private ExternalAmiiboCatalogClient amiiboCatalogClient;

    @InjectMocks
    private AmiiboSearchService service;

    @Test
    void search_rejectsBlankQuery() {
        assertThatThrownBy(() -> service.search("  ", PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void search_slicesClientResults() {
        List<AmiiboSearchResult> all = IntStream.range(0, 25)
                .mapToObj(i -> new AmiiboSearchResult("id" + i, "amiibo " + i, null, null, null, null, null,
                        null, List.of(), AmiiboSearchResult.EXTERNAL_SOURCE))
                .toList();
        when(amiiboCatalogClient.searchByName("mario")).thenReturn(all);

        Page<AmiiboSearchResult> page = service.search("mario", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getContent().get(0).externalId()).isEqualTo("id0");
    }

    @Test
    void search_returnsEmptyPage_whenClientEmpty() {
        when(amiiboCatalogClient.searchByName("zzz")).thenReturn(List.of());

        Page<AmiiboSearchResult> page = service.search("zzz", PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }
}