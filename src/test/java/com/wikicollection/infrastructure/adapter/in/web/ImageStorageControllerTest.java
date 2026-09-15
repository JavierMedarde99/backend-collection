package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicollection.application.exception.CatboxUploadException;
import com.wikicollection.application.service.ImageStorageService;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "app.boardgame-status-migration.enabled=false"})
@AutoConfigureMockMvc
class ImageStorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageStorageService imageStorageService;

    private MockMultipartFile pngFile() {
        byte[] content = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        return new MockMultipartFile("file", "foto.png", "image/png", content);
    }

    @Test
    void upload_returns201_withCatboxUrl() throws Exception {
        when(imageStorageService.store(any())).thenReturn("https://files.catbox.moe/abc123.png");

        mockMvc.perform(multipart("/api/v1/images/upload").with(user("u1")).file(pngFile()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "https://files.catbox.moe/abc123.png"))
                .andExpect(jsonPath("$.filename").value("abc123.png"))
                .andExpect(jsonPath("$.url").value("https://files.catbox.moe/abc123.png"));
    }

    @Test
    void upload_returns400_whenInvalid() throws Exception {
        when(imageStorageService.store(any())).thenThrow(new IllegalArgumentException("Tipo no permitido"));

        mockMvc.perform(multipart("/api/v1/images/upload").with(user("u1")).file(pngFile()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_returns502_whenCatboxFails() throws Exception {
        when(imageStorageService.store(any())).thenThrow(new CatboxUploadException("caído"));

        mockMvc.perform(multipart("/api/v1/images/upload").with(user("u1")).file(pngFile()))
                .andExpect(status().isBadGateway());
    }

    @Test
    void upload_returns400_whenMissingFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/images/upload").with(user("u1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204_whenExists() throws Exception {
        mockMvc.perform(delete("/api/v1/images/abc123.png").with(user("u1")))
                .andExpect(status().isNoContent());

        verify(imageStorageService).delete("abc123.png");
    }

    @Test
    void delete_returns400_whenInvalidName() throws Exception {
        doThrow(new IllegalArgumentException("Sin userhash")).when(imageStorageService).delete("nope.png");

        mockMvc.perform(delete("/api/v1/images/nope.png").with(user("u1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cors_allowsFrontendOrigin() throws Exception {
        when(imageStorageService.store(any())).thenReturn("https://files.catbox.moe/abc123.png");

        mockMvc.perform(multipart("/api/v1/images/upload").with(user("u1"))
                        .file(pngFile())
                        .header("Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
