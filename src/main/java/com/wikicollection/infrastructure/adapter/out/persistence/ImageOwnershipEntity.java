package com.wikicollection.infrastructure.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Propietario de una imagen subida a Catbox (#481). El nombre del archivo es el
 * id del documento: Catbox genera nombres únicos, así que un registro por
 * archivo garantiza un solo propietario sin necesidad de índice adicional.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "images")
public class ImageOwnershipEntity {

    @Id
    private String filename;

    private String ownerId;

    private String ownerName;
}
