package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.out.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new UserDetailsServiceImpl(userRepository, "admin");
    }

    @Test
    void loadUser_returnsDetailsWithUsername() {
        User user = User.builder().id("u1").username("javi").password("hash").build();
        when(userRepository.findByUsername("javi")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("javi");

        assertThat(details.getUsername()).isEqualTo("javi");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUser_byId_returnsDetailsWithUsername() {
        User user = User.builder().id("u1").username("javi").password("hash").build();
        when(userRepository.findByUsername("u1")).thenReturn(Optional.empty());
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername("u1");

        assertThat(principal.getUsername()).isEqualTo("javi");
        assertThat(principal.getId()).isEqualTo("u1");
    }

    @Test
    void loadUser_adminUser_getsAdminRole() {
        User admin = User.builder().id("a1").username("admin").password("hash").build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        UserDetails details = userDetailsService.loadUserByUsername("admin");

        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUser_throwsNotFound_whenMissing() {
        when(userRepository.findByUsername("nope")).thenReturn(Optional.empty());
        when(userRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nope"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
