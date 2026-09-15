package com.wikicollection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;

// Fase 8: auto-configuración desactivada hasta #226 (SecurityConfig)
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
public class WikiCollectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(WikiCollectionApplication.class, args);
    }
}