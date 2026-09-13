package com.wikicollection.infrastructure.adapter.out.catbox;

import com.wikicollection.application.exception.CatboxUploadException;
import com.wikicollection.domain.port.out.ImageHostingClient;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component("catboxClient")
public class CatboxClient implements ImageHostingClient {

    private static final String UPLOAD_PATH = "/user/api.php";

    private final RestClient catboxRestClient;
    private final String baseUrl;
    private final String userhash;

    public CatboxClient(
            @Qualifier("catboxRestClient") RestClient catboxRestClient,
            @Value("${catbox.api.base-url:https://catbox.moe}") String baseUrl,
            @Value("${catbox.userhash:}") String userhash) {
        this.catboxRestClient = catboxRestClient;
        this.baseUrl = baseUrl;
        this.userhash = userhash;
    }

    @Override
    public String upload(byte[] content, String filename, String contentType) {
        try {
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("reqtype", "fileupload");
            if (userhash != null && !userhash.isBlank()) {
                form.add("userhash", userhash);
            }
            ByteArrayResource resource = new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            var filePart = new org.springframework.http.HttpEntity<>(resource,
                    fileHeaders(filename, contentType));
            form.add("fileToUpload", filePart);

            String response = catboxRestClient.post()
                    .uri(baseUrl + UPLOAD_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            if (response == null || !response.strip().startsWith("http")) {
                throw new CatboxUploadException("Catbox rechazó la subida: " + response);
            }
            return response.strip();
        } catch (RestClientResponseException | ResourceAccessException e) {
            log.warn("Catbox no disponible: {}", e.getMessage());
            throw new CatboxUploadException("No se pudo subir la imagen a Catbox", e);
        }
    }

    @Override
    public void delete(String filename) {
        if (filename == null || filename.isBlank()
                || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Nombre de archivo inválido: " + filename);
        }
        if (userhash == null || userhash.isBlank()) {
            throw new IllegalArgumentException("Eliminar imágenes requiere configurar catbox.userhash");
        }
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("reqtype", "deletefiles");
            form.add("userhash", userhash);
            form.add("files", filename);
            catboxRestClient.post()
                    .uri(baseUrl + UPLOAD_PATH)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException | ResourceAccessException e) {
            log.warn("Catbox no disponible al eliminar {}: {}", filename, e.getMessage());
            throw new CatboxUploadException("No se pudo eliminar la imagen en Catbox: " + filename, e);
        }
    }

    private org.springframework.http.HttpHeaders fileHeaders(String filename, String contentType) {
        var headers = new org.springframework.http.HttpHeaders();
        headers.setContentDispositionFormData("fileToUpload", filename);
        if (contentType != null && !contentType.isBlank()) {
            headers.setContentType(MediaType.parseMediaType(contentType));
        }
        return headers;
    }
}
