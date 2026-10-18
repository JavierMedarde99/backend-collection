package com.wikicollection.infrastructure.config;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;

import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.out.UserRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthDataMigration implements ApplicationRunner {

    private static final List<String> COLLECTIONS =
            List.of("books", "games", "board_games", "magic_cards", "decks", "movie_shows",
                    "amiibos", "funkos");

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminEmail;
    private final Supplier<String> passwordGenerator;

    @Autowired
    public AuthDataMigration(MongoTemplate mongoTemplate,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             @Value("${app.migration.enabled:true}") boolean enabled,
                             @Value("${app.admin.username:admin}") String adminUsername,
                             @Value("${app.admin.password:}") String adminPassword,
                             @Value("${app.admin.email:admin@local.dev}") String adminEmail) {
        this(mongoTemplate, userRepository, passwordEncoder, enabled, adminUsername,
                adminPassword, adminEmail, AuthDataMigration::generatePassword);
    }

    AuthDataMigration(MongoTemplate mongoTemplate,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      boolean enabled,
                      String adminUsername,
                      String adminPassword,
                      String adminEmail,
                      Supplier<String> passwordGenerator) {
        this.mongoTemplate = mongoTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminEmail = adminEmail;
        this.passwordGenerator = passwordGenerator;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        try {
            Query missing = new Query(Criteria.where("ownerId").exists(false));
            Update system = new Update().set("ownerId", "system");
            for (String collection : COLLECTIONS) {
                mongoTemplate.updateMulti(missing, system, collection);
            }
            if (!userRepository.existsByUsername(adminUsername)) {
                String password = adminPassword == null || adminPassword.isBlank()
                        ? passwordGenerator.get()
                        : adminPassword;
                userRepository.save(User.builder()
                        .username(adminUsername)
                        .email(adminEmail)
                        .password(passwordEncoder.encode(password))
                        .displayName("Administrador")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
                logAdminPassword(adminPassword, password);
            }
        } catch (RuntimeException e) {
            log.warn("Migración de auth omitida (Mongo no disponible): {}", e.getMessage());
        }
    }

    private void logAdminPassword(String configuredPassword, String usedPassword) {
        if (configuredPassword == null || configuredPassword.isBlank()) {
            log.warn("""
                    ============================================================
                    Se ha creado la cuenta de administrador '{}' con una
                    contraseña ALEATORIA generada en este arranque:
                        {}
                    Anótala AHORA: no se vuelve a imprimir. Para fijar una
                    propia, exporta ADMIN_PASSWORD antes de arrancar.
                    ============================================================""", adminUsername, usedPassword);
        } else {
            log.info("Cuenta de administrador '{}' creada con la contraseña configurada en ADMIN_PASSWORD.",
                    adminUsername);
        }
    }

    /** Genera un secreto de 32 bytes aleatorios (256 bits) en Base64. */
    static String generatePassword() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
