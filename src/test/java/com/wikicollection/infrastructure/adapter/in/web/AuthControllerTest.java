package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicollection.application.exception.UserAlreadyExistsException;
import com.wikicollection.application.service.UserPrincipal;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
        when(userUseCase.register("javi", "javi@local.dev", "Pass1234!", "Javi"))
                .thenReturn(sampleSession());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"javi","email":"javi@local.dev","password":"Pass1234!","displayName":"Javi"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/v1/auth/me")))
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.user.username").value("javi"));
    }

    @Test
    void register_responseOmitsPrivateUserFields() throws Exception {
        when(userUseCase.register("javi", "javi@local.dev", "Pass1234!", "Javi"))
                .thenReturn(sampleSession());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"javi","email":"javi@local.dev","password":"Pass1234!","displayName":"Javi"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/v1/auth/me")))
                .andExpect(jsonPath("$.user.username").value("javi"))
                .andExpect(jsonPath("$.user.email").doesNotExist())
                .andExpect(jsonPath("$.user.updatedAt").doesNotExist());
    }

    @Test
    void register_returns400_whenWeakPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"javi","email":"javi@local.dev","password":"pass12345"}
                                """))
                .andExpect(status().isBadRequest());
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
                                {"username":"javi","email":"javi@local.dev","password":"Pass1234!"}
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

    private static org.springframework.security.core.Authentication authenticationFor(String id, String username) {
        var principal = new UserPrincipal(id, username, "hash", "USER");
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void me_returnsUser_whenAuthenticated() throws Exception {
        when(userUseCase.getById("u1")).thenReturn(sampleUser());
        var principal = new UserPrincipal("u1", "javi", "hash", "USER");
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/auth/me").with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("javi"));
    }

    @Test
    void patchMe_updatesProfile() throws Exception {
        User updated = User.builder().id("u1").username("javi").displayName("Nuevo nombre").build();
        when(userUseCase.updateProfile(eq("u1"), eq("Nuevo nombre"), isNull(), isNull()))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/v1/auth/me").with(authentication(authenticationFor("u1", "javi")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Nuevo nombre"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Nuevo nombre"));
    }

    @Test
    void patchMe_returns401_whenAnonymous() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"X"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteMe_removesAccount() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/me").with(authentication(authenticationFor("u1", "javi"))))
                .andExpect(status().isNoContent());
        verify(userUseCase).deleteAccount("u1");
    }

    @Test
    void deleteMe_returns401_whenAnonymous() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returns401_whenAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
