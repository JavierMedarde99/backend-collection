package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.port.out.BookRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookSeriesCatalogTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private OwnerScopeResolver ownerScopeResolver;

    @InjectMocks
    private BookService service;

    @Test
    void distinctSeries_devuelveLosNombresTalCualEstan() {
        when(ownerScopeResolver.excludedOwnerIds(CollectionType.BOOKS)).thenReturn(List.of("oculto"));
        when(bookRepository.distinctSeries(List.of("oculto"))).thenReturn(List.of("Harry Potter"));

        assertThat(service.distinctSeries()).containsExactly("Harry Potter");
    }

    @Test
    void distinctSeries_ordenaYQuitaBlancosYDuplicados() {
        when(bookRepository.distinctSeries(any())).thenReturn(
                java.util.Arrays.asList("Dune", "  ", "harry potter", "Dune", null, "The Hobbit"));

        assertThat(service.distinctSeries())
                .containsExactly("Dune", "harry potter", "The Hobbit");
    }

    @Test
    void distinctSeries_devuelveVacioSiElRepositorioDevuelveNull() {
        when(bookRepository.distinctSeries(any())).thenReturn(null);

        assertThat(service.distinctSeries()).isEmpty();
    }
}