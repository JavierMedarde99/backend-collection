package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.out.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUser_returnsDetailsWithUserIdAsUsername() {
        User user = User.builder().id("u1").username("javi").password("hash").build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("u1");

        assertThat(details.getUsername()).isEqualTo("u1");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities()).isNotEmpty();
    }

    @Test
    void loadUser_throwsNotFound_whenMissing() {
        when(userRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nope"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
