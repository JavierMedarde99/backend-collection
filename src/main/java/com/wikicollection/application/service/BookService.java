package com.wikicollection.application.service;

import java.time.LocalDateTime;

import com.wikicollection.application.exception.BookNotFoundException;
import com.wikicollection.application.exception.DuplicateIsbnException;
import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookStatus;
import com.wikicollection.domain.port.in.BookUseCase;
import com.wikicollection.domain.port.out.BookRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BookService implements BookUseCase {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public Page<Book> findAll(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    @Override
    public Page<Book> findByStatus(BookStatus status, Pageable pageable) {
        return bookRepository.findByStatus(status, pageable);
    }

    @Override
    public Page<Book> findByTag(String tag, Pageable pageable) {
        return bookRepository.findByTagsContaining(tag, pageable);
    }

    @Override
    public Page<Book> searchByTitleOrAuthor(String query, Pageable pageable) {
        return bookRepository.searchByTitleOrAuthor(query, pageable);
    }

    @Override
    public Book findById(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Libro no encontrado con id: " + id));
    }

    @Override
    public Book save(Book book) {
        if (book.getDateAdded() == null) {
            book.setDateAdded(LocalDateTime.now());
        }
        applyCompletedDate(book);
        ensureIsbnAvailable(book);
        return bookRepository.save(book);
    }

    @Override
    public Book update(String id, Book updates) {
        Book existing = findById(id);
        copyUpdatableFields(existing, updates);
        if (existing.getDateAdded() == null) {
            existing.setDateAdded(LocalDateTime.now());
        }
        applyCompletedDate(existing);
        ensureIsbnAvailable(existing);
        return bookRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        findById(id);
        bookRepository.deleteById(id);
    }

    private void applyCompletedDate(Book book) {
        if (book.getStatus() == BookStatus.COMPLETED && book.getDateCompleted() == null) {
            book.setDateCompleted(LocalDateTime.now());
        } else if (book.getStatus() != BookStatus.COMPLETED) {
            book.setDateCompleted(null);
        }
    }

    private void ensureIsbnAvailable(Book book) {
        if (book.getIsbn() == null || book.getIsbn().isBlank()) {
            return;
        }
        bookRepository.findByIsbn(book.getIsbn()).ifPresent(existing -> {
            boolean sameBook = book.getId() != null && book.getId().equals(existing.getId());
            if (!sameBook) {
                throw new DuplicateIsbnException("Ya existe un libro con el ISBN: " + book.getIsbn());
            }
        });
    }

    private void copyUpdatableFields(Book target, Book source) {
        target.setTitle(source.getTitle());
        target.setAuthors(source.getAuthors());
        target.setIsbn(source.getIsbn());
        target.setPublisher(source.getPublisher());
        target.setPublishedDate(source.getPublishedDate());
        target.setDescription(source.getDescription());
        target.setPageCount(source.getPageCount());
        target.setCategories(source.getCategories());
        target.setCoverImage(source.getCoverImage());
        target.setLanguage(source.getLanguage());
        target.setStatus(source.getStatus());
        target.setUserRating(source.getUserRating());
        target.setNotes(source.getNotes());
        target.setTags(source.getTags());
        target.setDateCompleted(source.getDateCompleted());
        target.setExternalSource(source.getExternalSource());
        target.setExternalId(source.getExternalId());
    }
}