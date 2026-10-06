package com.wikicollection.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.wikicollection.application.exception.DeckNotFoundException;
import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckCard;
import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckImportJob;
import com.wikicollection.domain.model.DeckImportMode;
import com.wikicollection.domain.model.DeckImportPhase;
import com.wikicollection.domain.model.DeckImportProgress;
import com.wikicollection.domain.model.DeckImportStatus;
import com.wikicollection.domain.model.DeckListEntry;
import com.wikicollection.domain.model.DeckStatusReport;
import com.wikicollection.domain.model.MagicCardSearchResult;
import com.wikicollection.domain.model.ParsedDeckList;
import com.wikicollection.domain.model.UnresolvedCardEntry;
import com.wikicollection.domain.model.UnresolvedReason;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;
import com.wikicollection.domain.port.out.MagicCardRepository;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Importa el archivo de un mazo en segundo plano (#353).
 *
 * <p>Tres decisiones que no se pueden intuir leyendo el código:
 *
 * <ul>
 *   <li>El mazo se escribe <strong>una sola vez</strong>, al final. Escribirlo carta a carta
 *       dejaría un mazo a medias si Scryfall falla a mitad, y el usuario vería un mazo que
 *       nunca pidió.
 *   <li>Una sugerencia de Scryfall solo se acepta si coincide con el nombre pedido tras
 *       normalizar. Perdona tildes y mayúsculas ("Kóngou" es "Kongou"), no cartas distintas
 *       ("Llanuras" no es "Plains": esa se propone y la elige el usuario).
 *   <li>Un fallo de Scryfall tumba el job sin tocar el mazo, y no se confunde con "esta carta
 *       no existe". Por eso {@code searchByNameExact} propaga el error en vez de devolver una
 *       lista vacía.
 * </ul>
 *
 * <p>El hilo no hereda el {@code SecurityContext}, así que el usuario va en parámetro. Y
 * {@link #run} no debe llamarse desde este mismo bean: la auto-invocación ignora
 * {@code @Async} en silencio y la importación se haría en el hilo del request.
 */
@Component
public class DeckImportWorker {

    private final DeckImportJobStore store;
    private final DeckListParserRegistry parsers;
    private final ExternalMagicCardCatalogClient catalog;
    private final DeckRepository deckRepository;
    private final MagicCardRepository magicCardRepository;
    private final DeckCardFactory cardFactory;
    private final DeckValidator validator;
    private final OwnershipValidator ownershipValidator;
    private final DeckNameNormalizer normalizer;
    private final DeckCacheInvalidator cacheInvalidator;

    public DeckImportWorker(DeckImportJobStore store,
                            DeckListParserRegistry parsers,
                            ExternalMagicCardCatalogClient catalog,
                            DeckRepository deckRepository,
                            MagicCardRepository magicCardRepository,
                            DeckCardFactory cardFactory,
                            DeckValidator validator,
                            OwnershipValidator ownershipValidator,
                            DeckNameNormalizer normalizer,
                            DeckCacheInvalidator cacheInvalidator) {
        this.store = store;
        this.parsers = parsers;
        this.catalog = catalog;
        this.deckRepository = deckRepository;
        this.magicCardRepository = magicCardRepository;
        this.cardFactory = cardFactory;
        this.validator = validator;
        this.ownershipValidator = ownershipValidator;
        this.normalizer = normalizer;
        this.cacheInvalidator = cacheInvalidator;
    }

    /**
     * Procesa una importación. Se encola; no devuelve el resultado.
     *
     * @throws org.springframework.core.task.TaskRejectedException si el executor está lleno;
     *         el controller la traduce a 429 en vez de dejar que un pico de importaciones
     *         devuelva un 500
     */
    @Async("deckImportExecutor")
    public void run(String jobId, String deckId, String ownerId, String content,
                    DeckImportFormat format, DeckImportMode mode) {
        DeckImportJob job = store.find(jobId)
                .orElseGet(() -> DeckImportJob.pending(jobId, deckId, ownerId, format, mode));
        DeckImportJob current = publish(job, DeckImportPhase.PARSING, 0, 0, 0, 0, List.of());

        Deck deck;
        ParsedDeckList parsed;
        try {
            deck = load(deckId, ownerId);
            parsed = parsers.forFormat(format).parse(content);
        } catch (RuntimeException e) {
            store.save(current.failed(describe(e)));
            return;
        }

        List<DeckListEntry> entries = parsed.entries();
        int total = entries.size();
        int sideboardIgnored = parsed.sideboardIgnored();
        List<UnresolvedCardEntry> unresolved = new ArrayList<>();
        current = publish(current, DeckImportPhase.RESOLVING, total, 0, 0, sideboardIgnored, unresolved);

        // Un nombre, una consulta: un mazo de 99 cartas sale de Scryfall muchas veces pero solo
        // tiene unas 40 distintas.
        Map<String, Resolution> resolutions = new HashMap<>();
        Map<String, Integer> quantities = new LinkedHashMap<>();
        int[] counters = {0, 0}; // procesados, resueltos

        String commanderName;
        List<String> commanderColors;
        try {
            Resolution commander = resolveCommander(entries);
            commanderName = commander.found() ? commander.card().name() : null;
            commanderColors = commander.found() ? commander.card().colorIdentity() : List.of();
            if (commanderName != null) {
                // Cuenta como resuelta: es una línea del archivo que acabó con su carta.
                counters[1]++;
                current = current.commander(commanderName, commanderColors);
                store.save(current);
            } else if (commander.entry() != null) {
                unresolved.add(unresolved(commander));
            }
            Set<String> takenByCommander = commanderName == null
                    ? Set.of()
                    : Set.of(normalizer.normalize(commanderName));

            for (DeckListEntry entry : entries) {
                counters[0]++;
                if (entry.commander() || takenByCommander.contains(normalizer.normalize(entry.name()))) {
                    // Ya está como comandante. Contarlo como carta del mazo haría que el
                    // validador lo marcara como singleton duplicado.
                    current = publish(current, DeckImportPhase.RESOLVING, total, counters[0], counters[1],
                            sideboardIgnored, unresolved);
                    continue;
                }
                String key = normalizer.normalize(entry.name());
                Resolution resolution = resolutions.computeIfAbsent(key, ignored -> resolve(entry));
                if (resolution.found()) {
                    counters[1]++;
                    quantities.merge(key, entry.quantity(), Integer::sum);
                } else {
                    unresolved.add(unresolved(resolution));
                }
                current = publish(current, DeckImportPhase.RESOLVING, total, counters[0], counters[1],
                        sideboardIgnored, unresolved);
            }
        } catch (UpstreamFailure e) {
            unresolved.add(new UnresolvedCardEntry(e.entry().line(), e.entry().name(), e.entry().quantity(),
                    e.entry().name(), UnresolvedReason.UPSTREAM_ERROR, List.of()));
            current = publish(current, DeckImportPhase.RESOLVING, total, counters[0], counters[1],
                    sideboardIgnored, unresolved);
            store.save(current.failed(e.getMessage()));
            return;
        } catch (RuntimeException e) {
            // Red final de esta fase. Sin ella, cualquier fallo que no fuera Scryfall (una
            // respuesta que Jackson no entiende, un nombre con el que no se puede construir
            // la URI) escaparía de run(): @Async se traga la excepción, no hay
            // AsyncUncaughtExceptionHandler configurado y el job se quedaría en RUNNING
            // hasta que el TTL lo borrara a los 30 minutos, sin que el usuario llegara a
            // ver un solo FAILED.
            store.save(current.failed(describe(e)));
            return;
        }

        current = publish(current, DeckImportPhase.SAVING, total, counters[0], counters[1], sideboardIgnored,
                unresolved);
        try {
            Deck saved = write(deck, mode, commanderName, commanderColors, quantities, resolutions, ownerId);
            cacheInvalidator.afterImport();
            DeckStatusReport report = new DeckStatusReport(validator.evaluate(saved), validator.validate(saved));
            store.save(current.completed(report, commanderName, commanderColors));
        } catch (RuntimeException e) {
            store.save(current.failed(describe(e)));
        }
    }

    // -------------------------------------------------------------------- carga

    private Deck load(String deckId, String ownerId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new DeckNotFoundException("No existe el mazo " + deckId));
        ownershipValidator.validateOwner(deck.getOwnerId(), ownerId);
        return deck;
    }

    /**
     * El comandante se resuelve antes que el resto del mazo, y solo el primero: un archivo con
     * dos líneas en zona COMMANDER no declara dos comandantes. Sin zona COMMANDER devuelve
     * una resolución con {@code entry() == null}, que no es un error sino un archivo sin
     * comandante.
     */
    private Resolution resolveCommander(List<DeckListEntry> entries) {
        for (DeckListEntry entry : entries) {
            if (entry.commander()) {
                return resolve(entry);
            }
        }
        return new Resolution(null, null, List.of(), null);
    }

    // ---------------------------------------------------------------- resolución

    private Resolution resolve(DeckListEntry entry) {
        String name = entry.name();
        if (normalizer.isBasicLand(name)) {
            return Resolution.found(entry, basicLand(name));
        }
        String expected = normalizer.normalize(name);

        List<MagicCardSearchResult> exact = searchByName(entry);
        List<MagicCardSearchResult> sameName = namedExactly(exact, expected);
        if (sameName.size() == 1) {
            return Resolution.found(entry, sameName.get(0));
        }
        if (!exact.isEmpty()) {
            // Scryfall devolvió varias parecidas al nombre pedido y no hay forma de elegir sin
            // el usuario.
            return Resolution.ambiguous(entry, exact);
        }

        List<MagicCardSearchResult> suggestions = searchSuggestions(entry);
        List<MagicCardSearchResult> closeEnough = namedExactly(suggestions, expected);
        if (closeEnough.size() == 1) {
            return Resolution.found(entry, closeEnough.get(0));
        }
        return Resolution.notFound(entry, suggestions);
    }

    private List<MagicCardSearchResult> searchByName(DeckListEntry entry) {
        try {
            return catalog.searchByNameExact(entry.name());
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw new UpstreamFailure(entry, e);
        }
    }

    private List<MagicCardSearchResult> searchSuggestions(DeckListEntry entry) {
        try {
            return catalog.searchSuggestions(entry.name());
        } catch (RestClientResponseException | ResourceAccessException e) {
            throw new UpstreamFailure(entry, e);
        }
    }

    private List<MagicCardSearchResult> namedExactly(List<MagicCardSearchResult> cards, String expected) {
        return cards.stream().filter(card -> expected.equals(normalizer.normalize(card.name()))).toList();
    }

    /**
     * Carta sintética para una tierra básica: no tiene id de Scryfall y no lo necesita,
     * {@link DeckValidator} la reconoce por el {@code typeLine}.
     */
    private MagicCardSearchResult basicLand(String name) {
        return new MagicCardSearchResult(null, name, null, "Basic Land", "common",
                null, null, null, null, List.of(), List.of(), null);
    }

    // ------------------------------------------------------------------ guardado

    private Deck write(Deck deck, DeckImportMode mode, String commanderName, List<String> commanderColors,
                       Map<String, Integer> quantities, Map<String, Resolution> resolutions, String ownerId) {
        Set<String> owned = normalizer.normalizeAll(magicCardRepository.findNamesByOwnerId(ownerId));

        Map<String, DeckCard> cards = new LinkedHashMap<>();
        if (mode == DeckImportMode.MERGE) {
            for (DeckCard existing : deck.getCards() == null ? List.<DeckCard>of() : deck.getCards()) {
                cards.put(key(existing), existing);
            }
        }
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            MagicCardSearchResult card = resolutions.get(entry.getKey()).card();
            DeckCard imported = cardFactory.fromSearchResult(card, entry.getValue(),
                    owned.contains(normalizer.normalize(card.name())));
            cards.merge(key(imported), imported, (existing, added) -> {
                existing.setQuantity(existing.getQuantity() + added.getQuantity());
                return existing;
            });
        }

        deck.setCards(new ArrayList<>(cards.values()));
        if (commanderName != null) {
            deck.setCommander(commanderName);
            deck.setCommanderColors(commanderColors);
        }
        deck.setUpdatedAt(LocalDateTime.now());
        return deckRepository.save(deck);
    }

    /**
     * Misma clave que usa {@code DeckValidator} para el singleton, para que al fusionar dos
     * cartas con la misma clave no acabemos con dos filas que el validador marcará como
     * duplicado.
     */
    private String key(DeckCard card) {
        return card.getScryfallId() != null
                ? "id:" + card.getScryfallId()
                : "name:" + normalizer.normalize(card.getCardName());
    }

    // -------------------------------------------------------------------- estado

    private DeckImportJob publish(DeckImportJob job, DeckImportPhase phase, int total, int processed,
                                  int resolved, int sideboardIgnored, List<UnresolvedCardEntry> unresolved) {
        DeckImportJob updated = job.progress(DeckImportStatus.RUNNING, phase,
                new DeckImportProgress(total, processed, resolved, sideboardIgnored), unresolved);
        store.save(updated);
        return updated;
    }

    /**
     * El {@code raw} es el nombre de la línea porque los parsers no guardan el texto original:
     * el número de línea y la cantidad ya bastan para que el usuario encuentre la carta en su
     * archivo.
     */
    private UnresolvedCardEntry unresolved(Resolution resolution) {
        DeckListEntry entry = resolution.entry();
        return new UnresolvedCardEntry(entry.line(), entry.name(), entry.quantity(), entry.name(),
                resolution.reason(), resolution.candidates());
    }

    /**
     * Nunca devuelve un mensaje vacío: un job en FAILED sin texto no le dice nada a quien lo
     * está mirando.
     */
    private static String describe(Throwable e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    // --------------------------------------------------------------------- tipos

    private record Resolution(MagicCardSearchResult card, UnresolvedReason reason,
                              List<MagicCardSearchResult> candidates, DeckListEntry entry) {

        static Resolution found(DeckListEntry entry, MagicCardSearchResult card) {
            return new Resolution(card, null, List.of(), entry);
        }

        static Resolution notFound(DeckListEntry entry, List<MagicCardSearchResult> candidates) {
            return new Resolution(null, UnresolvedReason.NOT_FOUND, candidates, entry);
        }

        static Resolution ambiguous(DeckListEntry entry, List<MagicCardSearchResult> candidates) {
            return new Resolution(null, UnresolvedReason.AMBIGUOUS, candidates, entry);
        }

        boolean found() {
            return card != null;
        }
    }

    /** Scryfall falló al buscar una carta concreta, así que el job no puede seguir. */
    private static final class UpstreamFailure extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final transient DeckListEntry entry;

        UpstreamFailure(DeckListEntry entry, RuntimeException cause) {
            super("Scryfall falló al buscar \"" + entry.name() + "\": " + describe(cause), cause);
            this.entry = entry;
        }

        DeckListEntry entry() {
            return entry;
        }
    }
}