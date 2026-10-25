package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.wikicollection.application.exception.BoardGameNotFoundException;
import com.wikicollection.application.exception.BookNotFoundException;
import com.wikicollection.application.exception.GameNotFoundException;
import com.wikicollection.application.exception.MagicCardNotFoundException;
import com.wikicollection.application.exception.MovieShowNotFoundException;
import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameSearchCriteria;
import com.wikicollection.domain.model.BoardGameStatus;
import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookSearchCriteria;
import com.wikicollection.domain.model.BookState;
import com.wikicollection.domain.model.BookType;
import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameSearchCriteria;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.model.MovieSearchCriteria;
import com.wikicollection.domain.model.MovieShow;
import com.wikicollection.domain.port.out.BoardGameRepository;
import com.wikicollection.domain.port.out.BookRepository;
import com.wikicollection.domain.port.out.DeckRepository;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.domain.port.out.MagicCardRepository;
import com.wikicollection.domain.port.out.MovieShowRepository;
import com.wikicollection.infrastructure.config.CacheTestSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Issue #439: los servicios cachean detalle y listado con {@code @Cacheable} pero no
 * invalidaban nada al escribir, asi que la API servia datos obsoletos durante el TTL
 * (1 hora). Un recurso borrado seguia devolviendo 200 y uno creado no aparecia en el
 * listado.
 *
 * <p>Estos tests usan el {@link CacheManager} real de la aplicacion para que las
 * anotaciones de cache sean EFFECTIVAS (un test puramente unitario con
 * {@code @InjectMocks} no pasa por el proxy y no detectaria el bug).
 */
@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false",
        "app.boardgame-status-migration.enabled=false"})
class CacheInvalidationServiceTest {

    private static final String OWNER = "u1";
    private static final Pageable PAGE = PageRequest.of(0, 20);

    private static <T> Page<T> pageOf(List<T> content) {
        return new PageImpl<>(content, PAGE, content.size());
    }

    @Autowired
    private BookService bookService;
    @Autowired
    private GameService gameService;
    @Autowired
    private DeckService deckService;
    @Autowired
    private MovieShowService movieShowService;
    @Autowired
    private MagicCardService magicCardService;
    @Autowired
    private BoardGameService boardGameService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private BookRepository bookRepository;
    @MockitoBean
    private GameRepository gameRepository;
    @MockitoBean
    private DeckRepository deckRepository;
    @MockitoBean
    private MovieShowRepository movieShowRepository;
    @MockitoBean
    private MagicCardRepository magicCardRepository;
    @MockitoBean
    private BoardGameRepository boardGameRepository;
    @MockitoBean
    private ExternalMagicCardCatalogClient magicCardCatalogClient;
    @MockitoBean
    private OwnerResolver ownerResolver;

