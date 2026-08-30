package com.wikicollection.domain.port.out;

import java.util.List;

import com.wikicollection.domain.model.BookSearchResult;

public interface ExternalBookCatalogClient {

    List<BookSearchResult> search(String query);
}