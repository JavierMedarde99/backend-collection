package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.domain.model.BookSearchResult;
import com.wikicollection.domain.port.out.ExternalBookCatalogClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookSearchServiceTest {

    @Mock
    private ExternalBookCatalogClient externalBookCatalogClient;

    @InjectMocks
    private BookSearchService bookSearchService;

    @Test
    void search_delegatesToClient() {
        BookSearchResult result = new BookSearchResult(
                "abc123", "Cien años de soledad", List.of("Gabriel García Márquez"),
                "9780307474728", "http://thumb", "Sinopsis", 417,
                "Vintage Español", "2011-05-03", "es", List.of("Literatura"));
        when(externalBookCatalogClient.search("cien")).thenReturn(List.of(result));

        List<BookSearchResult> results = bookSearchService.search("cien");

        assertThat(results).containsExactly(result);
        verify(externalBookCatalogClient).search("cien");
    }

    @Test
    void search_rejectsBlankQuery() {
        assertThatThrownBy(() -> bookSearchService.search("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void search_rejectsNullQuery() {
        assertThatThrownBy(() -> bookSearchService.search(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}