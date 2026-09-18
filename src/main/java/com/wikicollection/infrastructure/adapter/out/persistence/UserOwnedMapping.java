package com.wikicollection.infrastructure.adapter.out.persistence;

import com.wikicollection.domain.model.UserOwned;

final class UserOwnedMapping {

    private UserOwnedMapping() {
    }

    static UserOwnedEntity toEntity(UserOwned owned) {
        if (owned == null) {
            return null;
        }
        return UserOwnedEntity.builder()
                .ownerId(owned.getOwnerId())
                .ownerName(owned.getOwnerName())
                .build();
    }

    static UserOwned toDomain(UserOwnedEntity entity) {
        if (entity == null) {
            return null;
        }
        return UserOwned.builder()
                .ownerId(entity.getOwnerId())
                .ownerName(entity.getOwnerName())
                .build();
    }
}
