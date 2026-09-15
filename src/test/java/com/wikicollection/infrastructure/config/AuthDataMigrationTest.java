package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.out.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthDataMigrationTest {

    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthDataMigration migration(boolean enabled) {
        return new AuthDataMigration(mongoTemplate, userRepository, passwordEncoder,
                enabled, "admin", "admin123", "admin@local.dev");
    }

    @Test
    void run_migratesCollectionsAndSeedsAdmin() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("hash");

        migration(true).run(null);

        verify(mongoTemplate).updateMulti(any(Query.class), any(Update.class), eq("books"));
        verify(mongoTemplate).updateMulti(any(Query.class), any(Update.class), eq("movie_shows"));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThatCode(() -> org.assertj.core.api.Assertions.assertThat(captor.getValue().getUsername())
                .isEqualTo("admin")).doesNotThrowAnyException();
    }

    @Test
    void run_skipsAdmin_whenExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        migration(true).run(null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void run_doesNothing_whenDisabled() {
        migration(false).run(null);

        verify(mongoTemplate, never()).updateMulti(any(), any(), anyString());
    }

    @Test
    void run_swallowsMongoFailures() {
        when(mongoTemplate.updateMulti(any(), any(), anyString()))
                .thenThrow(new RuntimeException("sin mongo"));

        assertThatCode(() -> migration(true).run(null)).doesNotThrowAnyException();
    }
}
