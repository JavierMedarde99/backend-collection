package com.wikicollection.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wikicollection.application.exception.ImageNotFoundException;
import com.wikicollection.application.service.ImageStorageService;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
    void upload_returns201_withUrl() throws Exception {
        when(imageStorageService.store(any())).thenReturn("abc-123.png");

        mockMvc.perform(multipart("/api/v1/images/upload").file(pngFile()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/v1/images/abc-123.png")))
                .andExpect(jsonPath("$.filename").value("abc-123.png"))
                .andExpect(jsonPath("$.url").value(Matchers.containsString("/api/v1/images/abc-123.png")));
    }

    @Test
    void upload_returns400_whenInvalid() throws Exception {
        when(imageStorageService.store(any())).thenThrow(new IllegalArgumentException("Tipo no permitido"));

        mockMvc.perform(multipart("/api/v1/images/upload").file(pngFile()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_returns400_whenMissingFile() throws Exception {
        mockMvc.perform(multipart("/api/v1/images/upload"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204_whenExists() throws Exception {
        mockMvc.perform(delete("/api/v1/images/abc-123.png"))
                .andExpect(status().isNoContent());

        verify(imageStorageService).delete("abc-123.png");
    }

    @Test
    void delete_returns404_whenMissing() throws Exception {
        doThrow(new ImageNotFoundException("nope")).when(imageStorageService).delete("nope.png");

        mockMvc.perform(delete("/api/v1/images/nope.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    void serve_returnsContent_withCacheHeaders() throws Exception {
        byte[] content = {(byte) 0x89, 0x50, 0x4E, 0x47};
        when(imageStorageService.load("abc-123.png"))
                .thenReturn(new org.springframework.core.io.ByteArrayResource(content));

        mockMvc.perform(get("/api/v1/images/abc-123.png"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", Matchers.containsString("max-age=86400")));
    }

    @Test
    void serve_returns404_whenMissing() throws Exception {
        when(imageStorageService.load("nope.png")).thenThrow(new ImageNotFoundException("nope"));

        mockMvc.perform(get("/api/v1/images/nope.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cors_allowsFrontendOrigin() throws Exception {
        mockMvc.perform(multipart("/api/v1/images/upload")
                        .file(pngFile())
                        .header("Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
