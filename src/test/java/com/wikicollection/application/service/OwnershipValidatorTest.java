package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.wikicollection.application.exception.ForbiddenException;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.out.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnershipValidatorTest {

    @Mock
    private UserRepository userRepository;

    private OwnershipValidator ownershipValidator;

    @BeforeEach
    void setUp() {
        ownershipValidator = new OwnershipValidator(userRepository, "admin");
    }

    @Test
    void allowsOwner() {
        assertThatCode(() -> ownershipValidator.validateOwner("u1", "u1"))
                .doesNotThrowAnyException();
    }

    @Test
    void deniesStranger() {
        when(userRepository.findById("u9")).thenReturn(Optional.of(
                User.builder().id("u9").username("intruso").build()));

        assertThatThrownBy(() -> ownershipValidator.validateOwner("u1", "u9"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deniesNullOwner_forNonAdmin() {
        when(userRepository.findById("u9")).thenReturn(Optional.of(
                User.builder().id("u9").username("intruso").build()));

        assertThatThrownBy(() -> ownershipValidator.validateOwner(null, "u9"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void allowsAdmin() {
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(
                User.builder().id("admin-id").username("admin").build()));

        assertThatCode(() -> ownershipValidator.validateOwner("u1", "admin-id"))
                .doesNotThrowAnyException();
    }

    @Test
    void deniesMissingUser() {
        when(userRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ownershipValidator.validateOwner("u1", "ghost"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deniesNullUser() {
        assertThatThrownBy(() -> ownershipValidator.validateOwner("u1", null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void isAdmin_checksUsername() {
        when(userRepository.findById("admin-id")).thenReturn(Optional.of(
                User.builder().id("admin-id").username("admin").build()));
        when(userRepository.findById("u1")).thenReturn(Optional.of(
                User.builder().id("u1").username("javi").build()));

        assertThat(ownershipValidator.isAdmin("admin-id")).isTrue();
        assertThat(ownershipValidator.isAdmin("u1")).isFalse();
        assertThat(ownershipValidator.isAdmin(null)).isFalse();
    }
}
