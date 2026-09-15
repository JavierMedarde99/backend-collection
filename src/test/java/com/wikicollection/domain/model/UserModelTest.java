package com.wikicollection.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class UserModelTest {

    @Test
    void user_buildsWithAllFields() {
        User user = User.builder()
                .id("u1")
                .username("javi_99")
                .email("javi@local.dev")
                .password("hash")
                .displayName("Javi")
                .avatarUrl("http://avatar")
                .bio("Coleccionista")
                .createdAt(LocalDateTime.of(2026, 1, 1, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 12, 0))
                .build();

        assertThat(user.getId()).isEqualTo("u1");
        assertThat(user.getUsername()).isEqualTo("javi_99");
        assertThat(user.getEmail()).isEqualTo("javi@local.dev");
        assertThat(user.getBio()).isEqualTo("Coleccionista");
    }

    @Test
    void password_neverSerializedToJson() throws Exception {
        User user = User.builder().username("javi").password("secreto").build();

        String json = new ObjectMapper().writeValueAsString(user);

        assertThat(json).doesNotContain("secreto");
        assertThat(user.toString()).doesNotContain("secreto");
    }
}
