package com.wikicollection.infrastructure.adapter.in.web;

import java.util.LinkedHashMap;
import java.util.Map;

import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.port.in.UserProfileUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.BoardGameDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.BoardGameResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.BookDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.BookResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.GameDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.GameResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.MagicCardDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.MagicCardResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.MovieShowDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.MovieShowResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.PublicProfileResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/users")
@Validated
@Tag(name = "Perfiles públicos", description = "Perfiles y colecciones públicas de usuarios")
public class UserProfileController {

    private final UserProfileUseCase profileUseCase;
    private final BookDtoMapper bookMapper;
    private final GameDtoMapper gameMapper;
    private final BoardGameDtoMapper boardGameMapper;
    private final MagicCardDtoMapper magicCardMapper;
    private final DeckDtoMapper deckMapper;
    private final MovieShowDtoMapper movieShowMapper;
    private final ResponseVisibility visibility;

    public UserProfileController(UserProfileUseCase profileUseCase,
                                 BookDtoMapper bookMapper,
                                 GameDtoMapper gameMapper,
                                 BoardGameDtoMapper boardGameMapper,
                                 MagicCardDtoMapper magicCardMapper,
                                 DeckDtoMapper deckMapper,
                                 MovieShowDtoMapper movieShowMapper,
                                 ResponseVisibility visibility) {
        this.profileUseCase = profileUseCase;
        this.bookMapper = bookMapper;
        this.gameMapper = gameMapper;
        this.boardGameMapper = boardGameMapper;
        this.magicCardMapper = magicCardMapper;
        this.deckMapper = deckMapper;
        this.movieShowMapper = movieShowMapper;
        this.visibility = visibility;
    }

    @GetMapping("/{username}")
    @Operation(summary = "Obtiene el perfil público de un usuario")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    public PublicProfileResponse profile(
            @Parameter(description = "Nombre de usuario") @PathVariable String username) {
        var user = profileUseCase.getPublicProfile(username);
        Pageable single = PageRequest.of(0, 1);
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put(CollectionType.BOOKS.getKey(), profileUseCase.getPublicBooks(username, single).getTotalElements());
        counts.put(CollectionType.GAMES.getKey(), profileUseCase.getPublicGames(username, single).getTotalElements());
        counts.put(CollectionType.BOARDGAMES.getKey(),
                profileUseCase.getPublicBoardGames(username, single).getTotalElements());
        counts.put(CollectionType.MAGIC.getKey(),
                profileUseCase.getPublicMagicCards(username, single).getTotalElements());
        counts.put(CollectionType.DECKS.getKey(), profileUseCase.getPublicDecks(username, single).getTotalElements());
        counts.put(CollectionType.MOVIESHOWS.getKey(),
                profileUseCase.getPublicMovieShows(username, single).getTotalElements());
        return PublicProfileResponse.from(user, counts);
    }

    @GetMapping("/{username}/books")
    @Operation(summary = "Libros públicos de un usuario")
    public Page<BookResponse> books(
            @Parameter(description = "Nombre de usuario") @PathVariable String username,
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size) {
        return profileUseCase.getPublicBooks(username, PageRequest.of(page, size)).map(book -> {
            var response = bookMapper.toResponse(book);
            return visibility.canSeePrivate(book.getOwnerId(), CollectionType.BOOKS) ? response : response.withoutPrivate();
        });
    }

    @GetMapping("/{username}/games")
    @Operation(summary = "Juegos públicos de un usuario")
    public Page<GameResponse> games(
            @Parameter(description = "Nombre de usuario") @PathVariable String username,
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size) {
        return profileUseCase.getPublicGames(username, PageRequest.of(page, size)).map(game -> {
            var response = gameMapper.toResponse(game);
            return visibility.canSeePrivate(game.getOwnerId(), CollectionType.GAMES) ? response : response.withoutPrivate();
        });
    }

    @GetMapping("/{username}/boardgames")
    @Operation(summary = "Juegos de mesa públicos de un usuario")
    public Page<BoardGameResponse> boardGames(
            @Parameter(description = "Nombre de usuario") @PathVariable String username,
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size) {
        return profileUseCase.getPublicBoardGames(username, PageRequest.of(page, size)).map(boardGame -> {
            var response = boardGameMapper.toResponse(boardGame);
            return visibility.canSeePrivate(boardGame.getOwnerId(), CollectionType.BOARDGAMES) ? response : response.withoutPrivate();
        });
    }

    @GetMapping("/{username}/magic")
    @Operation(summary = "Cartas Magic públicas de un usuario")
    public Page<MagicCardResponse> magic(
            @Parameter(description = "Nombre de usuario") @PathVariable String username,
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size) {
        return profileUseCase.getPublicMagicCards(username, PageRequest.of(page, size)).map(card -> {
            var response = magicCardMapper.toResponse(card);
            return visibility.canSeePrivate(card.getOwnerId(), CollectionType.MAGIC) ? response : response.withoutPrivate();
        });
    }

    @GetMapping("/{username}/decks")
    @Operation(summary = "Mazos públicos de un usuario")
    public Page<DeckResponse> decks(
            @Parameter(description = "Nombre de usuario") @PathVariable String username,
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size) {
        return profileUseCase.getPublicDecks(username, PageRequest.of(page, size))
                .map(deckMapper::toResponse);
    }

    @GetMapping("/{username}/movieshows")
    @Operation(summary = "Películas y series públicas de un usuario")
    public Page<MovieShowResponse> movieShows(
            @Parameter(description = "Nombre de usuario") @PathVariable String username,
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size) {
        return profileUseCase.getPublicMovieShows(username, PageRequest.of(page, size)).map(movieShow -> {
            var response = movieShowMapper.toResponse(movieShow);
            return visibility.canSeePrivate(movieShow.getOwnerId(), CollectionType.MOVIESHOWS) ? response : response.withoutPrivate();
        });
    }
}
