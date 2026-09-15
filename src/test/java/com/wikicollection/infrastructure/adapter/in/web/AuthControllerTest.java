package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicollection.application.exception.UserAlreadyExistsException;
import com.wikicollection.domain.model.AuthSession;
import com.wikicollection.domain.model.AuthTokens;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.port.in.UserUseCase;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserUseCase userUseCase;

    private User sampleUser() {
        return User.builder().id("u1").username("javi").email("javi@local.dev").build();
    }

    private AuthSession sampleSession() {
        return new AuthSession(sampleUser(), new AuthTokens("access", "refresh"));
    }

    @Test
    void register_returns201_withTokens() throws Exception {
        when(userUseCase.register("javi", "javi@local.dev", "pass12345", "Javi"))
                .thenReturn(sampleSession());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"javi","email":"javi@local.dev","password":"pass12345","displayName":"Javi"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/v1/auth/me")))
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.user.username").value("javi"));
    }

    @Test
    void register_returns400_whenInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"x","email":"no-es-email","password":"corta"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns409_whenUsernameExists() throws Exception {
        when(userUseCase.register(any(), any(), any(), any()))
                .thenThrow(new UserAlreadyExistsException("en uso"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"javi","email":"javi@local.dev","password":"pass12345"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void login_returns200_withTokens() throws Exception {
        when(userUseCase.login("javi", "pass12345")).thenReturn(sampleSession());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"javi","password":"pass12345"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }

    @Test
    void login_returns401_whenBadCredentials() throws Exception {
        when(userUseCase.login("javi", "wrong")).thenThrow(new BadCredentialsException("mal"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"javi","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_returns200_withTokens() throws Exception {
        when(userUseCase.refresh("refresh")).thenReturn(sampleSession());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"refresh"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    @Test
    void me_returnsUser_whenAuthenticated() throws Exception {
        when(userUseCase.getById("u1")).thenReturn(sampleUser());

        mockMvc.perform(get("/api/v1/auth/me").with(user("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("javi"));
    }

    @Test
    void me_returns401_whenAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
