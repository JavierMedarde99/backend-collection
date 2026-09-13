package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wikicollection.domain.port.out.ImageHostingClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ImageStorageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x10};
    private static final byte[] GIF = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x00};
    private static final byte[] WEBP = {0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00,
            0x57, 0x45, 0x42, 0x50, 0x00};

    @Mock
    private ImageHostingClient hostingClient;

    private ImageStorageService service;

    @BeforeEach
    void setUp() {
        service = new ImageStorageService(
                hostingClient, 5242880, "image/jpeg,image/png,image/webp,image/gif");
    }

    private MultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    @Test
    void store_uploadsValidatedFile_andReturnsUrl() {
        when(hostingClient.upload(any(), eq("foto.png"), eq("image/png")))
                .thenReturn("https://files.catbox.moe/abc123.png");

        String url = service.store(file("foto.png", "image/png", PNG));

        assertThat(url).isEqualTo("https://files.catbox.moe/abc123.png");
    }

    @Test
    void store_acceptsAllAllowedTypes() {
        when(hostingClient.upload(any(), any(), any())).thenReturn("https://files.catbox.moe/x");

        assertThat(service.store(file("a.jpg", "image/jpeg", JPEG))).startsWith("http");
        assertThat(service.store(file("a.gif", "image/gif", GIF))).startsWith("http");
        assertThat(service.store(file("a.webp", "image/webp", WEBP))).startsWith("http");
    }

    @Test
    void store_rejectsEmptyFile() {
        assertThatThrownBy(() -> service.store(file("a.png", "image/png", new byte[0])))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void store_rejectsOversizeFile() {
        ImageStorageService tiny = new ImageStorageService(hostingClient, 5, "image/png");

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
    void delete_delegatesToHostingClient() {
        service.delete("abc123.png");

        verify(hostingClient).delete("abc123.png");
    }

    @Test
    void delete_rejectsPathTraversal() {
        assertThatThrownBy(() -> service.delete("../evil.png"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.delete("sub/evil.png"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
