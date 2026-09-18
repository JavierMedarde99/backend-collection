package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.Optional;

import com.wikicollection.application.service.JwtService;
import com.wikicollection.domain.model.Book;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.out.BookRepository;
import com.wikicollection.domain.port.out.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class AuthJwtFlowTest {

    /** Secreto por defecto de application.properties: el JwtService del contexto lo usa. */
    private static final String APP_DEFAULT_SECRET = "clave-cambiar-en-produccion-min-256-bits";

    @MockitoBean
    private com.wikicollection.application.service.OwnerResolver ownerResolver;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private UserRepository userRepository;

    private User sampleUser() {
        return User.builder().id("u1").username("javi").email("javi@local.dev").build();
    }

    private void existingUser() {
        when(userRepository.findByUsername("u1")).thenReturn(Optional.empty());
        when(userRepository.findById("u1")).thenReturn(Optional.of(sampleUser()));
    }

    private void unknownUser() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.findById(any())).thenReturn(Optional.empty());
    }

    private String validBody() {
        return """
                {"title":"Dune","author":"Herbert","state":"TO_READ","type":"NOVEL"}
                """;
    }

    @BeforeEach
    void stubOwnerResolver() {
        lenient().when(ownerResolver.resolveOwner(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> com.wikicollection.domain.model.UserOwned.builder()
                        .ownerId(invocation.getArgument(0)).ownerName("Javi").build());
    }

    @Test
    void validToken_reachesController() throws Exception {
        existingUser();
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book saved = invocation.getArgument(0);
            saved.setId("b-new");
            return saved;
        });
        String token = jwtService.generateAccessToken(sampleUser());

        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("b-new"));
    }

    @Test
    void malformedToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer no-es-un-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_returns401() throws Exception {
        JwtService expiredIssuer = new JwtService(APP_DEFAULT_SECRET, -1000, -1000, "admin");
        String expired = expiredIssuer.generateAccessToken(sampleUser());

        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + expired)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenForUnknownUser_returns401() throws Exception {
        unknownUser();
        String token = jwtService.generateAccessToken(
                User.builder().id("ghost").username("fantasma").build());

        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithWrongSecret_returns401() throws Exception {
        String otherSecret =
                Base64.getEncoder().encodeToString("fedcba9876543210fedcba9876543210".getBytes());
        String foreign = new JwtService(otherSecret, 900000, 604800000, "admin")
                .generateAccessToken(sampleUser());

        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + foreign)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());
    }
}
