package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookStatus;
import com.wikicollection.domain.port.out.BookRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class BookPersistenceAdapter implements BookRepository {

    private final SpringDataBookRepository springDataBookRepository;
    private final BookEntityMapper mapper;

    public BookPersistenceAdapter(SpringDataBookRepository springDataBookRepository, BookEntityMapper mapper) {
        this.springDataBookRepository = springDataBookRepository;
        this.mapper = mapper;
    }

    @Override
    public Page<Book> findAll(Pageable pageable) {
        return springDataBookRepository.findAll(pageable).map(mapper::toDomain);
    }

    @Override
    public Optional<Book> findById(String id) {
        return springDataBookRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Page<Book> findByStatus(BookStatus status, Pageable pageable) {
        return springDataBookRepository.findByStatus(status, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Book> findByTagsContaining(String tag, Pageable pageable) {
        return springDataBookRepository.findByTagsContaining(tag, pageable).map(mapper::toDomain);
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return springDataBookRepository.findByIsbn(isbn).map(mapper::toDomain);
    }

    @Override
    public Page<Book> searchByTitleOrAuthor(String query, Pageable pageable) {
        return springDataBookRepository.searchByTitleOrAuthor(query, pageable).map(mapper::toDomain);
    }

    @Override
    public Book save(Book book) {
        BookEntity saved = springDataBookRepository.save(mapper.toEntity(book));
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(String id) {
        springDataBookRepository.deleteById(id);
    }
}