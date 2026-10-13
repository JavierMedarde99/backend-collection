package com.wikicollection.infrastructure.adapter.out.persistence;

import com.wikicollection.domain.model.Funko;

import org.springframework.stereotype.Component;

@Component
public class FunkoEntityMapper {

    public FunkoEntity toEntity(Funko funko) {
        if (funko == null) {
            return null;
        }
        return FunkoEntity.builder()
                .id(funko.getId())
                .ownerId(funko.getOwnerId())
                .userOwned(UserOwnedMapping.toEntity(funko.getUserOwned()))
                .externalId(funko.getExternalId())
                .name(funko.getName())
                .series(funko.getSeries())
                .number(funko.getNumber())
                .category(funko.getCategory())
                .exclusive(funko.getExclusive())
                .vaulted(funko.getVaulted())
                .releaseYear(funko.getReleaseYear())
                .franchise(funko.getFranchise())
                .popType(funko.getPopType())
                .rarityScore(funko.getRarityScore())
                .imageUrl(funko.getImageUrl())
                .status(funko.getStatus())
                .notes(funko.getNotes())
                .dateAdded(funko.getDateAdded())
                .build();
    }

    public Funko toDomain(FunkoEntity entity) {
        if (entity == null) {
            return null;
        }
        return Funko.builder()
                .id(entity.getId())
                .ownerId(entity.getOwnerId())
                .userOwned(UserOwnedMapping.toDomain(entity.getUserOwned()))
                .externalId(entity.getExternalId())
                .name(entity.getName())
                .series(entity.getSeries())
                .number(entity.getNumber())
                .category(entity.getCategory())
                .exclusive(entity.getExclusive())
                .vaulted(entity.getVaulted())
                .releaseYear(entity.getReleaseYear())
                .franchise(entity.getFranchise())
                .popType(entity.getPopType())
                .rarityScore(entity.getRarityScore())
                .imageUrl(entity.getImageUrl())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .dateAdded(entity.getDateAdded())
                .build();
    }
}
