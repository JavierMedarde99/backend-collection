package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.Funko;
import com.wikicollection.domain.model.FunkoCategory;
import com.wikicollection.domain.model.FunkoSearchResult;

import org.springframework.stereotype.Component;

@Component
public class FunkoDtoMapper {

    public Funko toDomain(FunkoRequest request) {
        if (request == null) {
            return null;
        }
        return Funko.builder()
                .externalId(request.externalId())
                .name(request.name())
                .series(request.series())
                .number(request.number())
                .category(request.category())
                .exclusive(request.exclusive())
                .vaulted(request.vaulted())
                .releaseYear(request.releaseYear())
                .franchise(request.franchise())
                .popType(request.popType())
                .rarityScore(request.rarityScore())
                .imageUrl(request.imageUrl())
                .status(request.status())
                .notes(request.notes())
                .dateAdded(request.dateAdded())
                .build();
    }

    public FunkoResponse toResponse(Funko funko) {
        if (funko == null) {
            return null;
        }
        return new FunkoResponse(
                funko.getId(),
                funko.getExternalId(),
                funko.getName(),
                funko.getSeries(),
                funko.getNumber(),
                funko.getCategory(),
                funko.getExclusive(),
                funko.getVaulted(),
                funko.getReleaseYear(),
                funko.getFranchise(),
                funko.getPopType(),
                funko.getRarityScore(),
                funko.getImageUrl(),
                funko.getStatus(),
                funko.getNotes(),
                funko.getDateAdded(),
                UserOwnedResponse.from(funko.getUserOwned()));
    }

    public FunkoSearchResponse toSearchResponse(FunkoSearchResult result) {
        if (result == null) {
            return null;
        }
        return new FunkoSearchResponse(
                result.externalId(),
                result.name(),
                result.series(),
                result.number(),
                result.category(),
                result.exclusive(),
                result.vaulted(),
                result.releaseYear(),
                result.franchise(),
                result.popType(),
                result.rarityScore(),
                result.imageUrl(),
                result.externalSource());
    }

    public FunkoCategoryResponse toCategoryResponse(FunkoCategory category) {
        if (category == null) {
            return null;
        }
        return new FunkoCategoryResponse(category.name(), category.count());
    }
}
