package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckCard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeckPersistenceAdapterTest {

    @Mock
    private SpringDataDeckRepository springDataDeckRepository;

    @Mock
    private DeckEntityMapper mapper;

    @InjectMocks
    private DeckPersistenceAdapter adapter;

    private Deck sampleDeck() {
        return Deck.builder()
                .id("d1")
                .name("Mi Commander")
                .commander("Atraxa, Praetors' Voice")
                .commanderColors(List.of("W", "U", "B", "G"))
                .cards(List.of(DeckCard.builder()
                        .cardName("Sol Ring")
                        .quantity(1)
                        .inCollection(true)
                        .isProxy(false)
                        .colorIdentity(List.of())
                        .scryfallId("sf-1")
                        .build()))
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 12, 0))
                .build();
    }

    @Test
    void findById_mapsEntity_whenExists() {
        DeckEntity entity = DeckEntity.builder().id("d1").name("Mi Commander").build();
        Deck expected = sampleDeck();
        when(springDataDeckRepository.findById("d1")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        Optional<Deck> result = adapter.findById("d1");

        assertThat(result).contains(expected);
    }

    @Test
    void findById_returnsEmpty_whenMissing() {
        when(springDataDeckRepository.findById("nope")).thenReturn(Optional.empty());

        assertThat(adapter.findById("nope")).isEmpty();
    }

    @Test
    void save_mapsDomainToEntity_andBack() {
        Deck deck = sampleDeck();
        DeckEntity entity = DeckEntity.builder().id("d1").name("Mi Commander").build();
        when(mapper.toEntity(deck)).thenReturn(entity);
        when(springDataDeckRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(deck);

        Deck result = adapter.save(deck);

        assertThat(result).isSameAs(deck);
    }

    @Test
    void deleteById_delegatesToSpringData() {
        adapter.deleteById("d1");

        verify(springDataDeckRepository).deleteById("d1");
    }

    @Test
    void findByName_mapsEntities() {
        DeckEntity entity = DeckEntity.builder().id("d1").name("Mi Commander").build();
        Deck expected = sampleDeck();
        when(springDataDeckRepository.findByName("Mi Commander")).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        List<Deck> result = adapter.findByName("Mi Commander");

        assertThat(result).containsExactly(expected);
    }

    @Test
    void mapper_roundTripsAllFields() {
        Deck deck = sampleDeck();

        Deck roundTripped = new DeckEntityMapper().toDomain(new DeckEntityMapper().toEntity(deck));

        assertThat(roundTripped).usingRecursiveComparison().isEqualTo(deck);
    }

    @Test
    void mapper_handlesNull() {
        DeckEntityMapper deckMapper = new DeckEntityMapper();

        assertThat(deckMapper.toEntity(null)).isNull();
        assertThat(deckMapper.toDomain(null)).isNull();
    }
}
