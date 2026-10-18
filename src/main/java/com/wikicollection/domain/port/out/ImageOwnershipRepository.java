package com.wikicollection.domain.port.out;

import java.util.Optional;

import com.wikicollection.domain.model.ImageOwnership;

/**
 * Registro de a quién pertenece cada imagen subida al hosting externo.
 */
public interface ImageOwnershipRepository {

    ImageOwnership save(ImageOwnership ownership);

    Optional<ImageOwnership> findByFilename(String filename);

    void deleteByFilename(String filename);
}
