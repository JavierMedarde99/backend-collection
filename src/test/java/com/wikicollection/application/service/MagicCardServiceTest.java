package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.wikicollection.application.exception.MagicCardNotFoundException;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.port.out.MagicCardRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MagicCardServiceTest {

    @Mock
    private MagicCardRepository magicCardRepository;

    @InjectMocks
    private MagicCardService magicCardService;

    private MagicCard sampleCard() {
        return MagicCard.builder()
                .name("Lightning Bolt")
                .rarity("uncommon")
                .quantity(1)
                .isFoil(false)
                .build();
    }

    private MagicCard sampleCard(String id, String name) {
        MagicCard card = sampleCard();
        card.setId(id);
        card.setName(name);
        return card;
    }

    @Test
    void search_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        MagicCardSearchCriteria criteria = new MagicCardSearchCriteria("lightning");
        when(magicCardRepository.search(criteria, pageable)).thenReturn(Page.empty());

        Page<MagicCard> result = magicCardService.search(criteria, pageable);

        assertThat(result).isEmpty();
        verify(magicCardRepository).search(criteria, pageable);
    }

    @Test
    void findById_returnsCard_whenExists() {
        MagicCard card = sampleCard("mc1", "Lightning Bolt");
        when(magicCardRepository.findById("mc1")).thenReturn(Optional.of(card));

        MagicCard result = magicCardService.findById("mc1");

        assertThat(result).isSameAs(card);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(magicCardRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> magicCardService.findById("nope"))
                .isInstanceOf(MagicCardNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void delete_deletesCard_whenExists() {
        MagicCard card = sampleCard("mc1", "Lightning Bolt");
        when(magicCardRepository.findById("mc1")).thenReturn(Optional.of(card));

        magicCardService.delete("mc1");

        verify(magicCardRepository).deleteById("mc1");
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(magicCardRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> magicCardService.delete("nope"))
                .isInstanceOf(MagicCardNotFoundException.class)
                .hasMessageContaining("nope");
    }
}
