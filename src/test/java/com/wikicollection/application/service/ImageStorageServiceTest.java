package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import com.wikicollection.application.exception.ImageNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ImageStorageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x10};
    private static final byte[] GIF = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x00};
    private static final byte[] WEBP = {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50, 0x00};

    @TempDir
    private Path tempDir;

    private ImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new ImageStorageService(
                tempDir.resolve("images").toString(), 5242880, "image/jpeg,image/png,image/webp,image/gif");
    }

    private MultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    @Test
    void store_validPng_returnsUuidFilename() throws Exception {
        String filename = service.store(file("foto.png", "image/png", PNG));

        assertThat(filename).endsWith(".png").doesNotContain("foto");
        assertThat(Files.exists(tempDir.resolve("images").resolve(filename))).isTrue();
    }

    @Test
    void store_validTypes_keepExtension() {
        assertThat(service.store(file("a.jpg", "image/jpeg", JPEG))).endsWith(".jpg");
        assertThat(service.store(file("a.gif", "image/gif", GIF))).endsWith(".gif");
        assertThat(service.store(file("a.webp", "image/webp", WEBP))).endsWith(".webp");
    }

    @Test
    void store_generatesUniqueNames() {
        String first = service.store(file("a.png", "image/png", PNG));
        String second = service.store(file("a.png", "image/png", PNG));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void store_createsDirectory_whenMissing() {
        ImageStorageService nested = new ImageStorageService(
                tempDir.resolve("a").resolve("b").toString(), 5242880, "image/png");

        String filename = nested.store(file("a.png", "image/png", PNG));

        assertThat(Files.exists(tempDir.resolve("a").resolve("b").resolve(filename))).isTrue();
    }

    @Test
    void store_rejectsEmptyFile() {
        assertThatThrownBy(() -> service.store(file("a.png", "image/png", new byte[0])))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void store_rejectsOversizeFile() {
        ImageStorageService tiny = new ImageStorageService(
                tempDir.toString(), 5, "image/png");

        assertThatThrownBy(() -> tiny.store(file("a.png", "image/png", PNG)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamaño máximo");
    }

    @Test
    void store_rejectsDisallowedMimeType() {
        assertThatThrownBy(() -> service.store(file("a.pdf", "application/pdf", PNG)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void store_rejectsJpgExtensionWithPngContent() {
        assertThatThrownBy(() -> service.store(file("a.jpg", "image/jpeg", PNG)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void store_rejectsRandomBinary() {
        assertThatThrownBy(() -> service.store(file("a.png", "image/png", new byte[]{0x00, 0x01, 0x02})))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void load_returnsResource_whenExists() throws Exception {
        String filename = service.store(file("a.png", "image/png", PNG));

        Resource resource = service.load(filename);

        assertThat(resource.exists()).isTrue();
        assertThat(resource.contentLength()).isEqualTo(PNG.length);
    }

    @Test
    void load_throwsNotFound_whenMissing() {
        assertThatThrownBy(() -> service.load("nope.png"))
                .isInstanceOf(ImageNotFoundException.class);
    }

    @Test
    void load_rejectsPathTraversal() {
        assertThatThrownBy(() -> service.load("../evil.png"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.load("sub/evil.png"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_removesExistingFile() {
        String filename = service.store(file("a.png", "image/png", PNG));

        service.delete(filename);

        assertThat(Files.exists(tempDir.resolve("images").resolve(filename))).isFalse();
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        assertThatThrownBy(() -> service.delete("nope.png"))
                .isInstanceOf(ImageNotFoundException.class);
    }
}
