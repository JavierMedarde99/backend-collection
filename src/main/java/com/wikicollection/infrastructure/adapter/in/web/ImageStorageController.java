package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;

import com.wikicollection.application.service.ImageStorageService;
import com.wikicollection.infrastructure.adapter.in.web.dto.ImageResponse;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/images")
@Validated
@Tag(name = "Imágenes", description = "Subida y gestión de imágenes de la colección")
public class ImageStorageController {

    private final ImageStorageService imageStorageService;

    public ImageStorageController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Sube una imagen", description = "Recibe un archivo multipart, lo valida (5MB, JPEG/PNG/WebP/GIF) y devuelve su URL pública.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Imagen subida"),
            @ApiResponse(responseCode = "400", description = "Archivo vacío, tipo no permitido o tamaño excedido")
    })
    public ResponseEntity<ImageResponse> upload(
            @Parameter(description = "Archivo de imagen") @RequestParam("file") MultipartFile file,
            UriComponentsBuilder ucb) {
        String filename = imageStorageService.store(file);
        URI location = ucb.path("/api/v1/images/{filename}").buildAndExpand(filename).toUri();
        return ResponseEntity.created(location)
                .body(new ImageResponse(location.toString(), filename));
    }

    @DeleteMapping("/{filename}")
    @Operation(summary = "Elimina una imagen subida")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Imagen eliminada"),
            @ApiResponse(responseCode = "400", description = "Nombre de archivo inválido"),
            @ApiResponse(responseCode = "404", description = "Imagen no encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Nombre del archivo") @PathVariable String filename) {
        imageStorageService.delete(filename);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{filename}")
    @Operation(summary = "Sirve una imagen subida")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contenido de la imagen"),
            @ApiResponse(responseCode = "400", description = "Nombre de archivo inválido"),
            @ApiResponse(responseCode = "404", description = "Imagen no encontrada")
    })
    public ResponseEntity<Resource> serve(
            @Parameter(description = "Nombre del archivo") @PathVariable String filename) {
        Resource resource = imageStorageService.load(filename);
        MediaType contentType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, java.util.concurrent.TimeUnit.DAYS).cachePublic())
                .contentType(contentType)
                .body(resource);
    }
}