    @BeforeEach
    void setUp() {
        CacheTestSupport.clearAll(cacheManager);
        lenient().when(ownerResolver.resolveOwner(any()))
                .thenAnswer(i -> com.wikicollection.domain.model.UserOwned.builder()
                        .ownerId(i.getArgument(0)).ownerName("Javi").build());
        lenient().when(magicCardRepository.search(any(MagicCardSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());
        lenient().when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(deckRepository.save(any(Deck.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(movieShowRepository.save(any(MovieShow.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(magicCardRepository.save(any(MagicCard.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(boardGameRepository.save(any(BoardGame.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ---------------------------------------------------------------- libros

    @Test
    void bookDetail_isEvicted_afterDelete() {
        Book book = Book.builder().id("b1").ownerId(OWNER).title("Dune").state(BookState.TO_READ)
                .type(BookType.NOVEL).genres(List.of("Ficción")).build();
        when(bookRepository.findById("b1")).thenReturn(Optional.of(book));

        bookService.findById("b1");
        bookService.delete("b1", OWNER);
        when(bookRepository.findById("b1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById("b1"))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void bookList_isEvicted_afterSave() {
        BookSearchCriteria criteria = new BookSearchCriteria(null, null, null, null, null, null, null);
        when(bookRepository.search(any(BookSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        bookService.search(criteria, PAGE, "mine", OWNER);

        Book created = Book.builder().id("b1").title("Dune").type(BookType.NOVEL).state(BookState.TO_READ)
                .genres(List.of("Ficción")).build();
        bookService.save(created, OWNER);

        when(bookRepository.search(any(BookSearchCriteria.class), any(Pageable.class)))
                .thenReturn(pageOf(List.of(created)));
        assertThat(bookService.search(criteria, PAGE, "mine", OWNER).getContent()).hasSize(1);
    }

    // ---------------------------------------------------------------- juegos

    @Test
    void gameDetail_isEvicted_afterDelete() {
        Game game = Game.builder().id("g1").ownerId(OWNER).title("Hollow Knight").build();
        when(gameRepository.findById("g1")).thenReturn(Optional.of(game));

        gameService.findById("g1");
        gameService.delete("g1", OWNER);
        when(gameRepository.findById("g1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.findById("g1"))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void gameList_isEvicted_afterSave() {
        GameSearchCriteria criteria = new GameSearchCriteria(null, null, null, null, null, null);
        when(gameRepository.search(any(GameSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        gameService.search(criteria, PAGE, "mine", OWNER);

        Game created = Game.builder().id("g1").title("Hollow Knight").build();
        gameService.save(created, false, OWNER);

        when(gameRepository.search(any(GameSearchCriteria.class), any(Pageable.class)))
                .thenReturn(pageOf(List.of(created)));
        assertThat(gameService.search(criteria, PAGE, "mine", OWNER).getContent()).hasSize(1);
    }

    // ---------------------------------------------------------------- mazos

    @Test
    void deckDetail_isEvicted_afterDelete() {
        Deck deck = Deck.builder().id("d1").ownerId(OWNER).name("Azul").build();
        when(deckRepository.findById("d1")).thenReturn(Optional.of(deck));

        deckService.findById("d1");
        deckService.delete("d1", OWNER);
        when(deckRepository.findById("d1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.findById("d1"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deckList_isEvicted_afterSave() {
        when(deckRepository.findByOwnerId(any(), any(Pageable.class))).thenReturn(Page.empty());

        deckService.findAll(PAGE, "mine", OWNER);

        Deck created = Deck.builder().id("d1").name("Azul").build();
        deckService.save(created, OWNER);

        when(deckRepository.findByOwnerId(any(), any(Pageable.class)))
                .thenReturn(pageOf(List.of(created)));
        assertThat(deckService.findAll(PAGE, "mine", OWNER).getContent()).hasSize(1);
    }

    @Test
    void deckDetail_isEvicted_afterAddCard() {
        Deck deck = Deck.builder().id("d1").ownerId(OWNER).name("Azul")
                .cards(new java.util.ArrayList<>()).build();
        when(deckRepository.findById("d1")).thenReturn(Optional.of(deck));
        when(magicCardCatalogClient.findById("card-1"))
                .thenReturn(MagicCard.builder().scryfallId("card-1").name("Island").build());

        deckService.findById("d1");
        deckService.addCard("d1", "card-1", 1, OWNER);
        when(deckRepository.findById("d1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deckService.findById("d1"))
                .isInstanceOf(RuntimeException.class);
    }

    // ---------------------------------------------------------------- peliculas

    @Test
    void movieDetail_isEvicted_afterDelete() {
        MovieShow movie = MovieShow.builder().id("m1").ownerId(OWNER).title("Arrival").build();
        when(movieShowRepository.findById("m1")).thenReturn(Optional.of(movie));

        movieShowService.findById("m1");
        movieShowService.delete("m1", OWNER);
        when(movieShowRepository.findById("m1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieShowService.findById("m1"))
                .isInstanceOf(MovieShowNotFoundException.class);
    }

    @Test
    void movieList_isEvicted_afterSave() {
        MovieSearchCriteria criteria = new MovieSearchCriteria(null, null, null, null, null, null);
        when(movieShowRepository.findByCriteria(any(MovieSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        movieShowService.search(criteria, PAGE, "mine", OWNER);

        MovieShow created = MovieShow.builder().id("m1").title("Arrival").build();
        movieShowService.save(created, OWNER);

        when(movieShowRepository.findByCriteria(any(MovieSearchCriteria.class), any(Pageable.class)))
                .thenReturn(pageOf(List.of(created)));
        assertThat(movieShowService.search(criteria, PAGE, "mine", OWNER).getContent()).hasSize(1);
    }

    // ---------------------------------------------------------------- magic

    @Test
    void magicDetail_isEvicted_afterDelete() {
        MagicCard card = MagicCard.builder().id("mc1").ownerId(OWNER).name("Island")
                .scryfallId("card-1").build();
        when(magicCardRepository.findById("mc1")).thenReturn(Optional.of(card));

        magicCardService.findById("mc1");
        magicCardService.delete("mc1", OWNER);
        when(magicCardRepository.findById("mc1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> magicCardService.findById("mc1"))
                .isInstanceOf(MagicCardNotFoundException.class);
    }

    @Test
    void magicList_isEvicted_afterAddFromScryfall() {
        MagicCardSearchCriteria criteria = new MagicCardSearchCriteria(null);
        when(magicCardRepository.search(any(MagicCardSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        magicCardService.search(criteria, PAGE, "mine", OWNER);

        MagicCard fetched = MagicCard.builder().scryfallId("card-1").name("Island").build();
        when(magicCardCatalogClient.findById("card-1")).thenReturn(fetched);
        MagicCard added = magicCardService.addFromScryfall("card-1", 1, OWNER);

        when(magicCardRepository.search(any(MagicCardSearchCriteria.class), any(Pageable.class)))
                .thenReturn(pageOf(List.of(added)));
        assertThat(magicCardService.search(criteria, PAGE, "mine", OWNER).getContent()).hasSize(1);
    }

    // ---------------------------------------------------------------- juegos de mesa

    @Test
    void boardGameDetail_isEvicted_afterDelete() {
        BoardGame game = BoardGame.builder().id("bg1").ownerId(OWNER).title("Catan")
                .status(BoardGameStatus.OWNED).build();
        when(boardGameRepository.findById("bg1")).thenReturn(Optional.of(game));

        boardGameService.findById("bg1");
        boardGameService.delete("bg1", OWNER);
        when(boardGameRepository.findById("bg1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardGameService.findById("bg1"))
                .isInstanceOf(BoardGameNotFoundException.class);
    }

    @Test
    void boardGameList_isEvicted_afterSave() {
        BoardGameSearchCriteria criteria = new BoardGameSearchCriteria(null, null, null, null, null);
        when(boardGameRepository.search(any(BoardGameSearchCriteria.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        boardGameService.search(criteria, PAGE, "mine", OWNER);

        BoardGame created = BoardGame.builder().id("bg1").title("Catan")
                .status(BoardGameStatus.OWNED).build();
        boardGameService.save(created, OWNER);

        when(boardGameRepository.search(any(BoardGameSearchCriteria.class), any(Pageable.class)))
                .thenReturn(pageOf(List.of(created)));
        assertThat(boardGameService.search(criteria, PAGE, "mine", OWNER).getContent()).hasSize(1);
    }
}
