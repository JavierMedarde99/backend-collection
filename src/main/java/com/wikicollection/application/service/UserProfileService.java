package com.wikicollection.application.service;

import com.wikicollection.application.exception.UserNotFoundException;
import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameSearchCriteria;
import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookSearchCriteria;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.CollectionVisibility;
import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.model.UserPreferences;
import com.wikicollection.domain.port.in.UserProfileUseCase;
import com.wikicollection.domain.port.out.BoardGameRepository;
import com.wikicollection.domain.port.out.BookRepository;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.domain.port.out.MagicCardRepository;
import com.wikicollection.domain.port.out.MovieShowRepository;
import com.wikicollection.domain.port.out.UserPreferencesRepository;
import com.wikicollection.domain.port.out.UserProfilePort;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService implements UserProfileUseCase {

    private final UserProfilePort userProfilePort;
    private final UserPreferencesRepository preferencesRepository;
    private final BookRepository bookRepository;
    private final GameRepository gameRepository;
    private final BoardGameRepository boardGameRepository;
    private final MagicCardRepository magicCardRepository;
    private final DeckRepository deckRepository;
    private final MovieShowRepository movieShowRepository;

    public UserProfileService(UserProfilePort userProfilePort,
                              UserPreferencesRepository preferencesRepository,
                              BookRepository bookRepository,
                              GameRepository gameRepository,
                              BoardGameRepository boardGameRepository,
                              MagicCardRepository magicCardRepository,
                              DeckRepository deckRepository,
                              MovieShowRepository movieShowRepository) {
        this.userProfilePort = userProfilePort;
        this.preferencesRepository = preferencesRepository;
        this.bookRepository = bookRepository;
        this.gameRepository = gameRepository;
        this.boardGameRepository = boardGameRepository;
        this.magicCardRepository = magicCardRepository;
        this.deckRepository = deckRepository;
        this.movieShowRepository = movieShowRepository;
    }

    @Override
    public User getPublicProfile(String username) {
        return findUser(username);
    }

    @Override
    public Page<Book> getPublicBooks(String username, Pageable pageable) {
        User user = findUser(username);
        if (!isVisible(user.getId(), CollectionType.BOOKS)) {
            return Page.empty(pageable);
        }
        return bookRepository.search(
                new BookSearchCriteria(null, null, null, null, null, user.getId(), null), pageable);
    }

    @Override
    public Page<Game> getPublicGames(String username, Pageable pageable) {
        User user = findUser(username);
        if (!isVisible(user.getId(), CollectionType.GAMES)) {
            return Page.empty(pageable);
        }
        return gameRepository.search(
                new GameSearchCriteria(null, null, null, null, user.getId(), null), pageable);
    }

    @Override
    public Page<BoardGame> getPublicBoardGames(String username, Pageable pageable) {
        User user = findUser(username);
        if (!isVisible(user.getId(), CollectionType.BOARDGAMES)) {
            return Page.empty(pageable);
        }
        return boardGameRepository.search(
                new BoardGameSearchCriteria(null, null, null, user.getId(), null), pageable);
    }

    @Override
    public Page<MagicCard> getPublicMagicCards(String username, Pageable pageable) {
        User user = findUser(username);
        if (!isVisible(user.getId(), CollectionType.MAGIC)) {
            return Page.empty(pageable);
        }
        return magicCardRepository.search(
                new MagicCardSearchCriteria(null, null, null, null, user.getId(), null), pageable);
    }

    @Override
    public Page<Deck> getPublicDecks(String username, Pageable pageable) {
        User user = findUser(username);
        if (!isVisible(user.getId(), CollectionType.DECKS)) {
            return Page.empty(pageable);
        }
        return deckRepository.findByOwnerId(user.getId(), pageable);
    }

    @Override
    public Page<MovieShow> getPublicMovieShows(String username, Pageable pageable) {
        User user = findUser(username);
        if (!isVisible(user.getId(), CollectionType.MOVIESHOWS)) {
            return Page.empty(pageable);
        }
        return movieShowRepository.findByCriteria(
                new MovieSearchCriteria(null, null, null, null, user.getId(), null), pageable);
    }

    private User findUser(String username) {
        return userProfilePort.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + username));
    }

    private boolean isVisible(String userId, CollectionType type) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> UserPreferences.defaults(userId));
        return prefs.getActiveCollections().getOrDefault(type.getKey(), true)
                && prefs.getCollectionVisibility().getOrDefault(type.getKey(), CollectionVisibility.PUBLIC)
                        == CollectionVisibility.PUBLIC;
    }
}
