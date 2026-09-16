package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.wikicollection.domain.model.CollectionType;
import com.wikicollection.domain.model.CollectionVisibility;
import com.wikicollection.domain.model.UserPreferences;
import com.wikicollection.domain.port.in.UserPreferencesUseCase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class UserPreferencesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserPreferencesUseCase preferencesUseCase;

    private UserPreferences samplePrefs() {
        return UserPreferences.defaults("u1");
    }

    @Test
    void getPreferences_returnsPreferences_whenAuthenticated() throws Exception {
        when(preferencesUseCase.getPreferences("u1")).thenReturn(samplePrefs());

        mockMvc.perform(get("/api/v1/preferences").with(user("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"))
                .andExpect(jsonPath("$.activeCollections.books").value(true));
    }

    @Test
    void getPreferences_returns401_whenAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/preferences"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatePreferences_replacesPreferences() throws Exception {
        when(preferencesUseCase.updatePreferences(eq("u1"), eq(Map.of("books", true)),
                eq(Map.of("books", CollectionVisibility.PRIVATE)))).thenReturn(samplePrefs());

        mockMvc.perform(put("/api/v1/preferences").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activeCollections":{"books":true},"collectionVisibility":{"books":"PRIVATE"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("u1"));
    }

    @Test
    void updateVisibility_returns400_whenInvalidValue() throws Exception {
        mockMvc.perform(patch("/api/v1/preferences/collection-visibility").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"visibility":{"books":"ULTRA"}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchActiveCollections_merges() throws Exception {
        when(preferencesUseCase.setActiveCollections(eq("u1"), eq(Map.of("books", false))))
                .thenReturn(samplePrefs());

        mockMvc.perform(patch("/api/v1/preferences/active-collections").with(user("u1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"collections":{"books":false}}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void getActiveCollections_returnsList() throws Exception {
        when(preferencesUseCase.getActiveCollections("u1"))
                .thenReturn(List.of(CollectionType.BOOKS, CollectionType.GAMES));

        mockMvc.perform(get("/api/v1/preferences/active-collections").with(user("u1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("BOOKS"));
    }
}
