package com.wikicollection.domain.port.in;

import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserProfileUseCase {

    User getPublicProfile(String username);

    Page<Book> getPublicBooks(String username, Pageable pageable);

    Page<Game> getPublicGames(String username, Pageable pageable);

    Page<BoardGame> getPublicBoardGames(String username, Pageable pageable);

    Page<MagicCard> getPublicMagicCards(String username, Pageable pageable);

    Page<Deck> getPublicDecks(String username, Pageable pageable);

    Page<MovieShow> getPublicMovieShows(String username, Pageable pageable);
}
