package com.wikicollection.application.service;

import com.wikicollection.application.exception.BookConflictException;
import com.wikicollection.application.exception.InvalidProgressException;
import com.wikicollection.application.exception.BookNotFoundException;
import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookSearchCriteria;
import com.wikicollection.domain.port.in.BookUseCase;
import java.util.List;

import com.wikicollection.domain.port.out.BookRepository;
import com.wikicollection.domain.port.out.ExternalBookCatalogClient;

import org.springframework.dao.DuplicateKeyException;
import com.wikicollection.domain.model.CollectionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BookService implements BookUseCase {

    private final BookRepository bookRepository;
    private final ExternalBookCatalogClient catalogClient;
    private final DateRangeValidator dateRangeValidator;
    private final OwnershipValidator ownershipValidator;

    private final OwnerScopeResolver ownerScopeResolver;

    private final OwnerResolver ownerResolver;

    public BookService(BookRepository bookRepository, ExternalBookCatalogClient catalogClient, DateRangeValidator dateRangeValidator,
                       OwnershipValidator ownershipValidator,
                       OwnerScopeResolver ownerScopeResolver,
                       OwnerResolver ownerResolver) {
        this.bookRepository = bookRepository;
        this.catalogClient = catalogClient;
        this.dateRangeValidator = dateRangeValidator;
        this.ownershipValidator = ownershipValidator;
        this.ownerResolver = ownerResolver;
        this.ownerScopeResolver = ownerScopeResolver;
    }

    @Override
    public Page<Book> search(BookSearchCriteria criteria, Pageable pageable) {
        return bookRepository.search(criteria, pageable);
    }

    @Override
    public java.util.List<String> distinctGenres() {
        java.util.List<String> genres = bookRepository
                .distinctGenres(ownerScopeResolver.excludedOwnerIds(CollectionType.BOOKS));
        if (genres == null) {
            return java.util.List.of();
        }
        return genres.stream()
                .filter(g -> g != null && !g.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Override
    public Page<Book> search(BookSearchCriteria criteria, Pageable pageable, String owner, String viewerId) {
        OwnerScopeResolver.Scope scope = ownerScopeResolver.resolve(CollectionType.BOOKS, owner, viewerId);
        return bookRepository.search(new BookSearchCriteria(criteria.name(), criteria.author(), criteria.type(), criteria.state(), criteria.genre(), scope.ownerId(), scope.excludeOwnerIds()), pageable);
    }

    @Override
    public Book findById(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Libro no encontrado con id: " + id));
    }

    @Override
    @Transactional
    public Book save(Book book, String ownerId) {
        book.setOwnerId(ownerId);
        book.setUserOwned(ownerResolver.resolveOwner(ownerId));
        checkExternalIdUnique(book.getExternalId(), null);
        validateProgress(book.getPagesRead(), book.getPages());
        fillEmptyGenres(book);
        dateRangeValidator.validate(book.getStartDate(), book.getEndDate());
        return saveOrConflict(book);
    }

    @Override
    @Transactional
    public Book update(String id, Book updates, String userId) {
        Book existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        checkExternalIdUnique(updates.getExternalId(), id);
        copyUpdatableFields(existing, updates);
        validateProgress(existing.getPagesRead(), existing.getPages());
        fillEmptyGenres(existing);
        dateRangeValidator.validate(existing.getStartDate(), existing.getEndDate());
        return saveOrConflict(existing);
    }

    private Book saveOrConflict(Book book) {
        try {
            return bookRepository.save(book);
        } catch (DuplicateKeyException e) {
            throw new BookConflictException("Ya existe un libro con externalId: " + book.getExternalId());
        }
    }

    private void fillEmptyGenres(Book book) {
        if (book.getGenres() != null && !book.getGenres().isEmpty()) {
            return;
        }
        if (book.getExternalId() == null || book.getExternalId().isBlank()) {
            return;
        }
        try {
            List<String> categories = catalogClient.getCategories(book.getExternalId().strip());
            if (categories != null && !categories.isEmpty()) {
                book.setGenres(categories);
            }
        } catch (RuntimeException e) {
            log.warn("Categorías no disponibles para {}: {}", book.getExternalId(), e.getMessage());
        }
    }

    private void validateProgress(Integer pagesRead, Integer pages) {
        if (pagesRead != null && pages != null && pages > 0 && pagesRead > pages) {
            throw new InvalidProgressException(
                    "Las páginas leídas (" + pagesRead + ") no pueden exceder el total de páginas (" + pages + ")");
        }
    }

    private void checkExternalIdUnique(String externalId, String currentId) {
        if (externalId == null || externalId.isBlank()) {
            return;
        }
        bookRepository.findByExternalId(externalId)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new BookConflictException("Ya existe un libro con externalId: " + externalId);
                });
    }

    @Override
    public void delete(String id, String userId) {
        Book existing = findById(id);
        ownershipValidator.validateOwner(existing.getOwnerId(), userId);
        bookRepository.deleteById(id);
    }

    private void copyUpdatableFields(Book target, Book source) {
        target.setExternalId(source.getExternalId());
        target.setTitle(source.getTitle());
        target.setDescripcion(source.getDescripcion());
        target.setAuthor(source.getAuthor());
        target.setGenres(source.getGenres());
        target.setPages(source.getPages());
        target.setType(source.getType());
        target.setState(source.getState());
        target.setComment(source.getComment());
        target.setStart(source.getStart());
        target.setPagesRead(source.getPagesRead());
        target.setStartDate(source.getStartDate());
        target.setEndDate(source.getEndDate());
        target.setFrontpage(source.getFrontpage());
    }
}
