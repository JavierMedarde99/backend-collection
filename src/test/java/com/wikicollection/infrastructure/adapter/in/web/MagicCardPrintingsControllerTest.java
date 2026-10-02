package com.wikicollection.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.wikicollection.application.exception.MagicCardNotFoundException;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardPrinting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El endpoint es público como {@code /magic/search}, y {@code SecurityConfig} ya
 * permite todos los GET bajo {@code /api/v1/**}; estos tests lo fijan para que nadie lo
 * cierre por error al tocar la seguridad.
 */
@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class MagicCardPrintingsControllerTest {

    private static final String PRINTINGS_URL = "/api/v1/magic/scryfall/dce15387/printings";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.wikicollection.application.service.OwnerResolver ownerResolver;

    @MockitoBean
    private com.wikicollection.domain.port.out.MagicCardRepository magicCardRepository;

    @MockitoBean
    private com.wikicollection.infrastructure.adapter.out.scryfall.ScryfallClient scryfallClient;

    @MockitoBean
    private com.wikicollection.domain.port.in.UserPreferencesUseCase preferencesUseCase;

    private static MagicCardPrinting printing(String id, String set, String collector) {
        return new MagicCardPrinting(id, "Llanuras", set, "Star Trek", collector, "mythic",
                "Chris Rahn", "2026-08-31", "en",
                "https://cards.scryfall.io/normal/front/" + id + ".jpg",
                "https://cards.scryfall.io/art_crop/front/" + id + ".jpg",
                List.of("nonfoil", "foil"), true, List.of("promo"), List.of("inverted"),
                "black", "12.34", "10.50");
    }

    private void stubPrintings(int page, long total, List<MagicCardPrinting> content) {
        // El servicio resuelve primero el oracle_id de la impresión dada; sin este stub
        // devolvería una página vacía en todos los casos.
        lenient().when(scryfallClient.findById(anyString())).thenReturn(MagicCard.builder()
                .scryfallId("dce15387").oracleId("oracle-x").name("Llanuras").build());
        lenient().when(scryfallClient.findPrintings(anyString(), anyInt()))
                .thenReturn(new PageImpl<>(content, PageRequest.of(page, MagicCardPrinting.PAGE_SIZE), total));
    }

    @BeforeEach
    void stubOwnerResolver() {
        lenient().when(ownerResolver.resolveOwner(any()))
                .thenAnswer(invocation -> com.wikicollection.domain.model.UserOwned.builder()
                        .ownerId(invocation.getArgument(0)).ownerName("Javi").build());
    }

    @Test
    void devuelveLaPaginaDeImpresiones() throws Exception {
        stubPrintings(0, 955, List.of(printing("id-a", "dce", "325"), printing("id-b", "sta", "12")));

        mockMvc.perform(get(PRINTINGS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].scryfallId").value("id-a"))
                .andExpect(jsonPath("$.content[0].name").value("Llanuras"))
                .andExpect(jsonPath("$.content[0].set").value("dce"))
                .andExpect(jsonPath("$.content[0].setName").value("Star Trek"))
                .andExpect(jsonPath("$.content[0].collectorNumber").value("325"))
                .andExpect(jsonPath("$.content[0].rarity").value("mythic"))
                .andExpect(jsonPath("$.content[0].artist").value("Chris Rahn"))
                .andExpect(jsonPath("$.content[0].releasedAt").value("2026-08-31"))
                .andExpect(jsonPath("$.content[0].lang").value("en"))
                .andExpect(jsonPath("$.content[0].fullArt").value(true))
                .andExpect(jsonPath("$.content[0].borderColor").value("black"))
                .andExpect(jsonPath("$.content[0].priceUsd").value("12.34"))
                .andExpect(jsonPath("$.content[0].priceEur").value("10.50"))
                .andExpect(jsonPath("$.content[1].scryfallId").value("id-b"));
    }

    @Test
    void devuelveLosDescriptoresDeArteComoListas() throws Exception {
        stubPrintings(0, 2, List.of(printing("id-a", "dce", "325")));

        mockMvc.perform(get(PRINTINGS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].finishes[0]").value("nonfoil"))
                .andExpect(jsonPath("$.content[0].finishes[1]").value("foil"))
                .andExpect(jsonPath("$.content[0].promoTypes[0]").value("promo"))
                .andExpect(jsonPath("$.content[0].frameEffects[0]").value("inverted"));
    }

    @Test
    void cadaImpresionLlevaSuPropioScryfallId() throws Exception {
        stubPrintings(0, 2, List.of(printing("id-a", "dce", "325"), printing("id-b", "sta", "12")));

        mockMvc.perform(get(PRINTINGS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].scryfallId").value(org.hamcrest.Matchers.contains("id-a", "id-b")));
    }

    @Test
    void paginaEnBaseCeroPorDefecto() throws Exception {
        stubPrintings(0, 955, List.of());

        mockMvc.perform(get(PRINTINGS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0));

        verify(scryfallClient).findPrintings(anyString(), org.mockito.ArgumentMatchers.eq(0));
    }

    @Test
    void propagaElParametroPage() throws Exception {
        stubPrintings(3, 955, List.of());

        mockMvc.perform(get(PRINTINGS_URL).param("page", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(3));

        verify(scryfallClient).findPrintings(anyString(), org.mockito.ArgumentMatchers.eq(3));
    }

    @Test
    void informaDelTotalRealYDeLasPaginasQueImplican() throws Exception {
        stubPrintings(0, 955, List.of());

        mockMvc.perform(get(PRINTINGS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(955))
                // 955 impresiones en paginas fijas de 175 son 6 paginas.
                .andExpect(jsonPath("$.totalPages").value(6))
                .andExpect(jsonPath("$.size").value(175));
    }

    @Test
    void rechazaUnaPaginaNegativa() throws Exception {
        mockMvc.perform(get(PRINTINGS_URL).param("page", "-1"))
                .andExpect(status().isBadRequest());

        verify(scryfallClient, never()).findPrintings(anyString(), anyInt());
    }

    @Test
    void resuelveElOracleIdAntesDePedirLasImpresiones() throws Exception {
        stubPrintings(0, 955, List.of(printing("id-a", "dce", "325")));

        mockMvc.perform(get(PRINTINGS_URL))
                .andExpect(status().isOk());

        // Scryfall solo pagina por oracle_id. Pedir por el id de impresión devolvería
        // siempre 0 resultados y el endpoint parecería roto.
        verify(scryfallClient).findById("dce15387");
        verify(scryfallClient).findPrintings(org.mockito.ArgumentMatchers.eq("oracle-x"),
                org.mockito.ArgumentMatchers.eq(0));
    }

    @Test
    void unIdInexistenteDa404() throws Exception {
        when(scryfallClient.findById("no-existe"))
                .thenThrow(new MagicCardNotFoundException("Carta no encontrada en Scryfall con id: no-existe"));

        mockMvc.perform(get("/api/v1/magic/scryfall/no-existe/printings"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unaPaginaVaciaEs200YNoUnError() throws Exception {
        // Página posterior a la última de una carta con 100 impresiones: PageImpl solo
        // recorta el total cuando la página trae contenido, así que aquí se conserva.
        stubPrintings(1, 100, List.of());

        mockMvc.perform(get(PRINTINGS_URL).param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(100))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(175));
    }

    @Test
    void unaCartaConMenosImpresionesQueLaPaginaCabeEnUnaSola() throws Exception {
        stubPrintings(0, 100, List.of(printing("id-a", "dce", "325")));

        mockMvc.perform(get(PRINTINGS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(175));
    }

    @Test
    void noExponeElOracleIdNiLaCantidadDeCopias() throws Exception {
        stubPrintings(0, 1, List.of(printing("id-a", "dce", "325")));

        var body = mockMvc.perform(get(PRINTINGS_URL))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // El oracle_id es interno: sirve para agrupar, y el cliente no lo necesita.
        assertThat(body).doesNotContain("oracleId").doesNotContain("quantity").doesNotContain("ownerId");
    }

    @Test
    void elEndpointEsPublicoSinAutenticacion() throws Exception {
        stubPrintings(0, 1, List.of(printing("id-a", "dce", "325")));

        mockMvc.perform(get(PRINTINGS_URL))
                .andExpect(status().isOk());
    }

    @Test
    void noConsultaLaColeccionDelUsuario() throws Exception {
        stubPrintings(0, 1, List.of(printing("id-a", "dce", "325")));

        mockMvc.perform(get(PRINTINGS_URL))
                .andExpect(status().isOk());

        // El endpoint es de catálogo público: no toca el repositorio ni exige propietario.
        verify(magicCardRepository, never()).search(any(), any());
    }

}
