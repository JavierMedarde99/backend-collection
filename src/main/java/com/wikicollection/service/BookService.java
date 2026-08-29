package com.wikicollection.service;

import java.time.LocalDateTime;

import com.wikicollection.model.Book;
import com.wikicollection.model.BookStatus;
import com.wikicollection.repository.BookRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Page<Book> findAll(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    public Page<Book> findByStatus(BookStatus status, Pageable pageable) {
        return bookRepository.findByStatus(status, pageable);
    }

    public Page<Book> findByTag(String tag, Pageable pageable) {
        return bookRepository.findByTagsContaining(tag, pageable);
    }

    public Page<Book> searchByTitleOrAuthor(String query, Pageable pageable) {
        return bookRepository.searchByTitleOrAuthor(query, pageable);
    }

    public Book findById(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Libro no encontrado con id: " + id));
    }

    public Book save(Book book) {
        if (book.getDateAdded() == null) {
            book.setDateAdded(LocalDateTime.now());
        }
        applyCompletedDate(book);
        ensureIsbnAvailable(book);
        return bookRepository.save(book);
    }

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

    public void delete(String id) {
        Book book = findById(id);
        bookRepository.delete(book);
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
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe un libro con el ISBN: " + book.getIsbn());
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