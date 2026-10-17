package com.wikicollection.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.out.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
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

    @Test
    void run_seedsAdminWithGeneratedPassword_whenAdminPasswordNotConfigured() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("clave-generada-para-el-test")).thenReturn("hash");

        new AuthDataMigration(mongoTemplate, userRepository, passwordEncoder, true,
                "admin", "", "admin@local.dev", () -> "clave-generada-para-el-test").run(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hash");
        verify(passwordEncoder).encode("clave-generada-para-el-test");
    }

    @Test
    void run_printsGeneratedPasswordExactlyOnce_inLog() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");

        Logger logger = (Logger) LoggerFactory.getLogger(AuthDataMigration.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            new AuthDataMigration(mongoTemplate, userRepository, passwordEncoder, true,
                    "admin", null, "admin@local.dev", () -> "secreto-impreso-una-vez").run(null);
        } finally {
            logger.detachAppender(appender);
        }

        List<String> messages = new ArrayList<>();
        for (ILoggingEvent event : appender.list) {
            messages.add(event.getFormattedMessage());
        }
        assertThat(messages.stream().filter(m -> m.contains("secreto-impreso-una-vez")).count())
                .as("La contraseña generada debe imprimirse exactamente una vez")
                .isEqualTo(1);
    }

    @Test
    void generatedPassword_isStrongEnough() {
        String generated = AuthDataMigration.generatePassword();

        assertThat(generated).hasSizeGreaterThanOrEqualTo(24);
    }

    @Test
    void applicationProperties_declaresNoDefaultAdminPassword() throws Exception {
        String properties = new String(
                AuthDataMigration.class.getResourceAsStream("/application.properties").readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(properties)
                .as("La contraseña admin no puede tener valor por defecto hardcodeado (#479)")
                .contains("app.admin.password=${ADMIN_PASSWORD:}");
    }
}
