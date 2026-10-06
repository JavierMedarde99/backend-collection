package com.wikicollection.infrastructure.adapter.in.web;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import com.wikicollection.domain.model.Deck;
import com.wikicollection.domain.model.DeckImportFormat;
import com.wikicollection.domain.model.DeckImportStatus;
import com.wikicollection.domain.model.DeckImportMode;
import com.wikicollection.domain.port.in.DeckImportUseCase;
import com.wikicollection.domain.port.in.DeckUseCase;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckCardRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckImportAcceptedResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckImportDtoMapper;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckImportJobResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckRequest;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckResponse;
import com.wikicollection.infrastructure.adapter.in.web.dto.DeckStatusResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.wikicollection.infrastructure.config.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.validation.Valid;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/decks")
@Validated
@Tag(name = "Mazos Commander", description = "Gestión de mazos Commander de Magic: The Gathering")
public class DeckController {

    private final DeckUseCase deckUseCase;
    private final DeckDtoMapper mapper;
    private final DeckImportUseCase deckImportUseCase;
    private final DeckImportDtoMapper importMapper;

    public DeckController(DeckUseCase deckUseCase, DeckDtoMapper mapper,
                          DeckImportUseCase deckImportUseCase, DeckImportDtoMapper importMapper) {
        this.deckUseCase = deckUseCase;
        this.mapper = mapper;
        this.deckImportUseCase = deckImportUseCase;
        this.importMapper = importMapper;
    }

