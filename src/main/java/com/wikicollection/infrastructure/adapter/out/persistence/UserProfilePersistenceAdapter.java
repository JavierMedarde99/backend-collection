package com.wikicollection.infrastructure.adapter.out.persistence;

import java.util.Optional;

import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.out.UserProfilePort;

import org.springframework.stereotype.Component;

@Component
public class UserProfilePersistenceAdapter implements UserProfilePort {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserEntityMapper mapper;

    public UserProfilePersistenceAdapter(SpringDataUserRepository springDataUserRepository,
                                         UserEntityMapper mapper) {
        this.springDataUserRepository = springDataUserRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataUserRepository.findByUsername(username).map(mapper::toDomain);
    }
}
