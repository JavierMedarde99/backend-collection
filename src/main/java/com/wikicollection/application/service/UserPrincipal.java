package com.wikicollection.application.service;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal autenticado: {@link #getUsername()} es el username legible y
 * {@link #getId()} el identificador persistido (subject del JWT).
 */
public class UserPrincipal implements UserDetails {

    private final String id;
    private final String username;
    private final String password;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(String id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public String getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
