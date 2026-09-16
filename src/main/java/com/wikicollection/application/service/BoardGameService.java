package com.wikicollection.application.service;

import com.wikicollection.application.exception.BoardGameNotFoundException;
import com.wikicollection.domain.model.BoardGame;
import com.wikicollection.domain.model.BoardGameSearchCriteria;
import com.wikicollection.domain.port.in.BoardGameUseCase;
import com.wikicollection.domain.port.out.BoardGameRepository;

import com.wikicollection.domain.model.CollectionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BoardGameService implements BoardGameUseCase {

    private final BoardGameRepository boardGameRepository;
    private final OwnershipValidator ownershipValidator;

    private final OwnerScopeResolver ownerScopeResolver;

    public BoardGameService(BoardGameRepository boardGameRepository,
                              OwnershipValidator ownershipValidator,
                       OwnerScopeResolver ownerScopeResolver) {
        this.boardGameRepository = boardGameRepository;
        this.ownershipValidator = ownershipValidator;
        this.ownerScopeResolver = ownerScopeResolver;
    }

    @Override
    public Page<BoardGame> search(BoardGameSearchCriteria criteria, Pageable pageable) {
        return boardGameRepository.search(criteria, pageable);
    }

    @Override
    public Page<BoardGame> search(BoardGameSearchCriteria criteria, Pageable pageable, String owner, String viewerId) {
        OwnerScopeResolver.Scope scope = ownerScopeResolver.resolve(CollectionType.BOARDGAMES, owner, viewerId);
        return boardGameRepository.search(new BoardGameSearchCriteria(criteria.name(), criteria.status(), scope.ownerId(), scope.excludeOwnerIds()), pageable);
    }

    @Override
    public BoardGame findById(String id) {
        return boardGameRepository.findById(id)
                .orElseThrow(() -> new BoardGameNotFoundException("Juego de mesa no encontrado con id: " + id));
    }

    @Override
    public BoardGame save(BoardGame boardGame, String ownerId) {
        boardGame.setOwnerId(ownerId);
        return boardGameRepository.save(boardGame);
    }

    @Override
    public BoardGame update(String id, BoardGame updates, String userId) {
        BoardGame existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        copyUpdatableFields(existing, updates);
        return boardGameRepository.save(existing);
    }

    @Override
    public void delete(String id, String userId) {
        BoardGame existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
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