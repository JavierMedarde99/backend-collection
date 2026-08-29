package com.wikicollection.domain.port.in;

import java.util.List;

import com.wikicollection.domain.model.BookSearchResult;

public interface BookSearchUseCase {

    List<BookSearchResult> search(String query);
}