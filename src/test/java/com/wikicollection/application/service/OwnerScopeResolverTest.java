package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import com.wikicollection.application.exception.UnauthenticatedException;
import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.port.in.UserPreferencesUseCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnerScopeResolverTest {

    @Mock
    private UserPreferencesUseCase preferencesUseCase;

    @InjectMocks
    private OwnerScopeResolver resolver;

    @Test
    void mine_defaultsToAuthenticatedUser() {
        OwnerScopeResolver.Scope scope = resolver.resolve(CollectionType.BOOKS, null, "u1");

        assertThat(scope.ownerId()).isEqualTo("u1");
        assertThat(scope.excludeOwnerIds()).isEmpty();
    }

    @Test
    void mine_rejectsAnonymous() {
        assertThatThrownBy(() -> resolver.resolve(CollectionType.BOOKS, "mine", null))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void other_excludesPrivateAndSelf() {
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.BOOKS))
                .thenReturn(List.of("u9"));

        OwnerScopeResolver.Scope scope = resolver.resolve(CollectionType.BOOKS, "other", "u1");

        assertThat(scope.ownerId()).isNull();
        assertThat(scope.excludeOwnerIds()).containsExactlyInAnyOrder("u9", "u1");
    }

    @Test
    void other_worksForAnonymous() {
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.BOOKS))
                .thenReturn(List.of("u9"));

        OwnerScopeResolver.Scope scope = resolver.resolve(CollectionType.BOOKS, "other", null);

        assertThat(scope.ownerId()).isNull();
        assertThat(scope.excludeOwnerIds()).containsExactly("u9");
    }

    @Test
    void all_includesSelfDespitePrivate() {
        when(preferencesUseCase.getUserIdsWithPrivateCollection(CollectionType.BOOKS))
                .thenReturn(List.of("u1", "u9"));

        OwnerScopeResolver.Scope scope = resolver.resolve(CollectionType.BOOKS, "all", "u1");

        assertThat(scope.ownerId()).isNull();
        assertThat(scope.excludeOwnerIds()).containsExactly("u9");
    }

    @Test
    void all_rejectsAnonymous() {
        assertThatThrownBy(() -> resolver.resolve(CollectionType.BOOKS, "all", null))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void unknownMode_returns400() {
        assertThatThrownBy(() -> resolver.resolve(CollectionType.BOOKS, "everyone", "u1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
