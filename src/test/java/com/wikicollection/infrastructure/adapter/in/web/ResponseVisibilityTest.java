package com.wikicollection.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.application.service.UserPrincipal;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.port.in.UserPreferencesUseCase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ResponseVisibilityTest {

    @Mock
    private UserPreferencesUseCase preferencesUseCase;

    private ResponseVisibility visibility;

    @BeforeEach
    void setUp() {
        visibility = new ResponseVisibility(preferencesUseCase);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String id, String role) {
        var principal = new UserPrincipal(id, "user", "hash", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private void authenticateAnonymous() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
    }

    @Test
    void owner_seesPrivate() {
        authenticateAs("u1", "USER");

        assertThat(visibility.canSeePrivate("u1", CollectionType.BOOKS)).isTrue();
    }

    @Test
    void admin_seesPrivate() {
        authenticateAs("admin1", "ADMIN");

        assertThat(visibility.canSeePrivate("u1", CollectionType.BOOKS)).isTrue();
    }

    @Test
    void anonymous_seesPrivate_whenCollectionPublic() {
        authenticateAnonymous();
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.BOOKS))
                .thenReturn(List.of("otro"));

        assertThat(visibility.canSeePrivate("u1", CollectionType.BOOKS)).isTrue();
    }

    @Test
    void anonymous_doesNotSeePrivate_whenCollectionPrivate() {
        authenticateAnonymous();
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.BOOKS))
                .thenReturn(List.of("u1"));

        assertThat(visibility.canSeePrivate("u1", CollectionType.BOOKS)).isFalse();
    }

    @Test
    void nonOwner_seesPrivate_whenCollectionPublic() {
        authenticateAs("viewer", "USER");
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.GAMES))
                .thenReturn(List.of());

        assertThat(visibility.canSeePrivate("u1", CollectionType.GAMES)).isTrue();
    }

    @Test
    void nonOwner_doesNotSeePrivate_whenCollectionPrivate() {
        authenticateAs("viewer", "USER");
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.GAMES))
                .thenReturn(List.of("u1"));

        assertThat(visibility.canSeePrivate("u1", CollectionType.GAMES)).isFalse();
    }
}
