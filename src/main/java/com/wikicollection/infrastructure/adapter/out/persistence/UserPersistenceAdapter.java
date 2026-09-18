package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.out.UserRepository;

import org.springframework.stereotype.Component;

@Component
public class UserPersistenceAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserEntityMapper mapper;

    public UserPersistenceAdapter(SpringDataUserRepository springDataUserRepository,
                                  UserEntityMapper mapper) {
        this.springDataUserRepository = springDataUserRepository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        return mapper.toDomain(springDataUserRepository.save(mapper.toEntity(user)));
    }

    @Override
    public Optional<User> findById(String id) {
        return springDataUserRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return springDataUserRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataUserRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return springDataUserRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email);
    }

    @Override
    public void deleteById(String userId) {
        springDataUserRepository.deleteById(userId);
    }
}
