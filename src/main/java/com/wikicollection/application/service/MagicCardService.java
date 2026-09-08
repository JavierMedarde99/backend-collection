package com.wikicollection.application.service;

import com.wikicollection.application.exception.MagicCardNotFoundException;
import com.wikicollection.domain.model.MagicCard;
import com.wikicollection.domain.model.MagicCardSearchCriteria;
import com.wikicollection.domain.port.in.MagicCardUseCase;
import com.wikicollection.domain.port.out.MagicCardRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MagicCardService implements MagicCardUseCase {

    private final MagicCardRepository magicCardRepository;

    public MagicCardService(MagicCardRepository magicCardRepository) {
        this.magicCardRepository = magicCardRepository;
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
    public MagicCard save(MagicCard magicCard) {
        return magicCardRepository.save(magicCard);
    }

    @Override
    public MagicCard update(String id, MagicCard updates) {
        MagicCard existing = findById(id);
        copyUpdatableFields(existing, updates);
        return magicCardRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        findById(id);
        magicCardRepository.deleteById(id);
    }

    private void copyUpdatableFields(MagicCard target, MagicCard source) {
        target.setName(source.getName());
        target.setLanguage(source.getLanguage());
        target.setReleaseDate(source.getReleaseDate());
        target.setManaCost(source.getManaCost());
        target.setConvertedManaCost(source.getConvertedManaCost());
        target.setType(source.getType());
        target.setText(source.getText());
        target.setPower(source.getPower());
        target.setToughness(source.getToughness());
        target.setLoyalty(source.getLoyalty());
        target.setRarity(source.getRarity());
        target.setSetCode(source.getSetCode());
        target.setSetName(source.getSetName());
        target.setArtist(source.getArtist());
        target.setImageUrl(source.getImageUrl());
        target.setImageLargeUrl(source.getImageLargeUrl());
        target.setArtCropUrl(source.getArtCropUrl());
        target.setCondition(source.getCondition());
        target.setIsFoil(source.getIsFoil());
        target.setQuantity(source.getQuantity());
        target.setNotes(source.getNotes());
    }
}
