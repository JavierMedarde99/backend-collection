package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.Amiibo;
import com.wikicollection.domain.model.AmiiboSearchResult;

import org.springframework.stereotype.Component;

@Component
public class AmiiboDtoMapper {

    public Amiibo toDomain(AmiiboRequest request) {
        if (request == null) {
            return null;
        }
        return Amiibo.builder()
                .externalId(request.externalId())
                .name(request.name())
                .character(request.character())
                .amiiboSeries(request.amiiboSeries())
                .gameSeries(request.gameSeries())
                .amiiboType(request.amiiboType())
                .releaseDates(request.releaseDates())
                .imageUrl(request.imageUrl())
                .compatibleGames(request.compatibleGames())
                .status(request.status())
                .notes(request.notes())
                .dateAdded(request.dateAdded())
                .acquisitionDate(request.acquisitionDate())
                .acquisitionPrice(request.acquisitionPrice())
                .build();
    }

    public AmiiboResponse toResponse(Amiibo amiibo) {
        if (amiibo == null) {
            return null;
        }
        return new AmiiboResponse(
                amiibo.getId(),
                amiibo.getExternalId(),
                amiibo.getName(),
                amiibo.getCharacter(),
                amiibo.getAmiiboSeries(),
                amiibo.getGameSeries(),
                amiibo.getAmiiboType(),
                amiibo.getReleaseDates(),
                amiibo.getImageUrl(),
                amiibo.getCompatibleGames(),
                amiibo.getStatus(),
                amiibo.getNotes(),
                amiibo.getDateAdded(),
                amiibo.getAcquisitionDate(),
                amiibo.getAcquisitionPrice(),
                UserOwnedResponse.from(amiibo.getUserOwned()));
    }

    public AmiiboSearchResponse toSearchResponse(AmiiboSearchResult result) {
        if (result == null) {
            return null;
        }
        return new AmiiboSearchResponse(
                result.externalId(),
                result.name(),
                result.character(),
                result.amiiboSeries(),
                result.gameSeries(),
                result.amiiboType(),
                result.releaseDates(),
                result.imageUrl(),
                result.compatibleGames(),
                result.externalSource());
    }
}
