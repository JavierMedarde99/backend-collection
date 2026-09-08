package com.wikicollection.application.service;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

@Component
public class DateRangeValidator {

    public void validate(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        if (start != null && start.isAfter(today)) {
            throw new IllegalArgumentException("Fechas mal formadas: la fecha de inicio no puede ser futura");
        }
        if (end != null && end.isAfter(today)) {
            throw new IllegalArgumentException("Fechas mal formadas: la fecha de fin no puede ser futura");
        }
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Fechas mal formadas: la fecha de inicio no puede ser posterior a la fecha de fin");
        }
    }
}