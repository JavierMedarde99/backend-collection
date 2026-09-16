package com.wikicollection.infrastructure.config;

import com.wikicollection.domain.model.UserPreferences;
import com.wikicollection.domain.port.out.UserPreferencesRepository;
import com.wikicollection.domain.port.out.UserRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Asigna preferencias por defecto a usuarios existentes que no las tengan.
 * Idempotente: nunca sobrescribe preferencias existentes.
 */
@Component
@Slf4j
public class UserPreferencesMigration implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final boolean enabled;

    public UserPreferencesMigration(UserRepository userRepository,
                                    UserPreferencesRepository preferencesRepository,
                                    @Value("${app.migration.enabled:true}") boolean enabled) {
        this.userRepository = userRepository;
        this.preferencesRepository = preferencesRepository;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        try {
            userRepository.findAll().forEach(user -> {
                if (!preferencesRepository.existsByUserId(user.getId())) {
                    preferencesRepository.save(UserPreferences.defaults(user.getId()));
                }
            });
        } catch (RuntimeException e) {
            log.warn("Migración de preferencias omitida (Mongo no disponible): {}", e.getMessage());
        }
    }
}
