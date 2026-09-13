package com.wikicollection.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.data.mongodb.auto-index-creation=false",
        "app.boardgame-status-migration.enabled=false",
        "app.image.storage.path=target/test-images"})
@AutoConfigureMockMvc
class StaticImageServingTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void cleanStorage() throws Exception {
        Path dir = Paths.get("target/test-images");
        if (Files.exists(dir)) {
            try (Stream<Path> files = Files.walk(dir)) {
                files.sorted(Comparator.reverseOrder())
                        .forEach(path -> path.toFile().delete());
            }
        }
    }

    @Test
    void getImage_returnsContentWithCacheHeaders() throws Exception {
        mockMvc.perform(multipart("/api/v1/images/upload")
                        .file(new MockMultipartFile("file", "foto.png", "image/png", PNG)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", Matchers.containsString("/api/v1/images/")));

        String filename;
        try (Stream<Path> files = Files.list(Paths.get("target/test-images"))) {
            filename = files.findFirst().orElseThrow().getFileName().toString();
        }

        mockMvc.perform(get("/api/v1/images/{filename}", filename))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("image/png")))
                .andExpect(header().string("Cache-Control", Matchers.containsString("max-age=86400")));
    }

    @Test
    void getImage_returns404_whenMissing() throws Exception {
        mockMvc.perform(get("/api/v1/images/no-existe.png"))
                .andExpect(status().isNotFound());
    }
}
