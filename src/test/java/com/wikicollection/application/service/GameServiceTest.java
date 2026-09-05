package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.wikicollection.application.exception.GameNotFoundException;
import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GamePlatform;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.model.GameStatus;
import com.wikicollection.domain.port.out.GameRepository;

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
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameService gameService;

    private Game sampleGame() {
        return Game.builder()
                .title("The Witcher 3")
                .platform(GamePlatform.PC)
                .status(GameStatus.PLAYING)
                .build();
    }

    @Test
    void search_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        GameSearchCriteria criteria = new GameSearchCriteria("witc", GamePlatform.PC, GameStatus.PLAYING);
        when(gameRepository.search(criteria, pageable)).thenReturn(Page.empty());

        Page<Game> result = gameService.search(criteria, pageable);

        assertThat(result).isEmpty();
        verify(gameRepository).search(criteria, pageable);
    }

    @Test
    void findById_returnsGame_whenExists() {
        Game game = sampleGame();
        game.setId("g1");
        when(gameRepository.findById("g1")).thenReturn(Optional.of(game));

        Game result = gameService.findById("g1");

        assertThat(result).isSameAs(game);
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(gameRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.findById("nope"))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void save_delegatesToRepository() {
        Game game = sampleGame();
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Game result = gameService.save(game);

        assertThat(result).isSameAs(game);
        verify(gameRepository).save(game);
    }

    @Test
    void update_throwsNotFound_whenMissing() {
        when(gameRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.update("nope", sampleGame()))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    void update_appliesFieldsAndKeepsId() {
        Game existing = sampleGame();
        existing.setId("g1");
        existing.setTitle("Título original");

        Game updates = sampleGame();
        updates.setTitle("Nuevo título");
        updates.setComment("Nuevo comentario");
        updates.setStatus(GameStatus.COMPLETED);
        updates.setUserRating(5);

        when(gameRepository.findById("g1")).thenReturn(Optional.of(existing));
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Game result = gameService.update("g1", updates);

        assertThat(result.getId()).isEqualTo("g1");
        assertThat(result.getTitle()).isEqualTo("Nuevo título");
        assertThat(result.getComment()).isEqualTo("Nuevo comentario");
        assertThat(result.getStatus()).isEqualTo(GameStatus.COMPLETED);
        assertThat(result.getUserRating()).isEqualTo(5);

        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("g1");
        assertThat(captor.getValue().getTitle()).isEqualTo("Nuevo título");
    }

    @Test
    void delete_deletesGame_whenExists() {
        Game game = sampleGame();
        game.setId("g1");
        when(gameRepository.findById("g1")).thenReturn(Optional.of(game));

        gameService.delete("g1");

        verify(gameRepository).deleteById("g1");
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(gameRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.delete("nope"))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("nope");
    }
}