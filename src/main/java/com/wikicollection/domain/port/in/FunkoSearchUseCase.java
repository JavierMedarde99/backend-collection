package com.wikicollection.domain.port.in;

import java.util.List;

import com.wikicollection.domain.model.FunkoCategory;
import com.wikicollection.domain.model.FunkoSearchResult;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FunkoSearchUseCase {

    Page<FunkoSearchResult> search(String query, Pageable pageable);

    List<FunkoCategory> listCategories();
}
