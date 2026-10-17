package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guardián de la política transaccional del proyecto (issue #485).
 *
 * <p>Mongo no está configurado como replica set y el proyecto no declara ningún
 * {@code PlatformTransactionManager}, así que {@code @Transactional} no hace nada: Spring no
 * crea transacción y cada operación se ejecuta por separado. Mantener la anotación da una
 * falsa sensación de atomicidad.
 *
 * <p>La atomicidad real la da Mongo a nivel de documento (cada escritura es atómica por sí
 * misma). Cuando una operación necesite abarcar varios documentos, habrá que declarar
 * {@code MongoTransactionManager} y exigir replica set antes de volver a usar la anotación.
 */
class TransactionalPolicyTest {

    @Test
    void noService_usesTransactional_WhileThereIsNoTransactionManager() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        Set<BeanDefinition> services = scanner.findCandidateComponents("com.wikicollection.application.service");

        assertThat(services)
                .as("Debe haber servicios que escanear")
                .isNotEmpty();

        assertThat(services)
                .as("@Transactional no tiene efecto sin PlatformTransactionManager en este proyecto (#485): "
                        + "Mongo atómico por documento no necesita transacción. Si alguna operación llega a "
                        + "necesitar varias escrituras atómicas, declara MongoTransactionManager y documenta "
                        + "el requisito de replica set antes de volver a usarla.")
                .allSatisfy(definition -> {
                    Class<?> type = load(definition);
                    assertThat(type.getMethods())
                            .as("%s: métodos con @Transactional", type.getSimpleName())
                            .noneMatch(method -> method.isAnnotationPresent(Transactional.class));
                });
    }

    private static Class<?> load(BeanDefinition definition) {
        try {
            return Class.forName(definition.getBeanClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("No se pudo cargar " + definition.getBeanClassName(), e);
        }
    }
}
