package com.wikicollection.application.service;

import com.wikicollection.application.exception.BookNotFoundException;
import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookState;
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
    public Page<Book> findByState(BookState state, Pageable pageable) {
        return bookRepository.findByState(state, pageable);
    }

    @Override
    public Book findById(String id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException("Libro no encontrado con id: " + id));
    }

    @Override
    public Book save(Book book) {
        return bookRepository.save(book);
    }

    @Override
    public Book update(String id, Book updates) {
        Book existing = findById(id);
        copyUpdatableFields(existing, updates);
        return bookRepository.save(existing);
    }

    @Override
    public void delete(String id) {
        findById(id);
        bookRepository.deleteById(id);
    }

    private void copyUpdatableFields(Book target, Book source) {
        target.setExternalId(source.getExternalId());
        target.setTitle(source.getTitle());
        target.setDescripcion(source.getDescripcion());
        target.setAuthor(source.getAuthor());
        target.setPages(source.getPages());
        target.setType(source.getType());
        target.setState(source.getState());
        target.setComment(source.getComment());
        target.setStart(source.getStart());
        target.setStartDate(source.getStartDate());
        target.setEndDate(source.getEndDate());
        target.setFrontpage(source.getFrontpage());
    }
}
