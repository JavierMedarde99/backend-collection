package com.wikicollection.infrastructure.adapter.out.persistence;

import com.wikicollection.domain.model.ImageOwnership;

import org.springframework.stereotype.Component;

@Component
public class ImageOwnershipEntityMapper {

    public ImageOwnershipEntity toEntity(ImageOwnership ownership) {
        if (ownership == null) {
            return null;
        }
        return ImageOwnershipEntity.builder()
                .filename(ownership.filename())
                .ownerId(ownership.ownerId())
                .ownerName(ownership.ownerName())
                .build();
    }

    public ImageOwnership toDomain(ImageOwnershipEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ImageOwnership(entity.getFilename(), entity.getOwnerId(), entity.getOwnerName());
    }
}
