package com.wikicollection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserPreferencesTest {

    @Test
    void defaults_activatesAllCollectionsAsPublic() {
        UserPreferences prefs = UserPreferences.defaults("u1");

        assertThat(prefs.getUserId()).isEqualTo("u1");
        assertThat(prefs.getActiveCollections()).containsExactlyInAnyOrderEntriesOf(
                java.util.Map.of("books", true, "games", true, "boardgames", true,
                        "magic", true, "decks", true, "movieshows", true));
        assertThat(prefs.getCollectionVisibility().values())
                .allMatch(CollectionVisibility.PUBLIC::equals);
        assertThat(prefs.getCreatedAt()).isNotNull();
        assertThat(prefs.getUpdatedAt()).isNotNull();
    }

    @Test
    void collectionType_resolvesByKey() {
        assertThat(CollectionType.fromKey("books")).isEqualTo(CollectionType.BOOKS);
        assertThat(CollectionType.fromKey("movieshows")).isEqualTo(CollectionType.MOVIESHOWS);
    }

    @Test
    void collectionType_rejectsUnknownKey() {
        assertThatThrownBy(() -> CollectionType.fromKey("comics"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
