package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.wikicollection.application.exception.DeckListParseException;
import com.wikicollection.application.exception.ForbiddenException;
import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckCard;
import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckImportJob;
import com.wikicollection.domain.model.DeckImportMode;
import com.wikicollection.domain.model.DeckImportPhase;
import com.wikicollection.domain.model.DeckImportStatus;
import com.wikicollection.domain.model.DeckListEntry;
import com.wikicollection.domain.model.DeckStatus;
import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.domain.model.ParsedDeckList;
import com.wikicollection.domain.model.UnresolvedCardEntry;
import com.wikicollection.domain.model.UnresolvedReason;
import com.wikicollection.domain.port.out.DeckListParser;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;
import com.wikicollection.domain.port.out.MagicCardRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * El worker de importación de mazos (#353).
 *
 * <p>Es el único sitio donde se decide qué cartas de un archivo entran en el mazo, así que
 * aquí se fijan las reglas que el resto del sistema no puede dar por hechas: los nombres
 * duplicados suman en una sola carta, el comandante repetido en la zona principal no cuenta
 * como carta del mazo, las tierras básicas no se buscan en Scryfall, y un fallo de Scryfall
 * tumba el job sin tocar el mazo en vez de parecerse a "esta carta no existe".
 */
@ExtendWith(MockitoExtension.class)
class DeckImportWorkerTest {

    private static final String CONTENT = "contenido";

    @Mock
    private DeckImportJobStore store;
    @Mock
    private DeckListParserRegistry parsers;
    @Mock
    private DeckListParser parser;
    @Mock
    private ExternalMagicCardCatalogClient catalog;
    @Mock
    private DeckRepository deckRepository;
    @Mock
    private MagicCardRepository magicCardRepository;
    @Mock
    private DeckValidator validator;
    @Mock
    private OwnershipValidator ownershipValidator;
    @Mock
    private DeckCacheInvalidator cacheInvalidator;

    private final List<DeckImportJob> savedJobs = new ArrayList<>();

    private DeckImportWorker worker;

    @BeforeEach
    void setUp() {
        worker = new DeckImportWorker(store, parsers, catalog, deckRepository, magicCardRepository,
                new DeckCardFactory(), validator, ownershipValidator, new DeckNameNormalizer(),
                cacheInvalidator);
        lenient().when(store.find("job-1")).thenReturn(Optional.of(
                DeckImportJob.pending("job-1", "deck-1", "user-1", DeckImportFormat.TXT, DeckImportMode.REPLACE)));
        lenient().when(deckRepository.findById("deck-1")).thenReturn(Optional.of(newDeck()));
        lenient().when(magicCardRepository.findNamesByOwnerId(anyString())).thenReturn(List.of());
        lenient().when(validator.validate(any())).thenReturn(List.of());
        lenient().when(validator.evaluate(any())).thenReturn(DeckStatus.DRAFT);
        lenient().doAnswer(invocation -> {
            savedJobs.add(invocation.getArgument(0));
            return null;
        }).when(store).save(any());
    }

    // ------------------------------------------------------------------ guardado

    @Test
    void savesTheDeckOnceWithEveryResolvedCard() {
        givenEntries(2,
                entry(1, 1, "Atraxa, Grand Unifier", true),
                entry(3, 4, "Sol Ring", false),
                entry(4, 20, "Plains", false));
        givenExact("Atraxa, Grand Unifier", card("at", "Atraxa, Grand Unifier"));
        givenExact("Sol Ring", card("sr", "Sol Ring"));

        run();

        verify(deckRepository, times(1)).save(any(Deck.class));
        Deck deck = savedDeck();
        assertThat(deck.getCards()).hasSize(2);
        assertThat(quantityOf(deck, "Sol Ring")).isEqualTo(4);
        assertThat(quantityOf(deck, "Plains")).isEqualTo(20);
        assertThat(deck.getCommander()).isEqualTo("Atraxa, Grand Unifier");

        DeckImportJob job = finalJob();
        assertThat(job.status()).isEqualTo(DeckImportStatus.COMPLETED);
        assertThat(job.progress().total()).isEqualTo(3);
        assertThat(job.progress().processed()).isEqualTo(3);
        assertThat(job.progress().resolved()).isEqualTo(3);
        assertThat(job.unresolved()).isEmpty();
        verify(cacheInvalidator).afterImport();
    }

    @Test
    void countsTheSideboardAsIgnored() {
        givenEntries(7, entry(3, 4, "Sol Ring", false));
        givenExact("Sol Ring", card("sr", "Sol Ring"));

        run();

        assertThat(finalJob().progress().sideboardIgnored()).isEqualTo(7);
    }

    @Test
    void replaceModeOverwritesPreviousCardsOnReimport() {
        Deck existing = newDeck();
        existing.setCards(new ArrayList<>(List.of(
                DeckCard.builder().cardName("Carta Vieja").quantity(1).scryfallId("old").build())));
        when(deckRepository.findById("deck-1")).thenReturn(Optional.of(existing));
        givenEntries(0, entry(3, 4, "Sol Ring", false));
        givenExact("Sol Ring", card("sr", "Sol Ring"));

        worker.run("job-1", "deck-1", "user-1", CONTENT, DeckImportFormat.TXT, DeckImportMode.REPLACE);

        assertThat(savedDeck().getCards()).noneMatch(c -> "Carta Vieja".equals(c.getCardName()));
    }

    @Test
    void mergeModeSumsQuantitiesWithExistingCards() {
        Deck existing = newDeck();
        existing.setCards(new ArrayList<>(List.of(
                DeckCard.builder().cardName("Sol Ring").quantity(4).scryfallId("sr").build())));
        when(deckRepository.findById("deck-1")).thenReturn(Optional.of(existing));
        givenEntries(0, entry(3, 2, "Sol Ring", false));
        givenExact("Sol Ring", card("sr", "Sol Ring"));

        worker.run("job-1", "deck-1", "user-1", CONTENT, DeckImportFormat.TXT, DeckImportMode.MERGE);

        Deck deck = savedDeck();
        assertThat(deck.getCards()).hasSize(1);
        assertThat(deck.getCards().get(0).getQuantity()).isEqualTo(6);
    }

    // ------------------------------------------------------------ normalización

    @Test
    void aggregatesDuplicateNamesBySummingQuantities() {
        givenEntries(0, entry(3, 1, "Sol Ring", false), entry(4, 3, "sol ring", false));
        givenExact("Sol Ring", card("sr", "Sol Ring"));

        run();

        Deck deck = savedDeck();
        assertThat(deck.getCards()).hasSize(1);
        assertThat(deck.getCards().get(0).getQuantity()).isEqualTo(4);
        // Las dos líneas cuentan para el progreso, aunque salga una sola carta.
        assertThat(finalJob().progress().total()).isEqualTo(2);
        assertThat(finalJob().progress().resolved()).isEqualTo(2);
        // Solo se pregunta una vez a Scryfall por nombre normalizado.
        verify(catalog, times(1)).searchByNameExact("Sol Ring");
    }

    @Test
    void basicLandsAreResolvedWithoutCallingScryfall() {
        givenEntries(0, entry(3, 20, "Plains", false));

        run();

        verify(catalog, never()).searchByNameExact("Plains");
        verify(catalog, never()).searchSuggestions("Plains");
        DeckCard land = savedDeck().getCards().get(0);
        // El typeLine es lo que DeckValidator mira para exceptuar la regla de singleton.
        assertThat(land.getTypeLine()).contains("Basic Land");
        assertThat(land.getQuantity()).isEqualTo(20);
    }

    @Test
    void commanderListedInMainZoneIsNotCountedAsDeckCard() {
        givenEntries(0,
                entry(1, 1, "Atraxa, Grand Unifier", true),
                entry(3, 1, "Atraxa, Grand Unifier", false),
                entry(4, 99, "Plains", false));
        givenExact("Atraxa, Grand Unifier", card("at", "Atraxa, Grand Unifier"));

        run();

        Deck deck = savedDeck();
        assertThat(deck.getCards()).noneMatch(c -> "Atraxa, Grand Unifier".equals(c.getCardName()));
        assertThat(quantityOf(deck, "Plains")).isEqualTo(99);
        // Se procesaron las tres líneas, pero solo dos se resolvieron como carta o comandante.
        assertThat(finalJob().progress().processed()).isEqualTo(3);
        assertThat(finalJob().progress().resolved()).isEqualTo(2);
    }

    @Test
    void resolvedCommanderGetsItsColorIdentity() {
        givenEntries(0, entry(1, 1, "Atraxa, Grand Unifier", true));
        givenExact("Atraxa, Grand Unifier", card("at", "Atraxa, Grand Unifier", List.of("G", "W")));

        run();

        DeckImportJob job = finalJob();
        assertThat(job.commanderName()).isEqualTo("Atraxa, Grand Unifier");
        assertThat(job.commanderColors()).containsExactly("G", "W");
        assertThat(savedDeck().getCommanderColors()).containsExactly("G", "W");
    }

    /**
     * Un segundo comandante (un {@code partner} como Nesting Grounds) no se cuela en el mazo
     * como carta: {@code Deck.commander} es un único nombre, y meterlo en {@code cards} lo
     * convertiría en una de las 99 y lo marcaría como singleton duplicado. El archivo se
     * guarda con el primer comandante; el partner se descarta, que es lo mejor que se puede
     * hacer sin un campo para él.
     */
    @Test
    void onlyTheFirstCommanderLineIsTaken() {
        givenEntries(0,
                entry(1, 1, "Atraxa, Grand Unifier", true),
                entry(2, 1, "Nesting Grounds", true),
                entry(3, 20, "Plains", false));
        givenExact("Atraxa, Grand Unifier", card("at", "Atraxa, Grand Unifier"));

        run();

        assertThat(savedDeck().getCommander()).isEqualTo("Atraxa, Grand Unifier");
        assertThat(savedDeck().getCards()).noneMatch(c -> "Nesting Grounds".equals(c.getCardName()));
        // No se llega a preguntar por el segundo comandante: se descarta al parsear la zona.
        verify(catalog, never()).searchByNameExact("Nesting Grounds");
        assertThat(quantityOf(savedDeck(), "Plains")).isEqualTo(20);
    }

    // ------------------------------------------------------------------ resolución

    @Test
    void singleExactCandidateResolvesAndSuggestionsAreNotCalled() {
        givenEntries(0, entry(3, 4, "Sol Ring", false));
        givenExact("Sol Ring", card("sr", "Sol Ring"));

        run();

        verify(catalog, never()).searchSuggestions(anyString());
        assertThat(quantityOf(savedDeck(), "Sol Ring")).isEqualTo(4);
    }

    @Test
    void spanishNameLandsInNotFoundWithSuggestions() {
        givenEntries(0, entry(3, 4, "Llanuras", false));
        when(catalog.searchByNameExact("Llanuras")).thenReturn(List.of());
        when(catalog.searchSuggestions("Llanuras")).thenReturn(List.of(card("pl", "Plains")));

        run();

        DeckImportJob job = finalJob();
        assertThat(job.status()).isEqualTo(DeckImportStatus.COMPLETED);
        assertThat(savedDeck().getCards()).isEmpty();
        assertThat(job.unresolved()).singleElement().satisfies(entry -> {
            assertThat(entry.reason()).isEqualTo(UnresolvedReason.NOT_FOUND);
            assertThat(entry.line()).isEqualTo(3);
            assertThat(entry.quantity()).isEqualTo(4);
            assertThat(entry.candidates()).extracting(MagicCardSearchResult::name).containsExactly("Plains");
        });
    }

    @Test
    void fuzzySingleMatchResolvesAccentDifferences() {
        givenEntries(0, entry(3, 1, "Kóngou, Keeper of the Deep", false));
        when(catalog.searchByNameExact("Kóngou, Keeper of the Deep")).thenReturn(List.of());
        when(catalog.searchSuggestions("Kóngou, Keeper of the Deep"))
                .thenReturn(List.of(card("ko", "Kongou, Keeper of the Deep")));

        run();

        assertThat(quantityOf(savedDeck(), "Kongou, Keeper of the Deep")).isEqualTo(1);
        assertThat(finalJob().unresolved()).isEmpty();
    }

    @Test
    void ambiguousCandidatesAreReportedNotResolved() {
        givenEntries(0, entry(3, 2, "Ambiente", false));
        when(catalog.searchByNameExact("Ambiente"))
                .thenReturn(List.of(card("a", "Ambiente Estéril"), card("b", "Bosque Ambiguo")));

        run();

        DeckImportJob job = finalJob();
        assertThat(job.status()).isEqualTo(DeckImportStatus.COMPLETED);
        assertThat(savedDeck().getCards()).isEmpty();
        assertThat(job.unresolved()).singleElement().satisfies(entry -> {
            assertThat(entry.reason()).isEqualTo(UnresolvedReason.AMBIGUOUS);
            assertThat(entry.candidates()).hasSize(2);
        });
    }

    @Test
    void notFoundWithoutAnyCandidateIsStillReported() {
        givenEntries(0, entry(3, 1, "Carta Inventada", false));
        when(catalog.searchByNameExact("Carta Inventada")).thenReturn(List.of());
        when(catalog.searchSuggestions("Carta Inventada")).thenReturn(List.of());

        run();

        assertThat(finalJob().unresolved()).singleElement().satisfies(entry -> {
            assertThat(entry.reason()).isEqualTo(UnresolvedReason.NOT_FOUND);
            assertThat(entry.candidates()).isEmpty();
        });
    }

    @Test
    void unresolvedCommanderIsReportedAndLeavesTheDeckWithoutCommander() {
        givenEntries(0, entry(1, 1, "Comandante Imaginario", true), entry(3, 99, "Plains", false));

        run();

        // El mazo se guarda igual: un comandante que no existe no tira las 99 cartas.
        assertThat(savedDeck().getCommander()).isNull();
        assertThat(finalJob().unresolved()).singleElement()
                .extracting(UnresolvedCardEntry::reason).isEqualTo(UnresolvedReason.NOT_FOUND);
    }

    @Test
    void aDeckWithoutCommanderLineKeepsTheOneItAlreadyHad() {
        Deck existing = newDeck();
        existing.setCommander("Atraxa, Grand Unifier");
        when(deckRepository.findById("deck-1")).thenReturn(Optional.of(existing));
        givenEntries(0, entry(3, 20, "Plains", false));

        run();

        assertThat(savedDeck().getCommander()).isEqualTo("Atraxa, Grand Unifier");
    }

    // -------------------------------------------------------------------- fallos

    @Test
    void upstreamErrorFailsTheJobAndLeavesTheDeckUntouched() {
        givenEntries(0, entry(3, 4, "Sol Ring", false));
        when(catalog.searchByNameExact("Sol Ring"))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        run();

        DeckImportJob job = finalJob();
        assertThat(job.status()).isEqualTo(DeckImportStatus.FAILED);
        assertThat(job.error()).contains("Sol Ring");
        assertThat(job.unresolved()).singleElement()
                .extracting(UnresolvedCardEntry::reason).isEqualTo(UnresolvedReason.UPSTREAM_ERROR);
        verify(deckRepository, never()).save(any());
        verify(cacheInvalidator, never()).afterImport();
    }

    @Test
    void networkErrorAlsoFailsTheJob() {
        givenEntries(0, entry(3, 4, "Sol Ring", false));
        when(catalog.searchByNameExact("Sol Ring")).thenThrow(new ResourceAccessException("sin red"));

        run();

        assertThat(finalJob().status()).isEqualTo(DeckImportStatus.FAILED);
        verify(deckRepository, never()).save(any());
    }

    @Test
    void parseErrorFailsTheJob() {
        when(parsers.forFormat(DeckImportFormat.TXT)).thenReturn(parser);
        when(parser.parse(CONTENT)).thenThrow(new DeckListParseException("basura"));

        run();

        assertThat(finalJob().status()).isEqualTo(DeckImportStatus.FAILED);
        assertThat(finalJob().error()).contains("basura");
        verify(deckRepository, never()).save(any());
    }

    @Test
    void deckNotFoundFailsTheJob() {
        when(deckRepository.findById("deck-1")).thenReturn(Optional.empty());

        run();

        assertThat(finalJob().status()).isEqualTo(DeckImportStatus.FAILED);
        assertThat(finalJob().error()).contains("deck-1");
        verify(deckRepository, never()).save(any());
    }

    @Test
    void foreignDeckFailsTheJob() {
        doThrow(new ForbiddenException("no tuyo")).when(ownershipValidator).validateOwner("user-1", "otro");

        worker.run("job-1", "deck-1", "otro", CONTENT, DeckImportFormat.TXT, DeckImportMode.REPLACE);

        assertThat(finalJob().status()).isEqualTo(DeckImportStatus.FAILED);
        verify(deckRepository, never()).save(any());
    }

    @Test
    void savingErrorAlsoFailsTheJob() {
        givenEntries(0, entry(3, 1, "Sol Ring", false));
        givenExact("Sol Ring", card("sr", "Sol Ring"));
        when(deckRepository.save(any(Deck.class))).thenThrow(new IllegalStateException("mongo caído"));

        run();

        assertThat(finalJob().status()).isEqualTo(DeckImportStatus.FAILED);
        assertThat(finalJob().error()).contains("mongo caído");
    }

    // ------------------------------------------------------- informe y progreso

    @Test
    void validationReportIsStoredInTheJob() {
        givenEntries(0, entry(3, 1, "Sol Ring", false));
        givenExact("Sol Ring", card("sr", "Sol Ring"));
        when(validator.validate(any())).thenReturn(List.of("El mazo no llega a 99 cartas"));
        when(validator.evaluate(any())).thenReturn(DeckStatus.DRAFT);

        run();

        DeckImportJob job = finalJob();
        assertThat(job.status()).isEqualTo(DeckImportStatus.COMPLETED);
        assertThat(job.validation().status()).isEqualTo(DeckStatus.DRAFT);
        assertThat(job.validation().reasons()).containsExactly("El mazo no llega a 99 cartas");
    }

    @Test
    void progressIsPublishedWhileResolving() {
        givenEntries(0, entry(3, 1, "Sol Ring", false), entry(4, 1, "Counterspell", false));
        givenExact("Sol Ring", card("sr", "Sol Ring"));
        givenExact("Counterspell", card("cs", "Counterspell"));

        run();

        assertThat(savedJobs).anySatisfy(job -> {
            assertThat(job.phase()).isEqualTo(DeckImportPhase.RESOLVING);
            assertThat(job.status()).isEqualTo(DeckImportStatus.RUNNING);
            assertThat(job.progress().processed()).isPositive();
        });
        assertThat(savedJobs).anySatisfy(job ->
                assertThat(job.phase()).isEqualTo(DeckImportPhase.SAVING));
    }

    @Test
    void inCollectionComesFromASingleQueryForOwner() {
        when(magicCardRepository.findNamesByOwnerId("user-1")).thenReturn(List.of("sol ring"));
        givenEntries(0, entry(3, 4, "Sol Ring", false));
        givenExact("Sol Ring", card("sr", "Sol Ring"));

        run();

        DeckCard imported = savedDeck().getCards().get(0);
        assertThat(imported.getInCollection()).isTrue();
        assertThat(imported.getIsProxy()).isFalse();
        verify(magicCardRepository, times(1)).findNamesByOwnerId("user-1");
    }

    @Test
    void ownerNamesOfOtherCardsDoNotMarkThisOneAsOwned() {
        when(magicCardRepository.findNamesByOwnerId("user-1")).thenReturn(List.of("plains", "otra"));
        givenEntries(0, entry(3, 4, "Sol Ring", false));
        givenExact("Sol Ring", card("sr", "Sol Ring"));

        run();

        DeckCard imported = savedDeck().getCards().get(0);
        assertThat(imported.getInCollection()).isFalse();
        assertThat(imported.getIsProxy()).isTrue();
    }

    @Test
    void aJobMissingFromTheStoreStillImports() {
        when(store.find("job-1")).thenReturn(Optional.empty());
        givenEntries(0, entry(3, 1, "Sol Ring", false));
        givenExact("Sol Ring", card("sr", "Sol Ring"));

        run();

        assertThat(finalJob().status()).isEqualTo(DeckImportStatus.COMPLETED);
    }

    // --------------------------------------------------------------------- ayuda

    private void run() {
        worker.run("job-1", "deck-1", "user-1", CONTENT, DeckImportFormat.TXT, DeckImportMode.REPLACE);
    }

    private DeckImportJob finalJob() {
        assertThat(savedJobs).isNotEmpty();
        return savedJobs.get(savedJobs.size() - 1);
    }

    private Deck savedDeck() {
        ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
        verify(deckRepository).save(captor.capture());
        return captor.getValue();
    }

    private void givenEntries(int sideboardIgnored, DeckListEntry... entries) {
        when(parsers.forFormat(DeckImportFormat.TXT)).thenReturn(parser);
        when(parser.parse(CONTENT)).thenReturn(new ParsedDeckList(List.of(entries), sideboardIgnored));
    }

    private void givenExact(String name, MagicCardSearchResult... results) {
        when(catalog.searchByNameExact(name)).thenReturn(List.of(results));
    }

    private Deck newDeck() {
        return Deck.builder().id("deck-1").ownerId("user-1").cards(new ArrayList<>()).build();
    }

    private int quantityOf(Deck deck, String name) {
        return deck.getCards().stream()
                .filter(card -> name.equals(card.getCardName()))
                .mapToInt(card -> card.getQuantity() == null ? 0 : card.getQuantity())
                .findFirst()
                .orElse(-1);
    }

    private DeckListEntry entry(int line, int quantity, String name, boolean commander) {
        return new DeckListEntry(line, quantity, name, null, null, commander);
    }

    private MagicCardSearchResult card(String id, String name) {
        return card(id, name, List.of());
    }

    private MagicCardSearchResult card(String id, String name, List<String> colorIdentity) {
        return new MagicCardSearchResult(id, name, "{1}", "Instant", "rare", "tst", "Test",
                "https://img/" + id, "1.00", List.of(), colorIdentity, "text");
    }
}