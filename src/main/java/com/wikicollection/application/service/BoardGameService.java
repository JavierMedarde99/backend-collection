package com.wikicollection.application.service;

import com.wikicollection.application.exception.BoardGameNotFoundException;
import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameSearchCriteria;
import com.wikicollection.domain.port.in.BoardGameUseCase;
import com.wikicollection.domain.port.out.BoardGameRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BoardGameService implements BoardGameUseCase {

    private final BoardGameRepository boardGameRepository;

    public BoardGameService(BoardGameRepository boardGameRepository) {
        this.boardGameRepository = boardGameRepository;
    }

    @Override
    public Page<BoardGame> search(BoardGameSearchCriteria criteria, Pageable pageable) {
        return boardGameRepository.search(criteria, pageable);
    }

    @Override
    public BoardGame findById(String id) {
        return boardGameRepository.findById(id)
                .orElseThrow(() -> new BoardGameNotFoundException("Juego de mesa no encontrado con id: " + id));
    }

    @Override
    public BoardGame save(BoardGame boardGame) {
        return boardGameRepository.save(boardGame);
    }

    @Override
    public BoardGame update(String id, BoardGame updates) {
        BoardGame existing = findById(id);
        copyUpdatableFields(existing, updates);
        return boardGameRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        findById(id);
        boardGameRepository.deleteById(id);
    }

    private void copyUpdatableFields(BoardGame target, BoardGame source) {
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setYearPublished(source.getYearPublished());
        target.setMinPlayers(source.getMinPlayers());
        target.setMaxPlayers(source.getMaxPlayers());
        target.setMinPlaytime(source.getMinPlaytime());
        target.setMaxPlaytime(source.getMaxPlaytime());
        target.setPublisher(source.getPublisher());
        target.setDesigners(source.getDesigners());
        target.setCategories(source.getCategories());
        target.setMechanics(source.getMechanics());
        target.setImageUrl(source.getImageUrl());
        target.setThumbnailUrl(source.getThumbnailUrl());
        target.setBggRating(source.getBggRating());
        target.setBggId(source.getBggId());
        target.setStatus(source.getStatus());
        target.setNotes(source.getNotes());
        target.setDateAdded(source.getDateAdded());
    }
}