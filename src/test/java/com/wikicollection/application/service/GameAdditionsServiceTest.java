package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.wikicollection.application.exception.ExternalCatalogException;
import com.wikicollection.application.exception.ForbiddenException;
import com.wikicollection.application.exception.GameAdditionsException;
import com.wikicollection.application.exception.GameNotFoundException;
import com.wikicollection.domain.model.Game;
import com.wikicollection.domain.model.GameAdditionSkip;
import com.wikicollection.domain.model.GameAdditionsAddResult;
import com.wikicollection.domain.model.GameAdditionsCatalog;
import com.wikicollection.domain.model.GameAdditionsPage;
import com.wikicollection.domain.model.GameSearchResult;
import com.wikicollection.domain.model.GameStatus;
import com.wikicollection.domain.model.UserOwned;
import com.wikicollection.domain.port.out.ExternalGameAdditionsCatalogClient;
import com.wikicollection.domain.port.out.GameRepository;
import com.wikicollection.domain.port.out.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameAdditionsServiceTest {

    private static final String VIEWER = "u1";
    private static final String BASE_ID = "g1";
    private static final String BASE_EXTERNAL_ID = "292030";

    @Mock
    private GameRepository gameRepository;

    @Mock
    private ExternalGameAdditionsCatalogClient additionsCatalogClient;

    @Mock
    private OwnerResolver ownerResolver;

    private GameAdditionsService service;

    @BeforeEach
    void setUp() {
        OwnershipValidator validator = new OwnershipValidator(org.mockito.Mockito.mock(UserRepository.class), "admin");
        service = new GameAdditionsService(gameRepository, validator, additionsCatalogClient, ownerResolver);
        lenient().when(ownerResolver.resolveOwner(any())).thenAnswer(i -> UserOwned.builder()
                .ownerId(i.getArgument(0)).ownerName("Javi").build());
        lenient().when(gameRepository.save(any(Game.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Game base(GameStatus status) {
        return Game.builder()
                .id(BASE_ID)
                .ownerId(VIEWER)
                .title("The Witcher 3: Wild Hunt")
                .externalId(BASE_EXTERNAL_ID)
                .externalSource("RAWG")
                .platform("PC")
                .status(status)
                .build();
    }

    private void givenBase(Game base) {
        when(gameRepository.findById(base.getId())).thenReturn(Optional.of(base));
    }

    private GameSearchResult addition(String externalId, String platform) {
        return new GameSearchResult(externalId, "Adición " + externalId, "desc", "RPG", platform,
                "CD Projekt Red", "CD Projekt Red", LocalDate.of(2016, 5, 31),
                "http://thumb/" + externalId, "RAWG");
    }

    private void givenCatalog(GameSearchResult... additions) {
        when(additionsCatalogClient.getAdditions(BASE_EXTERNAL_ID))
                .thenReturn(new GameAdditionsPage(List.of(additions), 12));
    }

    // ------------------------------------------------------------------ catálogo (lectura)

    @Test
    void additionsOf_throws404_whenBaseDoesNotExist() {
        when(gameRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.additionsOf("missing", VIEWER))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void additionsOf_throws422_whenBaseDoesNotComeFromRawg() {
        givenBase(Game.builder().id(BASE_ID).ownerId(VIEWER).title("Sin RAWG")
                .externalId("999").externalSource("FreeToGame").status(GameStatus.OWNED).build());

        assertThatThrownBy(() -> service.additionsOf(BASE_ID, VIEWER))
                .isInstanceOf(GameAdditionsException.class)
                .hasMessageContaining("RAWG");
        verify(additionsCatalogClient, never()).getAdditions(any());
    }

    @Test
    void additionsOf_throws422_whenBaseHasNoExternalId() {
        givenBase(Game.builder().id(BASE_ID).ownerId(VIEWER).title("Sin externalId")
                .externalSource("RAWG").status(GameStatus.OWNED).build());

        assertThatThrownBy(() -> service.additionsOf(BASE_ID, VIEWER))
                .isInstanceOf(GameAdditionsException.class);
        verify(additionsCatalogClient, never()).getAdditions(any());
    }

    @Test
    void additionsOf_marksWhatIsInCollection_andWhatIsAlreadyLinkedToThisBase() {
        givenBase(base(GameStatus.OWNED));
        givenCatalog(addition("355880", "PC"), addition("378648", "PC"), addition("999", "PC"));
        when(gameRepository.findByOwnerIdAndExternalIdIn(VIEWER, List.of("355880", "378648", "999")))
                .thenReturn(List.of(
                        Game.builder().id("d1").ownerId(VIEWER).externalId("355880").parentId(BASE_ID).build(),
                        Game.builder().id("d2").ownerId(VIEWER).externalId("378648").parentId("other").build()));

        GameAdditionsCatalog catalog = service.additionsOf(BASE_ID, VIEWER);

        assertThat(catalog.baseGameId()).isEqualTo(BASE_ID);
        assertThat(catalog.baseExternalId()).isEqualTo(BASE_EXTERNAL_ID);
        assertThat(catalog.totalAvailable()).isEqualTo(12);
        assertThat(catalog.additions()).hasSize(3);
        assertThat(catalog.additions().get(0).inCollection()).isTrue();
        assertThat(catalog.additions().get(0).alreadyLinked()).isTrue();
        assertThat(catalog.additions().get(1).inCollection()).isTrue();
        assertThat(catalog.additions().get(1).alreadyLinked()).isFalse();
        assertThat(catalog.additions().get(2).inCollection()).isFalse();
        assertThat(catalog.additions().get(2).alreadyLinked()).isFalse();
        assertThat(catalog.additions().get(0).title()).isEqualTo("Adición 355880");
        assertThat(catalog.additions().get(0).genres()).containsExactly("RPG");
        assertThat(catalog.additions().get(0).externalSource()).isEqualTo("RAWG");
    }

    @Test
    void additionsOf_marksNothing_withoutViewer() {
        givenBase(base(GameStatus.OWNED));
        givenCatalog(addition("355880", "PC"));

        GameAdditionsCatalog catalog = service.additionsOf(BASE_ID, null);

        assertThat(catalog.additions()).singleElement()
                .satisfies(a -> {
                    assertThat(a.inCollection()).isFalse();
                    assertThat(a.alreadyLinked()).isFalse();
                });
        verify(gameRepository, never()).findByOwnerIdAndExternalIdIn(any(), anyList());
    }

    @Test
    void additionsOf_propagatesCatalogFailure_soTheApiCanAnswer502() {
        givenBase(base(GameStatus.OWNED));
        when(additionsCatalogClient.getAdditions(BASE_EXTERNAL_ID))
                .thenThrow(new ExternalCatalogException("RAWG devolvió error 500"));

        assertThatThrownBy(() -> service.additionsOf(BASE_ID, VIEWER))
                .isInstanceOf(ExternalCatalogException.class);
    }

    // ---------------------------------------------------------------------- alta (escritura)

    @Test
    void addAdditions_throws400_whenNoIdsAreGiven() {
        assertThatThrownBy(() -> service.addAdditions(BASE_ID, List.of(), GameStatus.PLAYING, VIEWER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.addAdditions(BASE_ID, null, GameStatus.PLAYING, VIEWER))
                .isInstanceOf(IllegalArgumentException.class);
        verify(gameRepository, never()).findById(any());
    }

    @Test
    void addAdditions_throws400_whenAnIdIsBlank() {
        assertThatThrownBy(() -> service.addAdditions(BASE_ID, List.of("355880", "  "), GameStatus.PLAYING, VIEWER))
                .isInstanceOf(IllegalArgumentException.class);
        verify(gameRepository, never()).findById(any());
    }

    @Test
    void addAdditions_throws400_whenMoreThanFortyIdsAreGiven() {
        List<String> tooMany = IntStream.rangeClosed(1, 41).mapToObj(i -> "id-" + i).toList();

        assertThatThrownBy(() -> service.addAdditions(BASE_ID, tooMany, GameStatus.PLAYING, VIEWER))
                .isInstanceOf(IllegalArgumentException.class);
        verify(gameRepository, never()).findById(any());
    }

    @Test
    void addAdditions_throws404_whenBaseDoesNotExist() {
        when(gameRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addAdditions("missing", List.of("355880"), GameStatus.PLAYING, VIEWER))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    void addAdditions_throws403_whenBaseBelongsToAnotherUser() {
        Game other = base(GameStatus.OWNED);
        other.setOwnerId("u2");
        givenBase(other);

        assertThatThrownBy(() -> service.addAdditions(BASE_ID, List.of("355880"), GameStatus.PLAYING, VIEWER))
                .isInstanceOf(ForbiddenException.class);
        verify(additionsCatalogClient, never()).getAdditions(any());
    }

    @Test
    void addAdditions_throws422_whenBaseIsWishlist() {
        givenBase(base(GameStatus.WISHLIST));

        assertThatThrownBy(() -> service.addAdditions(BASE_ID, List.of("355880"), GameStatus.PLAYING, VIEWER))
                .isInstanceOf(GameAdditionsException.class)
                .hasMessageContaining("WISHLIST");
        verify(additionsCatalogClient, never()).getAdditions(any());
    }

    @Test
    void addAdditions_throws422_whenBaseHasNoRawgExternalId() {
        givenBase(Game.builder().id(BASE_ID).ownerId(VIEWER).title("Sin RAWG")
                .externalSource("FreeToGame").status(GameStatus.OWNED).build());

        assertThatThrownBy(() -> service.addAdditions(BASE_ID, List.of("355880"), GameStatus.PLAYING, VIEWER))
                .isInstanceOf(GameAdditionsException.class);
        verify(additionsCatalogClient, never()).getAdditions(any());
    }

    @Test
    void addAdditions_throws422_whenTheIdIsNotAnAdditionOfTheBase() {
        givenBase(base(GameStatus.OWNED));
        givenCatalog(addition("355880", "PC"));

        assertThatThrownBy(() -> service.addAdditions(BASE_ID, List.of("777"), GameStatus.PLAYING, VIEWER))
                .isInstanceOf(GameAdditionsException.class)
                .hasMessageContaining("777");
        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void addAdditions_createsNewDlc_withRequestedStatusAndNoAcquisitionData() {
        givenBase(base(GameStatus.OWNED));
        givenCatalog(addition("355880", "PC"));
        when(gameRepository.findByOwnerIdAndExternalIdIn(VIEWER, List.of("355880"))).thenReturn(List.of());

        GameAdditionsAddResult result = service.addAdditions(BASE_ID, List.of("355880"), GameStatus.PLAYING, VIEWER);

        ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
        verify(gameRepository).save(captor.capture());
        Game created = captor.getValue();
        assertThat(result.created()).containsExactly(created);
        assertThat(result.linked()).isEmpty();
        assertThat(result.skipped()).isEmpty();
        assertThat(created.getParentId()).isEqualTo(BASE_ID);
        assertThat(created.getParentTitle()).isEqualTo("The Witcher 3: Wild Hunt");
        assertThat(created.getOwnerId()).isEqualTo(VIEWER);
        assertThat(created.getExternalId()).isEqualTo("355880");
        assertThat(created.getExternalSource()).isEqualTo("RAWG");
        assertThat(created.getStatus()).isEqualTo(GameStatus.PLAYING);
        assertThat(created.getDateAdded()).isEqualTo(LocalDate.now());
        assertThat(created.getAcquisitionDate()).isNull();
        assertThat(created.getAcquisitionPrice()).isNull();
        assertThat(created.getGenres()).containsExactly("RPG");
        assertThat(created.getPlatform()).isEqualTo("PC");
        assertThat(created.getThumbnailUrl()).isEqualTo("http://thumb/355880");
        assertThat(created.getUserOwned()).isNotNull();
    }

    @Test
    void addAdditions_acceptsWishlistForTheDlcItself() {
        givenBase(base(GameStatus.OWNED));
        givenCatalog(addition("355880", "PC"));
        when(gameRepository.findByOwnerIdAndExternalIdIn(VIEWER, List.of("355880"))).thenReturn(List.of());

        GameAdditionsAddResult result = service.addAdditions(BASE_ID, List.of("355880"), GameStatus.WISHLIST, VIEWER);

        assertThat(result.created()).singleElement()
                .extracting(Game::getStatus).isEqualTo(GameStatus.WISHLIST);
    }

    @Test
    void addAdditions_fallsBackToTheBasePlatform_whenRawgHasNoPlatform() {
        givenBase(base(GameStatus.OWNED));
        givenCatalog(addition("355880", null));
        when(gameRepository.findByOwnerIdAndExternalIdIn(VIEWER, List.of("355880"))).thenReturn(List.of());

        GameAdditionsAddResult result = service.addAdditions(BASE_ID, List.of("355880"), GameStatus.OWNED, VIEWER);

        assertThat(result.created()).singleElement()
                .extracting(Game::getPlatform).isEqualTo("PC");
    }

    @Test
    void addAdditions_linksAnExistingLooseGame_keepingItsOwnData() {
        givenBase(base(GameStatus.OWNED));
        givenCatalog(addition("355880", "PC"));
        Game loose = Game.builder()
                .id("g2").ownerId(VIEWER).externalId("355880").title("Blood and Wine")
                .status(GameStatus.PLAYING).userRating(5).comment("buenísimo")
                .dateAdded(LocalDate.of(2026, 1, 1)).build();
        when(gameRepository.findByOwnerIdAndExternalIdIn(VIEWER, List.of("355880")))
                .thenReturn(List.of(loose));

        GameAdditionsAddResult result = service.addAdditions(BASE_ID, List.of("355880"), GameStatus.OWNED, VIEWER);

        assertThat(result.created()).isEmpty();
        assertThat(result.linked()).containsExactly(loose);
        assertThat(loose.getParentId()).isEqualTo(BASE_ID);
        assertThat(loose.getParentTitle()).isEqualTo("The Witcher 3: Wild Hunt");
        assertThat(loose.getStatus()).isEqualTo(GameStatus.PLAYING);
        assertThat(loose.getUserRating()).isEqualTo(5);
        assertThat(loose.getComment()).isEqualTo("buenísimo");
        assertThat(loose.getDateAdded()).isEqualTo(LocalDate.of(2026, 1, 1));
        verify(gameRepository).save(loose);
    }

    @Test
    void addAdditions_skipsWhatIsAlreadyLinkedToThisBase() {
        givenBase(base(GameStatus.OWNED));
        givenCatalog(addition("355880", "PC"));
        when(gameRepository.findByOwnerIdAndExternalIdIn(VIEWER, List.of("355880")))
                .thenReturn(List.of(Game.builder().id("d1").ownerId(VIEWER).externalId("355880")
                        .parentId(BASE_ID).build()));

        GameAdditionsAddResult result = service.addAdditions(BASE_ID, List.of("355880"), GameStatus.OWNED, VIEWER);

        assertThat(result.created()).isEmpty();
        assertThat(result.linked()).isEmpty();
        assertThat(result.skipped()).containsExactly(
                new GameAdditionSkip("355880", GameAdditionSkip.REASON_ALREADY_LINKED));
        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    void addAdditions_classifiesCreatedLinkedAndSkipped_inOneRequest() {
        givenBase(base(GameStatus.OWNED));
        givenCatalog(addition("100", "PC"), addition("200", "PC"), addition("300", "PC"));
        when(gameRepository.findByOwnerIdAndExternalIdIn(VIEWER, List.of("100", "200", "300")))
                .thenReturn(List.of(
                        Game.builder().id("loose").ownerId(VIEWER).externalId("200").build(),
                        Game.builder().id("linked").ownerId(VIEWER).externalId("300").parentId(BASE_ID).build()));

        GameAdditionsAddResult result = service.addAdditions(BASE_ID, List.of("100", "200", "300"),
                GameStatus.OWNED, VIEWER);

        assertThat(result.created()).singleElement()
                .extracting(Game::getExternalId).isEqualTo("100");
        assertThat(result.linked()).singleElement()
                .extracting(Game::getExternalId).isEqualTo("200");
        assertThat(result.skipped()).containsExactly(
                new GameAdditionSkip("300", GameAdditionSkip.REASON_ALREADY_LINKED));
    }

    @Test
    void addAdditions_deduplicatesRepeatedIds() {
        givenBase(base(GameStatus.OWNED));
        givenCatalog(addition("355880", "PC"));
        when(gameRepository.findByOwnerIdAndExternalIdIn(VIEWER, List.of("355880"))).thenReturn(List.of());

        GameAdditionsAddResult result = service.addAdditions(BASE_ID,
                List.of("355880", "355880"), GameStatus.OWNED, VIEWER);

        assertThat(result.created()).hasSize(1);
        verify(gameRepository).save(any(Game.class));
    }

    @Test
    void addAdditions_propagatesCatalogFailure_soTheApiCanAnswer502() {
        givenBase(base(GameStatus.OWNED));
        when(additionsCatalogClient.getAdditions(BASE_EXTERNAL_ID))
                .thenThrow(new ExternalCatalogException("RAWG no responde"));

        assertThatThrownBy(() -> service.addAdditions(BASE_ID, List.of("355880"), GameStatus.OWNED, VIEWER))
                .isInstanceOf(ExternalCatalogException.class);
        verify(gameRepository, never()).save(any(Game.class));
    }
}
