package com.wikicollection.application.service;

import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.out.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final String adminUsername;

    public UserDetailsServiceImpl(UserRepository userRepository,
                                  @Value("${app.admin.username:admin}") String adminUsername) {
        this.userRepository = userRepository;
        this.adminUsername = adminUsername;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrId) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(usernameOrId)
                .or(() -> userRepository.findById(usernameOrId))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + usernameOrId));
        String role = adminUsername != null && adminUsername.equals(user.getUsername()) ? "ADMIN" : "USER";
        return new UserPrincipal(user.getId(), user.getUsername(), user.getPassword(), role);
    }
}
