package com.wikicollection.domain.model;

/**
 * Categoría del catálogo de HobbyQuantHQ con el número de pops que contiene.
 * Se usa para poblar el filtro de categorías del frontend.
 */
public record FunkoCategory(String name, Integer count) {
}
