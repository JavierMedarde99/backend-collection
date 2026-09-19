package com.wikicollection.domain.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class User {

    private String id;

    @Size(min = 3, max = 20, message = "El username debe tener entre 3 y 20 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "El username solo admite letras, números y _")
    private String username;

    @Email(message = "Email inválido")
    private String email;

    @JsonIgnore
    @ToString.Exclude
    private String password;

    private String displayName;
    private String avatarUrl;

    @Size(max = 200, message = "La bio no puede superar los 200 caracteres")
    private String bio;

    @Pattern(regexp = "^\\d{17}$", message = "El SteamId debe ser un SteamID64 de 17 dígitos")
    private String steamId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
