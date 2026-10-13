package com.wikicollection.infrastructure.adapter.out.persistence;

import com.wikicollection.domain.model.Amiibo;
import com.wikicollection.domain.model.AmiiboReleaseDates;

import org.springframework.stereotype.Component;

@Component
public class AmiiboEntityMapper {

    public AmiiboEntity toEntity(Amiibo amiibo) {
        if (amiibo == null) {
            return null;
        }
        AmiiboReleaseDates releaseDates = amiibo.getReleaseDates();
        return AmiiboEntity.builder()
                .id(amiibo.getId())
                .ownerId(amiibo.getOwnerId())
                .userOwned(UserOwnedMapping.toEntity(amiibo.getUserOwned()))
                .externalId(amiibo.getExternalId())
                .name(amiibo.getName())
                .character(amiibo.getCharacter())
                .amiiboSeries(amiibo.getAmiiboSeries())
                .gameSeries(amiibo.getGameSeries())
                .amiiboType(amiibo.getAmiiboType())
                .releaseDateAu(releaseDates == null ? null : releaseDates.au())
                .releaseDateEu(releaseDates == null ? null : releaseDates.eu())
                .releaseDateJp(releaseDates == null ? null : releaseDates.jp())
                .releaseDateNa(releaseDates == null ? null : releaseDates.na())
                .imageUrl(amiibo.getImageUrl())
                .compatibleGames(amiibo.getCompatibleGames())
                .status(amiibo.getStatus())
                .notes(amiibo.getNotes())
                .dateAdded(amiibo.getDateAdded())
                .build();
    }

    public Amiibo toDomain(AmiiboEntity entity) {
        if (entity == null) {
            return null;
        }
        return Amiibo.builder()
                .id(entity.getId())
                .ownerId(entity.getOwnerId())
                .userOwned(UserOwnedMapping.toDomain(entity.getUserOwned()))
                .externalId(entity.getExternalId())
                .name(entity.getName())
                .character(entity.getCharacter())
                .amiiboSeries(entity.getAmiiboSeries())
                .gameSeries(entity.getGameSeries())
                .amiiboType(entity.getAmiiboType())
                .releaseDates(toReleaseDates(entity))
                .imageUrl(entity.getImageUrl())
                .compatibleGames(entity.getCompatibleGames())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .dateAdded(entity.getDateAdded())
                .build();
    }

    private static AmiiboReleaseDates toReleaseDates(AmiiboEntity entity) {
        if (entity.getReleaseDateAu() == null && entity.getReleaseDateEu() == null
                && entity.getReleaseDateJp() == null && entity.getReleaseDateNa() == null) {
            return null;
        }
        return new AmiiboReleaseDates(
                entity.getReleaseDateAu(),
                entity.getReleaseDateEu(),
                entity.getReleaseDateJp(),
                entity.getReleaseDateNa());
    }
}
