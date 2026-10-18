package com.wikicollection.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.wikicollection.application.exception.ForbiddenException;
import com.wikicollection.domain.model.ImageOwnership;
import com.wikicollection.domain.model.User;
import com.wikicollection.domain.model.UserOwned;
import com.wikicollection.domain.port.out.ImageHostingClient;
import com.wikicollection.domain.port.out.ImageOwnershipRepository;
import com.wikicollection.domain.port.out.UserRepository;

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

    @Mock
    private ImageOwnershipRepository ownershipRepository;

    @Mock
    private OwnerResolver ownerResolver;

    @Mock
    private UserRepository userRepository;

    private OwnershipValidator ownershipValidator;

    private ImageStorageService service;

    @BeforeEach
    void setUp() {
        ownershipValidator = new OwnershipValidator(userRepository, "admin");
        service = new ImageStorageService(
                hostingClient, ownershipRepository, ownershipValidator, ownerResolver,
                5242880, "image/jpeg,image/png,image/webp,image/gif");
        lenient().when(ownerResolver.resolveOwner(any())).thenAnswer(invocation -> UserOwned.builder()
                .ownerId(invocation.getArgument(0)).ownerName("Javi").username("Javi").build());
    }

    private MultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    private User user(String id, String username) {
        return User.builder().id(id).username(username).build();
    }

    @Test
    void store_uploadsValidatedFile_andReturnsUrl() {
        when(hostingClient.upload(any(), eq("foto.png"), eq("image/png")))
                .thenReturn("https://files.catbox.moe/abc123.png");

        String url = service.store(file("foto.png", "image/png", PNG), "u1");

        assertThat(url).isEqualTo("https://files.catbox.moe/abc123.png");
    }

    @Test
    void store_registersTheUploaderAsOwner() {
        when(hostingClient.upload(any(), eq("foto.png"), eq("image/png")))
                .thenReturn("https://files.catbox.moe/abc123.png");

        service.store(file("foto.png", "image/png", PNG), "u1");

        verify(ownershipRepository).save(new ImageOwnership("abc123.png", "u1", "Javi"));
    }

    @Test
    void store_withoutUser_doesNotRegisterOwnership() {
        when(hostingClient.upload(any(), any(), any())).thenReturn("https://files.catbox.moe/sin-dueño.png");

        service.store(file("foto.png", "image/png", PNG), null);

        verifyNoInteractions(ownershipRepository);
    }

    @Test
    void store_acceptsAllAllowedTypes() {
        when(hostingClient.upload(any(), any(), any())).thenReturn("https://files.catbox.moe/x");

        assertThat(service.store(file("a.jpg", "image/jpeg", JPEG), "u1")).startsWith("http");
        assertThat(service.store(file("a.gif", "image/gif", GIF), "u1")).startsWith("http");
        assertThat(service.store(file("a.webp", "image/webp", WEBP), "u1")).startsWith("http");
    }

    @Test
    void store_rejectsEmptyFile() {
        assertThatThrownBy(() -> service.store(file("a.png", "image/png", new byte[0]), "u1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void store_rejectsOversizeFile() {
        ImageStorageService tiny = new ImageStorageService(
                hostingClient, ownershipRepository, ownershipValidator, ownerResolver, 5, "image/png");

        assertThatThrownBy(() -> tiny.store(file("a.png", "image/png", PNG), "u1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamaño máximo");
    }

    @Test
    void store_rejectsDisallowedMimeType() {
        assertThatThrownBy(() -> service.store(file("a.pdf", "application/pdf", PNG), "u1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void store_rejectsJpgExtensionWithPngContent() {
        assertThatThrownBy(() -> service.store(file("a.jpg", "image/jpeg", PNG), "u1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void store_rejectsRandomBinary() {
        assertThatThrownBy(() -> service.store(file("a.png", "image/png", new byte[]{0x00, 0x01, 0x02}), "u1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------ autorización (#481)

    @Test
    void delete_deletesTheImage_whenTheUserIsTheOwner() {
        when(ownershipRepository.findByFilename("abc123.png"))
                .thenReturn(Optional.of(new ImageOwnership("abc123.png", "u1", "Javi")));

        service.delete("abc123.png", "u1");

        verify(hostingClient).delete("abc123.png");
        verify(ownershipRepository).deleteByFilename("abc123.png");
    }

    @Test
    void delete_forbidden_whenSomeoneElseUploadedIt() {
        when(ownershipRepository.findByFilename("abc123.png"))
                .thenReturn(Optional.of(new ImageOwnership("abc123.png", "u1", "Javi")));
        when(userRepository.findById("u2")).thenReturn(Optional.of(user("u2", "u2")));

        assertThatThrownBy(() -> service.delete("abc123.png", "u2"))
                .isInstanceOf(ForbiddenException.class);

        verify(hostingClient, never()).delete(any());
        verify(ownershipRepository, never()).deleteByFilename(any());
    }

    @Test
    void delete_allowedForAdmin_whenSomeoneElseUploadedIt() {
        when(ownershipRepository.findByFilename("abc123.png"))
                .thenReturn(Optional.of(new ImageOwnership("abc123.png", "u1", "Javi")));
        when(userRepository.findById("u9")).thenReturn(Optional.of(user("u9", "admin")));

        service.delete("abc123.png", "u9");

        verify(hostingClient).delete("abc123.png");
        verify(ownershipRepository).deleteByFilename("abc123.png");
    }

    @Test
    void delete_forbidden_whenThereIsNoOwnershipRecord_andUserIsNotAdmin() {
        when(ownershipRepository.findByFilename("legacy.png")).thenReturn(Optional.empty());
        when(userRepository.findById("u2")).thenReturn(Optional.of(user("u2", "u2")));

        assertThatThrownBy(() -> service.delete("legacy.png", "u2"))
                .isInstanceOf(ForbiddenException.class);

        verify(hostingClient, never()).delete(any());
    }

    @Test
    void delete_allowedForAdmin_whenThereIsNoOwnershipRecord() {
        when(ownershipRepository.findByFilename("legacy.png")).thenReturn(Optional.empty());
        when(userRepository.findById("u9")).thenReturn(Optional.of(user("u9", "admin")));

        service.delete("legacy.png", "u9");

        verify(hostingClient).delete("legacy.png");
    }

    @Test
    void delete_rejectsPathTraversal() {
        assertThatThrownBy(() -> service.delete("../evil.png", "u1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.delete("sub/evil.png", "u1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
