package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DateRangeValidatorTest {

    private final DateRangeValidator validator = new DateRangeValidator();

    private final LocalDate today = LocalDate.now();
    private final LocalDate yesterday = today.minusDays(1);

    @Test
    void validate_acceptsNullDates() {
        assertThatCode(() -> validator.validate(null, null)).doesNotThrowAnyException();
    }

    @Test
    void validate_acceptsOneDateProvided() {
        assertThatCode(() -> validator.validate(null, yesterday)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(yesterday, null)).doesNotThrowAnyException();
    }

    @Test
    void validate_acceptsPastOrTodayDatesInOrder() {
        assertThatCode(() -> validator.validate(yesterday, today)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(yesterday, yesterday)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(today, today)).doesNotThrowAnyException();
    }

    @Test
    void validate_throws_whenStartIsFuture() {
        assertThatThrownBy(() -> validator.validate(today.plusDays(1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_throws_whenEndIsFuture() {
        assertThatThrownBy(() -> validator.validate(null, today.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_throws_whenStartIsAfterEnd() {
        assertThatThrownBy(() -> validator.validate(today, yesterday))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validate_throws_whenBothDatesUnorderedAndFuture() {
        assertThatThrownBy(() -> validator.validate(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fechas mal formadas");
    }
}