    @GetMapping
    @Operation(summary = "Lista mazos", description = "Devuelve una página de mazos, opcionalmente filtrada por nombre exacto.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de mazos"),
            @ApiResponse(responseCode = "400", description = "Parámetros de paginación inválidos")
    })
    public Page<DeckResponse> list(
            @Parameter(description = "Número de página (base 0)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Ordenación como campo,asc|desc") @RequestParam(defaultValue = "name,asc") String sort,
            @Parameter(description = "Filtro por nombre exacto") @RequestParam(required = false) @Size(max = 100, message = "La búsqueda no puede superar los 100 caracteres") String name,
            @Parameter(description = "Filtro por propiedad: mine|other|all") @RequestParam(defaultValue = "mine") String owner,
            @CurrentUser String viewerId) {
        Pageable pageable = PageRequest.of(page, size, buildSort(sort));
        var decks = (name == null || name.isBlank())
                ? deckUseCase.findAll(pageable, owner, viewerId)
                : deckUseCase.findByName(name, pageable, owner, viewerId);
        return decks.map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un mazo por su id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mazo encontrado"),
            @ApiResponse(responseCode = "404", description = "Mazo no encontrado")
    })
    public DeckResponse getById(
            @Parameter(description = "Identificador del mazo") @PathVariable String id) {
        return mapper.toResponse(deckUseCase.findById(id));
    }

    @PostMapping
    @Operation(summary = "Crea un mazo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Mazo creado"),
            @ApiResponse(responseCode = "400", description = "Datos del mazo inválidos")
    })
    public ResponseEntity<DeckResponse> create(@Valid @RequestBody DeckRequest request, @CurrentUser String currentUserId, UriComponentsBuilder ucb) {
        var saved = deckUseCase.save(mapper.toDomain(request), currentUserId);
        URI location = ucb.path("/api/v1/decks/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un mazo existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mazo actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos del mazo inválidos"),
            @ApiResponse(responseCode = "404", description = "Mazo no encontrado")
    })
    public DeckResponse update(
            @Parameter(description = "Identificador del mazo") @PathVariable String id,
            @Valid @RequestBody DeckRequest request, @CurrentUser String currentUserId) {
        return mapper.toResponse(deckUseCase.update(id, mapper.toDomain(request), currentUserId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina un mazo")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mazo eliminado"),
            @ApiResponse(responseCode = "404", description = "Mazo no encontrado")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador del mazo") @PathVariable String id, @CurrentUser String currentUserId) {
        deckUseCase.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cards")
    @Operation(summary = "Añade una carta al mazo desde Scryfall")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carta añadida"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Mazo o carta no encontrados")
    })
    public DeckResponse addCard(
            @Parameter(description = "Identificador del mazo") @PathVariable String id,
            @Valid @RequestBody DeckCardRequest request, @CurrentUser String currentUserId) {
        return mapper.toResponse(deckUseCase.addCard(id, request.scryfallId(), request.quantity(), currentUserId));
    }

    @DeleteMapping("/{id}/cards/{scryfallId}")
    @Operation(summary = "Quita una carta del mazo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Carta eliminada del mazo"),
            @ApiResponse(responseCode = "400", description = "La carta no está en el mazo"),
            @ApiResponse(responseCode = "404", description = "Mazo no encontrado")
    })
    public DeckResponse removeCard(
            @Parameter(description = "Identificador del mazo") @PathVariable String id,
            @Parameter(description = "Identificador Scryfall de la carta") @PathVariable String scryfallId,
            @CurrentUser String currentUserId) {
        return mapper.toResponse(deckUseCase.removeCard(id, scryfallId, currentUserId));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Estado del mazo según reglas Commander")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado calculado"),
            @ApiResponse(responseCode = "404", description = "Mazo no encontrado")
    })
    public DeckStatusResponse status(
            @Parameter(description = "Identificador del mazo") @PathVariable String id) {
        return mapper.toStatusResponse(deckUseCase.getStatusReport(id));
    }

    // ------------------------------------------------------- importación (#353)

    /**
     * Importa una lista de mazo a este mazo. Responde 202 con la URL del trabajo: resolver
     * 99 nombres contra Scryfall tarda, y hacerlos en la petición dejaría al usuario mirando
     * una pantalla en blanco sin poder hacer nada más.
     */
    @PostMapping(value = "/{id}/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importa una lista de mazo",
            description = "Acepta el archivo y responde 202. La importación ocurre en segundo plano: "
                    + "consulta el estado con GET /{id}/imports/{jobId}.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Importación encolada"),
            @ApiResponse(responseCode = "400", description = "Archivo vacío, demasiado grande o formato inconsistente"),
            @ApiResponse(responseCode = "403", description = "El mazo no es del usuario"),
            @ApiResponse(responseCode = "404", description = "Mazo no encontrado"),
            @ApiResponse(responseCode = "429", description = "Demasiadas importaciones en cola")
    })
    public ResponseEntity<DeckImportAcceptedResponse> importDeck(
            @Parameter(description = "Id del mazo") @PathVariable String id,
            @Parameter(description = "Archivo de lista de mazo") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Formato declarado: TXT, JSON o CSV; si se omite se deduce") @RequestParam(required = false) String format,
            @Parameter(description = "Qué hacer con las cartas ya guardadas: replace o merge") @RequestParam(defaultValue = "replace") String mode,
            @CurrentUser String currentUserId,
            UriComponentsBuilder ucb) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return acceptImport(id, content, format, mode, currentUserId, ucb);
    }

    /** Igual que {@link #importDeck}, pero con el contenido pegado en vez de subido. */
    @PostMapping(value = "/{id}/imports/text", consumes = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Importa una lista de mazo desde texto plano",
            description = "Variante de POST /{id}/imports con la lista en el cuerpo, para clientes "
                    + "que ya la tienen en memoria.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Importación encolada"),
            @ApiResponse(responseCode = "400", description = "Contenido vacío o formato inconsistente"),
            @ApiResponse(responseCode = "403", description = "El mazo no es del usuario"),
            @ApiResponse(responseCode = "404", description = "Mazo no encontrado"),
            @ApiResponse(responseCode = "429", description = "Demasiadas importaciones en cola")
    })
    public ResponseEntity<DeckImportAcceptedResponse> importDeckText(
            @Parameter(description = "Id del mazo") @PathVariable String id,
            @Parameter(description = "Contenido de la lista") @RequestBody String content,
            @Parameter(description = "Formato declarado: TXT, JSON o CSV; si se omite se deduce") @RequestParam(required = false) String format,
            @Parameter(description = "Qué hacer con las cartas ya guardadas: replace o merge") @RequestParam(defaultValue = "replace") String mode,
            @CurrentUser String currentUserId,
            UriComponentsBuilder ucb) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        return acceptImport(id, content, format, mode, currentUserId, ucb);
    }

    /**
     * Estado del trabajo. El mazo solo viene cuando la importación ha terminado: mientras
     * corre, lo que hay en Mongo es el mazo de antes, y devolverlo haría que el frontend
     * pintara un mazo a medias como si fuera el resultado.
     */
    @GetMapping("/{id}/imports/{jobId}")
    @Operation(summary = "Estado de una importación",
            description = "Devuelve el avance del trabajo y, si ya terminó, el mazo guardado, las "
                    + "cartas que no se pudieron resolver y el resultado de validarlo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado del trabajo"),
            @ApiResponse(responseCode = "403", description = "El mazo no es del usuario"),
            @ApiResponse(responseCode = "404", description = "Mazo o trabajo no encontrado")
    })
    public DeckImportJobResponse getImport(
            @Parameter(description = "Id del mazo") @PathVariable String id,
            @Parameter(description = "Id del trabajo") @PathVariable String jobId,
            @CurrentUser String currentUserId) {
        var job = deckImportUseCase.findJob(id, jobId, currentUserId);
        Deck deck = job.status() == DeckImportStatus.COMPLETED ? deckUseCase.findById(id) : null;
        return importMapper.toJobResponse(job, deck, mapper);
    }

    private ResponseEntity<DeckImportAcceptedResponse> acceptImport(
            String id, String content, String format, String mode, String currentUserId, UriComponentsBuilder ucb) {
        DeckImportFormat declared = format == null || format.isBlank() ? null : parseFormat(format);
        DeckImportMode parsedMode = parseMode(mode);
        var job = deckImportUseCase.startImport(id, content, declared, parsedMode, currentUserId);
        String statusUrl = "/api/v1/decks/" + id + "/imports/" + job.jobId();
        URI location = ucb.path(statusUrl).build().toUri();
        return ResponseEntity.accepted().location(location).body(importMapper.toAccepted(job, statusUrl));
    }

    private DeckImportFormat parseFormat(String value) {
        try {
            return DeckImportFormat.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Formato desconocido: " + value
                    + ". Los válidos son TXT, JSON y CSV.");
        }
    }

    private DeckImportMode parseMode(String value) {
        try {
            return DeckImportMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Modo desconocido: " + value
                    + ". Los válidos son REPLACE y MERGE.");
        }
    }

    private Sort buildSort(String sort) {
        String field = "name";
        Sort.Direction direction = Sort.Direction.ASC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length > 0 && !parts[0].isBlank()) {
                field = parts[0].trim();
            }
            if (parts.length > 1 && !parts[1].isBlank()) {
                direction = "asc".equalsIgnoreCase(parts[1].trim())
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;
            }
        }
        return Sort.by(direction, field);
    }
}
