package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.application.exception.MagicCardNotFoundException;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardPrinting;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * El endpoint público recibe un id de impresión, pero Scryfall solo permite buscar
 * impresiones por {@code oracle_id}. Por eso el servicio tiene que resolver primero la
 * carta: si no, buscaría literalmente por el id de impresión y devolvería siempre vacío.
 */
@ExtendWith(MockitoExtension.class)
class MagicCardPrintingsServiceTest {

    @Mock
    private ExternalMagicCardCatalogClient catalogClient;

    @InjectMocks
    private MagicCardService service;

    private static MagicCard cardWithOracleId(String scryfallId, String oracleId) {
        return MagicCard.builder().scryfallId(scryfallId).oracleId(oracleId).name("Llanuras").build();
    }

    /** Refleja la página pedida, como hace el mapper real. */
    private static PageImpl<MagicCardPrinting> pageOf(int page, String total) {
        return new PageImpl<>(List.of(), PageRequest.of(page, 175), Long.parseLong(total));
    }

    @Test
    void resuelveElOracleIdAntesDeBuscarImpresiones() {
        when(catalogClient.findById("dce15387")).thenReturn(cardWithOracleId("dce15387", "oracle-x"));
        when(catalogClient.findPrintings("oracle-x", 0)).thenReturn(pageOf(0, "955"));

        service.printings("dce15387", 0);

        // Buscar por el id de impresión, sin resolver, devolvería siempre 0 resultados.
        verify(catalogClient).findPrintings("oracle-x", 0);
    }

    @Test
    void propagaElNumeroDePaginaTalCual() {
        lenient().when(catalogClient.findById(any())).thenReturn(cardWithOracleId("dce15387", "oracle-x"));
        when(catalogClient.findPrintings("oracle-x", 4)).thenReturn(pageOf(4, "955"));

        var result = service.printings("dce15387", 4);

        assertThat(result.getNumber()).isEqualTo(4);
        verify(catalogClient).findPrintings("oracle-x", 4);
    }

    @Test
    void devuelveLaPaginaDeImpresionesConSuTotal() {
        var printings = List.of(new MagicCardPrinting("id-a", "Llanuras", "dce", "Star Trek", "325",
                "mythic", "Chris Rahn", "2026-08-31", "en", null, null, List.of(), true, null, null,
                "black", "12.34", "10.50"));
        when(catalogClient.findById("dce15387")).thenReturn(cardWithOracleId("dce15387", "oracle-x"));
        when(catalogClient.findPrintings("oracle-x", 0)).thenReturn(new PageImpl<>(printings, PageRequest.of(0, 175), 955));

        var result = service.printings("dce15387", 0);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(955);
        assertThat(result.getTotalPages()).isEqualTo(6);
    }

    @Test
    void unIdQueNoExisteEnScryfallDa404() {
        when(catalogClient.findById("no-existe")).thenThrow(new HttpClientErrorException(
                HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> service.printings("no-existe", 0))
                .isInstanceOf(MagicCardNotFoundException.class)
                .hasMessageContaining("no-existe");
    }

    @Test
    void unFalloDeServidorNoSeConfundeConUnaCartaInexistente() {
        when(catalogClient.findById("dce15387")).thenThrow(new HttpServerErrorException(
                HttpStatus.SERVICE_UNAVAILABLE));

        // Un 503 con el mensaje "no encontrada" sería un 404 falso en la API.
        assertThatThrownBy(() -> service.printings("dce15387", 0))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void unaCartaSinOracleIdNoDisparaUnaBusquedaSinSentido() {
        when(catalogClient.findById("raro")).thenReturn(cardWithOracleId("raro", null));

        var result = service.printings("raro", 0);

        // "q=oracleid:" sin valor devuelve 200 con 0 resultados: mejor una página vacía
        // explícita que una llamada sin sentido a Scryfall.
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(catalogClient, never()).findPrintings(any(), any(Integer.class));
    }

    @Test
    void unaCartaConOracleIdEnBlancoTampocoSeBusca() {
        when(catalogClient.findById("raro")).thenReturn(cardWithOracleId("raro", "   "));

        assertThat(service.printings("raro", 0).getContent()).isEmpty();
        verify(catalogClient, never()).findPrintings(any(), any(Integer.class));
    }

}
