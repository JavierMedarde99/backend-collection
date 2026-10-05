package com.wikicollection.application.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

/**
 * Vacía las cachés de mazos después de una importación (#353).
 *
 * <p>Es un bean aparte y no un {@code @CacheEvict} sobre el método del worker porque ese
 * método está anotado con {@code @Async}: los dos advisors se ejecutan en el orden de sus
 * AspectJWeaver, y si el de caché corriera primero lo haría en el hilo que encola la
 * importación, antes de que el mazo se guarde. El resultado sería una lectura concurrente
 * que vuelve a cachear el mazo viejo, y el usuario sigue viendo la lista anterior a la
 * importación aunque la importación haya terminado.
 *
 * <p>Desde aquí sí funciona: el worker es otro bean, así que la llamada pasa por el proxy.
 */
@Component
public class DeckCacheInvalidator {

    /**
     * Se llama después de guardar el mazo y desde el worker, nunca desde el propio worker
     * por auto-invocación (eso ignoraría el proxy y no ejecutaría nada).
     */
    @CacheEvict(cacheNames = {"deckDetail", "deckList"}, allEntries = true)
    public void afterImport() {
        // El cuerpo está vacío a propósito: lo que importa es que el advisor de caché lo vea.
    }
}