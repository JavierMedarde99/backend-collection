package com.wikicollection.application.service;

import com.wikicollection.application.exception.MagicCardNotFoundException;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.port.in.MagicCardUseCase;
import com.wikicollection.domain.port.out.ExternalMagicCardCatalogClient;
import com.wikicollection.domain.port.out.MagicCardRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
public class MagicCardService implements MagicCardUseCase {

    private final MagicCardRepository magicCardRepository;
    private final ExternalMagicCardCatalogClient catalogClient;

    public MagicCardService(MagicCardRepository magicCardRepository,
                            ExternalMagicCardCatalogClient catalogClient) {
        this.magicCardRepository = magicCardRepository;
        this.catalogClient = catalogClient;
    }

    @Override
    public Page<MagicCard> search(MagicCardSearchCriteria criteria, Pageable pageable) {
        return magicCardRepository.search(criteria, pageable);
    }

    @Override
    public MagicCard findById(String id) {
        return magicCardRepository.findById(id)
                .orElseThrow(() -> new MagicCardNotFoundException("Carta no encontrada con id: " + id));
    }

    @Override
    public MagicCard addFromScryfall(String scryfallId) {
        MagicCard fetched;
        try {
            fetched = catalogClient.findById(scryfallId);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new MagicCardNotFoundException("Carta no encontrada en Scryfall con id: " + scryfallId);
            }
            throw e;
        }
        return magicCardRepository.save(fetched);
    }

    @Override
    public void delete(String id) {
        findById(id);
        magicCardRepository.deleteById(id);
    }
}
