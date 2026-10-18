package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.wikicollection.application.exception.BoardGameExpansionException;
import com.wikicollection.application.exception.BoardGameNotFoundException;
import com.wikicollection.application.exception.ExternalCatalogException;
import com.wikicollection.application.exception.ForbiddenException;
import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameExpansion;
import com.wikicollection.domain.model.BoardGameExpansionAddResult;
import com.wikicollection.domain.model.BoardGameExpansionCatalog;
import com.wikicollection.domain.model.BoardGameExpansionInfo;
import com.wikicollection.domain.model.BoardGameExpansionSkip;
import com.wikicollection.domain.model.BoardGameStatus;
import com.wikicollection.domain.model.BoardGameType;
import com.wikicollection.domain.model.UserOwned;
import com.wikicollection.domain.port.out.BoardGameRepository;
import com.wikicollection.domain.port.out.ExternalBoardGameCatalogClient;
import com.wikicollection.domain.port.out.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoardGameExpansionServiceTest {

    private static final String VIEWER = "u1";
    private static final String BASE_ID = "bg1";
    private static final String BASE_BGG_ID = "205637";

    @Mock
    private BoardGameRepository boardGameRepository;

    @Mock
    private ExternalBoardGameCatalogClient bggXmlClient;

    @Mock
    private OwnerResolver ownerResolver;

    private BoardGameExpansionService service;

    @BeforeEach
    void setUp() {
        // Validador real: el 403 lo tiene que dar él, no un stub.
        OwnershipValidator validator = new OwnershipValidator(mock(UserRepository.class), "admin");
        service = new BoardGameExpansionService(boardGameRepository, validator, bggXmlClient, ownerResolver);
        lenient().when(ownerResolver.resolveOwner(any())).thenAnswer(i -> UserOwned.builder()
                .ownerId(i.getArgument(0)).ownerName("Javi").build());
        lenient().when(boardGameRepository.save(any(BoardGame.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static <T> T mock(Class<T> type) {
        return org.mockito.Mockito.mock(type);
    }

    private BoardGame base(BoardGameStatus status) {
        return BoardGame.builder()
                .id(BASE_ID)
                .ownerId(VIEWER)
                .title("Arkham Horror: The Card Game")
                .bggId(BASE_BGG_ID)
                .status(status)
                .build();
    }

    private BoardGameExpansionInfo expansionInfo(String bggId, String... baseBggIds) {
        return new BoardGameExpansionInfo(
                bggId, "Expansión " + bggId, "Una expansión", 2017, 2, 4, 30, 60,
                "Kosmos", List.of("Klaus Teuber"), List.of("Negociación"), List.of("Dados"),
                "http://img" + bggId, "http://thumb" + bggId, new BigDecimal("8.1"),
                BoardGameType.BOARDGAME_EXPANSION, List.of(baseBggIds));
    }

    private void givenBase(BoardGame base) {
        when(boardGameRepository.findById(base.getId())).thenReturn(Optional.of(base));
    }

    // --------------------------------------------------------------- catálogo (lectura)

    @Test
    void expansionsOf_throws404_whenBaseDoesNotExist() {
        when(boardGameRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.expansionsOf("missing", VIEWER))
                .isInstanceOf(BoardGameNotFoundException.class);
    }

    @Test
    void expansionsOf_marksExpansionsAlreadyInCollection() {
        givenBase(base(BoardGameStatus.OWNED));
        when(bggXmlClient.expansionsOf(BASE_BGG_ID))
                .thenReturn(List.of(expansionInfo("926", BASE_BGG_ID), expansionInfo("325", BASE_BGG_ID)));
        when(boardGameRepository.findByOwnerIdAndBggIdIn(VIEWER, List.of("926", "325")))
                .thenReturn(List.of(BoardGame.builder().id("bg2").ownerId(VIEWER).bggId("926").build()));

        BoardGameExpansionCatalog catalog = service.expansionsOf(BASE_ID, VIEWER);

        assertThat(catalog.baseGameId()).isEqualTo(BASE_ID);
        assertThat(catalog.baseBggId()).isEqualTo(BASE_BGG_ID);
        assertThat(catalog.expansions()).hasSize(2);
        BoardGameExpansion first = catalog.expansions().get(0);
        assertThat(first.bggId()).isEqualTo("926");
        assertThat(first.title()).isEqualTo("Expansión 926");
        assertThat(first.inCollection()).isTrue();
        assertThat(catalog.expansions().get(1).inCollection()).isFalse();
    }

    @Test
    void expansionsOf_marksNothingAsOwned_withoutViewer() {
        givenBase(base(BoardGameStatus.OWNED));
        when(bggXmlClient.expansionsOf(BASE_BGG_ID)).thenReturn(List.of(expansionInfo("926", BASE_BGG_ID)));

        BoardGameExpansionCatalog catalog = service.expansionsOf(BASE_ID, null);

        assertThat(catalog.expansions()).singleElement()
                .extracting(BoardGameExpansion::inCollection).isEqualTo(Boolean.FALSE);
        verify(boardGameRepository, never()).findByOwnerIdAndBggIdIn(any(), anyList());
    }

    @Test
    void expansionsOf_skipsCatalogCall_whenBaseHasNoBggId() {
        givenBase(BoardGame.builder().id(BASE_ID).ownerId(VIEWER).title("Sin bggId")
                .status(BoardGameStatus.OWNED).build());

        BoardGameExpansionCatalog catalog = service.expansionsOf(BASE_ID, VIEWER);

        assertThat(catalog.expansions()).isEmpty();
        verify(bggXmlClient, never()).expansionsOf(any());
    }

    @Test
    void expansionsOf_propagatesCatalogFailure_soTheApiCanAnswer502() {
        givenBase(base(BoardGameStatus.OWNED));
        when(bggXmlClient.expansionsOf(BASE_BGG_ID))
                .thenThrow(new ExternalCatalogException("BGG devolvió error 500"));

        assertThatThrownBy(() -> service.expansionsOf(BASE_ID, VIEWER))
                .isInstanceOf(ExternalCatalogException.class);
    }

    // ------------------------------------------------------------------- alta (escritura)

    @Test
    void addExpansions_throws404_whenBaseDoesNotExist() {
        when(boardGameRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addExpansions("missing", List.of("926"), VIEWER))
                .isInstanceOf(BoardGameNotFoundException.class);
    }

    @Test
    void addExpansions_throws403_whenBaseBelongsToAnotherUser() {
        BoardGame other = base(BoardGameStatus.OWNED);
        other.setOwnerId("u2");
        givenBase(other);

        assertThatThrownBy(() -> service.addExpansions(BASE_ID, List.of("926"), VIEWER))
                .isInstanceOf(ForbiddenException.class);
        verify(bggXmlClient, never()).findByBggIds(anyList());
    }

    @Test
    void addExpansions_throws422_whenBaseIsNotOwned() {
        givenBase(base(BoardGameStatus.WISHLIST));

        assertThatThrownBy(() -> service.addExpansions(BASE_ID, List.of("926"), VIEWER))
                .isInstanceOf(BoardGameExpansionException.class)
                .hasMessageContaining("OWNED");
        verify(bggXmlClient, never()).findByBggIds(anyList());
    }

    @Test
    void addExpansions_throws422_whenBaseHasNoBggId() {
        givenBase(BoardGame.builder().id(BASE_ID).ownerId(VIEWER).title("Sin bggId")
                .status(BoardGameStatus.OWNED).build());

        assertThatThrownBy(() -> service.addExpansions(BASE_ID, List.of("926"), VIEWER))
                .isInstanceOf(BoardGameExpansionException.class)
                .hasMessageContaining("bggId");
    }

    @Test
    void addExpansions_throws400_whenBggIdsIsEmpty() {
        givenBase(base(BoardGameStatus.OWNED));

        assertThatThrownBy(() -> service.addExpansions(BASE_ID, List.of(), VIEWER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addExpansions_skipsWhatIsAlreadyInCollection_andIsIdempotent() {
        givenBase(base(BoardGameStatus.OWNED));
        when(boardGameRepository.findByOwnerIdAndBggIdIn(VIEWER, List.of("926", "325")))
                .thenReturn(List.of(BoardGame.builder().id("bg2").ownerId(VIEWER).bggId("926").build()));
        when(bggXmlClient.findByBggIds(List.of("325")))
                .thenReturn(List.of(expansionInfo("325", BASE_BGG_ID)));

        BoardGameExpansionAddResult result = service.addExpansions(BASE_ID, List.of("926", "325"), VIEWER);

        assertThat(result.created()).hasSize(1);
        assertThat(result.created().get(0).getBggId()).isEqualTo("325");
        assertThat(result.skipped()).containsExactly(
                new BoardGameExpansionSkip("926", BoardGameExpansionSkip.REASON_ALREADY_IN_COLLECTION));
        verify(bggXmlClient).findByBggIds(List.of("325"));
        verify(bggXmlClient, never()).expansionsOf(any());
    }

    @Test
    void addExpansions_createsExpansionLinkedToItsBase() {
        givenBase(base(BoardGameStatus.OWNED));
        when(bggXmlClient.findByBggIds(List.of("926")))
                .thenReturn(List.of(expansionInfo("926", BASE_BGG_ID)));

        BoardGameExpansionAddResult result = service.addExpansions(BASE_ID, List.of("926"), VIEWER);

        ArgumentCaptor<BoardGame> captor = ArgumentCaptor.forClass(BoardGame.class);
        verify(boardGameRepository).save(captor.capture());
        BoardGame created = captor.getValue();
        assertThat(result.created()).containsExactly(created);
        assertThat(result.skipped()).isEmpty();
        assertThat(created.getBggType()).isEqualTo(BoardGameType.BOARDGAME_EXPANSION);
        assertThat(created.getParentId()).isEqualTo(BASE_ID);
        assertThat(created.getParentTitle()).isEqualTo("Arkham Horror: The Card Game");
        assertThat(created.getOwnerId()).isEqualTo(VIEWER);
        assertThat(created.getStatus()).isEqualTo(BoardGameStatus.OWNED);
        assertThat(created.getDateAdded()).isEqualTo(LocalDate.now());
        assertThat(created.getAcquisitionPrice()).isNull();
        assertThat(created.getTitle()).isEqualTo("Expansión 926");
        assertThat(created.getDescription()).isEqualTo("Una expansión");
        assertThat(created.getYearPublished()).isEqualTo(2017);
        assertThat(created.getMinPlayers()).isEqualTo(2);
        assertThat(created.getMaxPlayers()).isEqualTo(4);
        assertThat(created.getMinPlaytime()).isEqualTo(30);
        assertThat(created.getMaxPlaytime()).isEqualTo(60);
        assertThat(created.getPublisher()).isEqualTo("Kosmos");
        assertThat(created.getDesigners()).containsExactly("Klaus Teuber");
        assertThat(created.getCategories()).containsExactly("Negociación");
        assertThat(created.getMechanics()).containsExactly("Dados");
        assertThat(created.getImageUrl()).isEqualTo("http://img926");
        assertThat(created.getThumbnailUrl()).isEqualTo("http://thumb926");
        assertThat(created.getBggRating()).isEqualByComparingTo(new BigDecimal("8.1"));
        assertThat(created.getUserOwned()).isNotNull();
    }

    @Test
    void addExpansions_throws422_whenBggDoesNotKnowTheId() {
        givenBase(base(BoardGameStatus.OWNED));
        when(bggXmlClient.findByBggIds(List.of("999999"))).thenReturn(List.of());

        assertThatThrownBy(() -> service.addExpansions(BASE_ID, List.of("999999"), VIEWER))
                .isInstanceOf(BoardGameExpansionException.class)
                .hasMessageContaining("999999");
    }

    @Test
    void addExpansions_throws422_whenTheIdIsNotAnExpansion() {
        givenBase(base(BoardGameStatus.OWNED));
        BoardGameExpansionInfo notAnExpansion = new BoardGameExpansionInfo(
                "31260", "Catan", null, 2007, 3, 4, 60, 120, "Kosmos", List.of(), List.of(), List.of(),
                null, null, null, BoardGameType.BOARDGAME, List.of());
        when(bggXmlClient.findByBggIds(List.of("31260"))).thenReturn(List.of(notAnExpansion));

        assertThatThrownBy(() -> service.addExpansions(BASE_ID, List.of("31260"), VIEWER))
                .isInstanceOf(BoardGameExpansionException.class)
                .hasMessageContaining("expansión");
    }

    @Test
    void addExpansions_throws422_whenTheExpansionBelongsToAnotherBase() {
        givenBase(base(BoardGameStatus.OWNED));
        when(bggXmlClient.findByBggIds(List.of("926")))
                .thenReturn(List.of(expansionInfo("926", "13")));

        assertThatThrownBy(() -> service.addExpansions(BASE_ID, List.of("926"), VIEWER))
                .isInstanceOf(BoardGameExpansionException.class)
                .hasMessageContaining(BASE_BGG_ID);
        verify(boardGameRepository, never()).save(any(BoardGame.class));
    }

    @Test
    void addExpansions_propagatesCatalogFailure_soTheApiCanAnswer502() {
        givenBase(base(BoardGameStatus.OWNED));
        when(bggXmlClient.findByBggIds(List.of("926")))
                .thenThrow(new ExternalCatalogException("BGG no responde"));

        assertThatThrownBy(() -> service.addExpansions(BASE_ID, List.of("926"), VIEWER))
                .isInstanceOf(ExternalCatalogException.class);
        verify(boardGameRepository, never()).save(any(BoardGame.class));
    }
}
