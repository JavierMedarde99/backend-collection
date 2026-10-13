package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;

import com.wikicollection.domain.model.FunkoCategory;
import com.wikicollection.domain.model.FunkoSearchResult;
import com.wikicollection.domain.port.out.ExternalFunkoCatalogClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class FunkoSearchServiceTest {

    @Mock
    private ExternalFunkoCatalogClient funkoCatalogClient;

    @InjectMocks
    private FunkoSearchService service;

    @Test
    void search_rejectsBlankQuery() {
        assertThatThrownBy(() -> service.search("  ", PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(funkoCatalogClient, never()).searchByName(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void search_rejectsSingleCharacter() {
        assertThatThrownBy(() -> service.search("b", PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(funkoCatalogClient, never()).searchByName(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void search_slicesClientResults() {
        List<FunkoSearchResult> all = IntStream.range(0, 25)
                .mapToObj(i -> new FunkoSearchResult("id" + i, "pop " + i, null, null, null, null, null,
                        null, null, null, null, null, FunkoSearchResult.EXTERNAL_SOURCE))
                .toList();
        when(funkoCatalogClient.searchByName("batman")).thenReturn(all);

        Page<FunkoSearchResult> page = service.search("batman", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getContent().get(0).externalId()).isEqualTo("id0");
    }

    @Test
    void search_returnsEmptyPage_whenClientEmpty() {
        when(funkoCatalogClient.searchByName("zzzz")).thenReturn(List.of());

        Page<FunkoSearchResult> page = service.search("zzzz", PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void listCategories_delegatesToClient() {
        List<FunkoCategory> categories = List.of(new FunkoCategory("Movies", 1238));
        when(funkoCatalogClient.listCategories()).thenReturn(categories);

        assertThat(service.listCategories()).isEqualTo(categories);
    }
}
