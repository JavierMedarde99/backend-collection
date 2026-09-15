package com.wikicollection.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.wikicollection.domain.model.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterTest {

    @Mock
    private SpringDataUserRepository springDataUserRepository;

    @Mock
    private UserEntityMapper mapper;

    @InjectMocks
    private UserPersistenceAdapter adapter;

    private User sampleUser() {
        return User.builder().id("u1").username("javi").email("javi@local.dev").build();
    }

    @Test
    void save_mapsDomainToEntity_andBack() {
        User user = sampleUser();
        UserEntity entity = UserEntity.builder().id("u1").username("javi").build();
        when(mapper.toEntity(user)).thenReturn(entity);
        when(springDataUserRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(user);

        assertThat(adapter.save(user)).isSameAs(user);
    }

    @Test
    void findByUsername_mapsEntity_whenExists() {
        UserEntity entity = UserEntity.builder().id("u1").username("javi").build();
        User expected = sampleUser();
        when(springDataUserRepository.findByUsername("javi")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(expected);

        assertThat(adapter.findByUsername("javi")).contains(expected);
    }

    @Test
    void findByEmail_returnsEmpty_whenMissing() {
        when(springDataUserRepository.findByEmail("no@local.dev")).thenReturn(Optional.empty());

        assertThat(adapter.findByEmail("no@local.dev")).isEmpty();
    }

    @Test
    void exists_delegatesToSpringData() {
        when(springDataUserRepository.existsByUsername("javi")).thenReturn(true);
        when(springDataUserRepository.existsByEmail("javi@local.dev")).thenReturn(false);

        assertThat(adapter.existsByUsername("javi")).isTrue();
        assertThat(adapter.existsByEmail("javi@local.dev")).isFalse();
    }

    @Test
    void mapper_roundTripsAllFields() {
        User user = User.builder()
                .id("u1").username("javi").email("javi@local.dev").password("hash")
                .displayName("Javi").avatarUrl("http://avatar").bio("Bio")
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 12, 0))
                .build();

        User roundTripped = new UserEntityMapper().toDomain(new UserEntityMapper().toEntity(user));

        assertThat(roundTripped).usingRecursiveComparison().isEqualTo(user);
    }

    @Test
    void mapper_handlesNull() {
        UserEntityMapper userMapper = new UserEntityMapper();

        assertThat(userMapper.toEntity(null)).isNull();
        assertThat(userMapper.toDomain(null)).isNull();
    }
}
