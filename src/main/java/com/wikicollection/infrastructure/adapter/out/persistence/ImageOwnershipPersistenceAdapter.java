package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import com.wikicollection.domain.model.ImageOwnership;
import com.wikicollection.domain.port.out.ImageOwnershipRepository;

import org.springframework.stereotype.Component;

/**
 * El nombre del archivo es la id del documento en Mongo, así que buscar y
 * borrar por nombre son {@code findById}/{@code deleteById}.
 */
@Component
public class ImageOwnershipPersistenceAdapter implements ImageOwnershipRepository {

    private final SpringDataImageOwnershipRepository springDataRepository;
    private final ImageOwnershipEntityMapper mapper;

    public ImageOwnershipPersistenceAdapter(SpringDataImageOwnershipRepository springDataRepository,
            ImageOwnershipEntityMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public ImageOwnership save(ImageOwnership ownership) {
        return mapper.toDomain(springDataRepository.save(mapper.toEntity(ownership)));
    }

    @Override
    public Optional<ImageOwnership> findByFilename(String filename) {
        return springDataRepository.findById(filename).map(mapper::toDomain);
    }

    @Override
    public void deleteByFilename(String filename) {
        springDataRepository.deleteById(filename);
    }
}
