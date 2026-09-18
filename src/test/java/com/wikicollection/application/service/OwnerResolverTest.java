package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.wikicollection.domain.model.User;
import com.wikicollection.domain.model.UserOwned;
import com.wikicollection.domain.port.out.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnerResolverTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OwnerResolver resolver;

    @Test
    void resolveOwner_usesDisplayName() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(
                User.builder().id("u1").username("javi").displayName("Javi").build()));

        UserOwned owned = resolver.resolveOwner("u1");

        assertThat(owned.getOwnerId()).isEqualTo("u1");
        assertThat(owned.getOwnerName()).isEqualTo("Javi");
    }

    @Test
    void resolveOwner_fallsBackToUsername_whenNoDisplayName() {
        when(userRepository.findById("u1")).thenReturn(Optional.of(
                User.builder().id("u1").username("javi").build()));

        assertThat(resolver.resolveOwner("u1").getOwnerName()).isEqualTo("javi");
    }

    @Test
    void resolveOwner_fallsBackToId_whenUserMissing() {
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());

        UserOwned owned = resolver.resolveOwner("ghost");

        assertThat(owned.getOwnerId()).isEqualTo("ghost");
        assertThat(owned.getOwnerName()).isEqualTo("ghost");
    }
}
