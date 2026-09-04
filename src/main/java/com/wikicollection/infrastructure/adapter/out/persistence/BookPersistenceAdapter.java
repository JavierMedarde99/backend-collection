package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.BookSearchCriteria;
import com.wikicollection.domain.port.out.BookRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class BookPersistenceAdapter implements BookRepository {

    private final SpringDataBookRepository springDataBookRepository;
    private final MongoTemplate mongoTemplate;
    private final BookEntityMapper mapper;

    public BookPersistenceAdapter(SpringDataBookRepository springDataBookRepository,
                                  MongoTemplate mongoTemplate,
                                  BookEntityMapper mapper) {
        this.springDataBookRepository = springDataBookRepository;
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }

    @Override
    public Page<Book> search(BookSearchCriteria criteria, Pageable pageable) {
        Query query = buildQuery(criteria);
        long total = mongoTemplate.count(query, BookEntity.class);
        query.with(pageable);
        List<Book> content = mongoTemplate.find(query, BookEntity.class).stream()
                .map(mapper::toDomain)
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    private Query buildQuery(BookSearchCriteria criteria) {
        Query query = new Query();
        if (criteria.hasName()) {
            query.addCriteria(Criteria.where("title").regex(ciPattern(criteria.name())));
        }
        if (criteria.hasAuthor()) {
            query.addCriteria(Criteria.where("author").regex(ciPattern(criteria.author())));
        }
        if (criteria.type() != null) {
            query.addCriteria(Criteria.where("type").is(criteria.type()));
        }
        if (criteria.state() != null) {
            query.addCriteria(Criteria.where("state").is(criteria.state()));
        }
        return query;
    }

    private Pattern ciPattern(String value) {
        return Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    @Override
    public Optional<Book> findById(String id) {
        return springDataBookRepository.findById(id).map(mapper::toDomain);
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
