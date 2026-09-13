package com.wikicollection.infrastructure.adapter.in.web;

import java.net.URI;

import com.wikicollection.application.service.ImageStorageService;
import com.wikicollection.infrastructure.adapter.in.web.dto.ImageResponse;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
    @Operation(summary = "Sube una imagen", description = "Recibe un archivo multipart, lo valida (5MB, JPEG/PNG/WebP/GIF) y lo sube a Catbox, devolviendo su URL pública.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Imagen subida"),
            @ApiResponse(responseCode = "400", description = "Archivo vacío, tipo no permitido o tamaño excedido"),
            @ApiResponse(responseCode = "502", description = "El servicio de imágenes devolvió un error")
    })
    public ResponseEntity<ImageResponse> upload(
            @Parameter(description = "Archivo de imagen") @RequestParam("file") MultipartFile file) {
        String url = imageStorageService.store(file);
        String filename = url.substring(url.lastIndexOf('/') + 1);
        return ResponseEntity.created(java.net.URI.create(url))
                .body(new ImageResponse(url, filename));
    }

    @DeleteMapping("/{filename}")
    @Operation(summary = "Elimina una imagen subida", description = "Requiere catbox.userhash configurado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Imagen eliminada"),
            @ApiResponse(responseCode = "400", description = "Nombre de archivo inválido o borrado no disponible"),
            @ApiResponse(responseCode = "404", description = "Imagen no encontrada")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Nombre del archivo") @PathVariable String filename) {
        imageStorageService.delete(filename);
        return ResponseEntity.noContent().build();
    }
}
