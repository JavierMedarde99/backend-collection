package com.wikicollection.infrastructure.config;

import java.time.LocalDateTime;
import java.util.List;

import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.out.UserRepository;

import lombok.extern.slf4j.Slf4j;

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
            List.of("books", "games", "board_games", "magic_cards", "decks", "movie_shows");

    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String adminUsername;
    private final String adminPassword;
    private final String adminEmail;

    public AuthDataMigration(MongoTemplate mongoTemplate,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             @Value("${app.migration.enabled:true}") boolean enabled,
                             @Value("${app.admin.username:admin}") String adminUsername,
                             @Value("${app.admin.password:admin123}") String adminPassword,
                             @Value("${app.admin.email:admin@local.dev}") String adminEmail) {
        this.mongoTemplate = mongoTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminEmail = adminEmail;
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
                userRepository.save(User.builder()
                        .username(adminUsername)
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .displayName("Administrador")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());
            }
        } catch (RuntimeException e) {
            log.warn("Migración de auth omitida (Mongo no disponible): {}", e.getMessage());
        }
    }
}
