package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.wikicollection.application.exception.BoardGameNotFoundException;
import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameSearchCriteria;
import com.wikicollection.domain.model.BoardGameStatus;
import com.wikicollection.domain.port.out.BoardGameRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class BoardGameServiceTest {

    @Mock
    private BoardGameRepository boardGameRepository;

    @InjectMocks
    private BoardGameService boardGameService;

    private BoardGame sampleGame() {
        return BoardGame.builder()
                .title("Catan")
                .status(BoardGameStatus.OWNED)
                .build();
    }

    private BoardGame sampleGame(String id, String title) {
        BoardGame game = sampleGame();
        game.setId(id);
        game.setTitle(title);
        return game;
    }

    @Test
    void search_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        BoardGameSearchCriteria criteria = new BoardGameSearchCriteria("catan", BoardGameStatus.OWNED);
        when(boardGameRepository.search(criteria, pageable)).thenReturn(Page.empty());

        Page<BoardGame> result = boardGameService.search(criteria, pageable);

        assertThat(result).isEmpty();
        verify(boardGameRepository).search(criteria, pageable);
    }

    @Test
    void findById_returnsBoardGame_whenExists() {
        BoardGame game = sampleGame("bg1", "Catan");
        when(boardGameRepository.findById("bg1")).thenReturn(Optional.of(game));

        BoardGame result = boardGameService.findById("bg1");

        assertThat(result).isSameAs(game);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(boardGameRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardGameService.findById("nope"))
                .isInstanceOf(BoardGameNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void save_delegatesToRepository() {
        BoardGame game = sampleGame();
        when(boardGameRepository.save(any(BoardGame.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoardGame result = boardGameService.save(game);

        assertThat(result).isSameAs(game);
        verify(boardGameRepository).save(game);
    }

    @Test
    void update_throwsNotFound_whenMissing() {
        when(boardGameRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardGameService.update("nope", sampleGame()))
                .isInstanceOf(BoardGameNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void update_appliesFieldsAndKeepsId() {
        BoardGame existing = sampleGame("bg1", "Título original");
        existing.setDescription("Descripción original");

        BoardGame updates = sampleGame();
        updates.setTitle("Nuevo título");
        updates.setDescription("Nueva descripción");
        updates.setStatus(BoardGameStatus.WISHLIST);
        updates.setBggRating(new java.math.BigDecimal("8.4"));
        updates.setBggId("31260");
        updates.setMinPlayers(2);
        updates.setMaxPlayers(4);
        updates.setMinPlaytime(30);
        updates.setMaxPlaytime(120);
        updates.setYearPublished(2007);
        updates.setPublisher("Editorial");
        updates.setDesigners(List.of("Klaus Teuber"));
        updates.setCategories(List.of("Estrategia"));
        updates.setMechanics(List.of("Dados"));
        updates.setImageUrl("http://img");
        updates.setThumbnailUrl("http://thumb");
        updates.setNotes("Nota nueva");
        updates.setDateAdded(LocalDate.of(2026, 1, 1));

        when(boardGameRepository.findById("bg1")).thenReturn(Optional.of(existing));
        when(boardGameRepository.save(any(BoardGame.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoardGame result = boardGameService.update("bg1", updates);

        assertThat(result.getId()).isEqualTo("bg1");
        assertThat(result.getTitle()).isEqualTo("Nuevo título");
        assertThat(result.getDescription()).isEqualTo("Nueva descripción");
        assertThat(result.getStatus()).isEqualTo(BoardGameStatus.WISHLIST);
        assertThat(result.getBggRating()).isEqualByComparingTo("8.4");
        assertThat(result.getBggId()).isEqualTo("31260");
        assertThat(result.getMinPlayers()).isEqualTo(2);
        assertThat(result.getMaxPlayers()).isEqualTo(4);
        assertThat(result.getMinPlaytime()).isEqualTo(30);
        assertThat(result.getMaxPlaytime()).isEqualTo(120);
        assertThat(result.getYearPublished()).isEqualTo(2007);
        assertThat(result.getPublisher()).isEqualTo("Editorial");
        assertThat(result.getDesigners()).containsExactly("Klaus Teuber");
        assertThat(result.getCategories()).containsExactly("Estrategia");
        assertThat(result.getMechanics()).containsExactly("Dados");
        assertThat(result.getImageUrl()).isEqualTo("http://img");
        assertThat(result.getThumbnailUrl()).isEqualTo("http://thumb");
        assertThat(result.getNotes()).isEqualTo("Nota nueva");
        assertThat(result.getDateAdded()).isEqualTo(LocalDate.of(2026, 1, 1));

        ArgumentCaptor<BoardGame> captor = ArgumentCaptor.forClass(BoardGame.class);
        verify(boardGameRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("bg1");
        assertThat(captor.getValue().getTitle()).isEqualTo("Nuevo título");
    }

    @Test
    void delete_deletesBoardGame_whenExists() {
        BoardGame game = sampleGame("bg1", "Catan");
        when(boardGameRepository.findById("bg1")).thenReturn(Optional.of(game));

        boardGameService.delete("bg1");

        verify(boardGameRepository).deleteById("bg1");
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(boardGameRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardGameService.delete("nope"))
                .isInstanceOf(BoardGameNotFoundException.class)
                .hasMessageContaining("nope");
    }
}