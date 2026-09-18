package com.wikicollection.infrastructure.adapter.in.web.dto;

import com.wikicollection.domain.model.UserOwned;

public record UserOwnedResponse(
        String ownerId,
        String ownerName) {

    public static UserOwnedResponse from(UserOwned owned) {
        if (owned == null) {
            return null;
        }
        return new UserOwnedResponse(owned.getOwnerId(), owned.getOwnerName());
    }
}
