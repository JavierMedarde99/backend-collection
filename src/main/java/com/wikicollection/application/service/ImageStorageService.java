package com.wikicollection.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.wikicollection.domain.port.out.ImageHostingClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {

    private static final Map<String, String> EXTENSION_BY_MIME = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final ImageHostingClient imageHostingClient;
    private final long maxSize;
    private final Set<String> allowedTypes;

    public ImageStorageService(
            ImageHostingClient imageHostingClient,
            @Value("${app.image.max-size:5242880}") long maxSize,
            @Value("${app.image.allowed-types:image/jpeg,image/png,image/webp,image/gif}") String allowedTypes) {
        this.imageHostingClient = imageHostingClient;
        this.maxSize = maxSize;
        this.allowedTypes = Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    public String store(MultipartFile file) {
        validate(file);
        try {
            byte[] content = file.getInputStream().readAllBytes();
            String filename = file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
                    ? file.getOriginalFilename()
                    : "upload" + EXTENSION_BY_MIME.get(file.getContentType());
            return imageHostingClient.upload(content, filename, file.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la imagen", e);
        }
    }

    public void delete(String filename) {
        if (filename == null || filename.isBlank()
                || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Nombre de archivo inválido: " + filename);
        }
        imageHostingClient.delete(filename);
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("El archivo supera el tamaño máximo de " + maxSize + " bytes");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType) || !EXTENSION_BY_MIME.containsKey(contentType)) {
            throw new IllegalArgumentException("Tipo de imagen no permitido: " + contentType);
        }
        if (!matchesMagicBytes(file, contentType)) {
            throw new IllegalArgumentException("El contenido no corresponde a una imagen " + contentType);
        }
    }

    private boolean matchesMagicBytes(MultipartFile file, String contentType) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(12);
            return switch (contentType) {
                case "image/jpeg" -> startsWith(header, 0xFF, 0xD8, 0xFF);
                case "image/png" -> startsWith(header, 0x89, 0x50, 0x4E, 0x47);
                case "image/gif" -> startsWith(header, 0x47, 0x49, 0x46, 0x38);
                case "image/webp" -> startsWith(header, 0x52, 0x49, 0x46, 0x46)
                        && header.length >= 12
                        && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50;
                default -> false;
            };
        } catch (IOException e) {
            return false;
        }
    }

    private boolean startsWith(byte[] header, int... bytes) {
        if (header.length < bytes.length) {
            return false;
        }
        for (int i = 0; i < bytes.length; i++) {
            if ((header[i] & 0xFF) != bytes[i]) {
                return false;
            }
        }
        return true;
    }
}
