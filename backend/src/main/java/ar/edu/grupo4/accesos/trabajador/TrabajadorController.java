package ar.edu.grupo4.accesos.trabajador;

import ar.edu.grupo4.accesos.common.paging.OrdenPermitido;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.trabajador.dto.DocumentoRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorEditarRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * API REST para la gestión de trabajadores (US-25, US-26, US-28).
 *
 * Seguridad:
 * - Todos los endpoints requieren un JWT válido (SecurityConfig exige token para /api/**).
 * - @PreAuthorize restringe el acceso al rol EMPRESA.
 *   Otros roles (ADMIN, GUARDIA, OPERADOR, MONOTRIBUTISTA) recibirán HTTP 403.
 * - El perfilId NUNCA se recibe del frontend: el Service lo toma del usuario autenticado
 *   (UsuarioActual, T-03), garantizando aislamiento entre empresas (RF-23).
 *
 * Paginación:
 * - Por defecto se muestran 10 trabajadores por página, ordenados alfabéticamente por nombre (A-Z).
 * - El frontend puede cambiar página, tamaño y orden con query params estándar de Spring:
 *   ?page=0&size=10&sort=nombre,asc
 */
@RestController
@RequestMapping("/api/trabajadores")
@PreAuthorize("hasRole('EMPRESA')")
@RequiredArgsConstructor
public class TrabajadorController {

    // Columnas por las que se puede ordenar con ?sort= (OrdenPermitido)
    private static final Set<String> ORDEN_PERMITIDO = Set.of("nombre", "apellido", "dni", "creadoEn");

    private final TrabajadorService trabajadorService;

    // ========================================================================================
    // US-25: Registrar trabajador
    // ========================================================================================

    /**
     * Crea un nuevo trabajador para el perfil del usuario autenticado.
     * En S1 solo se envían nombre, apellido y DNI (sin archivos).
     *
     * @param request datos del trabajador validados por Bean Validation
     * @return el trabajador creado con su estado documental calculado
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201: recurso creado correctamente
    public TrabajadorResponse crear(
            @Valid @RequestBody TrabajadorRequest request) {

        return trabajadorService.crear(request);
    }

    // ========================================================================================
    // US-26: Listar y modificar trabajadores
    // ========================================================================================

    /**
     * Lista paginada de trabajadores del perfil del usuario autenticado.
     * Spring traduce automáticamente los query params (?page, ?size, ?sort) al objeto Pageable.
     *
     * @PageableDefault define los valores si el frontend no envía ninguno:
     *   - size = 10 (10 filas por página, coincide con el wireframe)
     *   - sort = {"nombre", "apellido"} (orden alfabético por defecto, RNF-08)
     */
    @GetMapping
    public PageResponse<TrabajadorResponse> listar(
            @PageableDefault(size = 10, sort = {"nombre", "apellido"}) Pageable pageable) {

        return trabajadorService.listar(OrdenPermitido.validar(pageable, ORDEN_PERMITIDO));
    }

    /**
     * Detalle de un trabajador específico.
     * Si el trabajador no existe o pertenece a otro perfil → 404 (no 403,
     * para no revelar la existencia de trabajadores ajenos).
     */
    @GetMapping("/{id}")
    public TrabajadorResponse obtener(
            @PathVariable Long id) {

        return trabajadorService.obtener(id);
    }

    /**
     * Modifica nombre y/o apellido de un trabajador existente.
     * El DNI no se puede cambiar una vez registrado (viene así del requerimiento).
     * La foto y documentos se modifican aparte (US-28, S2).
     */
    @PutMapping("/{id}")
    public TrabajadorResponse editar(
            @PathVariable Long id,
            @Valid @RequestBody TrabajadorEditarRequest request) {

        return trabajadorService.editar(id, request);
    }

    // ========================================================================================
    // US-27: Dar de baja trabajador
    // ========================================================================================

    /**
     * Da de baja a un trabajador de la empresa (fin de contrato o renuncia, RF-12).
     * Inhabilita únicamente este vínculo en todos los barrios (RN-05).
     */
    @PostMapping("/{id}/baja")
    public TrabajadorResponse darDeBaja(
            @PathVariable Long id) {

        return trabajadorService.darDeBaja(id);
    }

    /**
     * Reincorpora a un trabajador dado de baja en la empresa (RF-12, RN-05).
     * Restablece el vínculo laboral (fecha_baja = null) y su habilitación
     * para ingresar según el estado de su documentación legal.
     */
    @PostMapping("/{id}/reincorporar")
    public TrabajadorResponse reincorporar(
            @PathVariable Long id) {

        return trabajadorService.reincorporar(id);
    }

    // ========================================================================================
    // US-28: Cargar, renovar y eliminar documentación
    // ========================================================================================

    /**
     * Carga o renueva un documento (ART o Seguro de Vida) para un trabajador.
     */
    @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TrabajadorResponse cargarDocumento(
            @PathVariable Long id,
            @Valid @ModelAttribute DocumentoRequest request) {

        return trabajadorService.cargarDocumento(id, request);
    }

    /**
     * Elimina un documento legal de un trabajador.
     */
    @DeleteMapping("/{id}/documentos/{tipo}")
    public TrabajadorResponse eliminarDocumento(
            @PathVariable Long id,
            @PathVariable TipoDocumento tipo) {

        return trabajadorService.eliminarDocumento(id, tipo);
    }

    /**
     * Descarga el archivo PDF de un documento legal de un trabajador.
     */
    @GetMapping("/{id}/documentos/{tipo}/archivo")
    public ResponseEntity<Resource> descargarDocumento(
            @PathVariable Long id,
            @PathVariable TipoDocumento tipo) {

        Resource recurso = trabajadorService.descargarDocumento(id, tipo);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tipo.name().toLowerCase() + ".pdf\"")
            .body(recurso);
    }

    // ========================================================================================
    // US-25 / US-26: Carga, eliminación y descarga de Foto
    // ========================================================================================

    /**
     * Sube o reemplaza la foto de perfil del trabajador.
     */
    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TrabajadorResponse cargarFoto(
            @PathVariable Long id,
            @RequestParam("foto") MultipartFile foto) {

        return trabajadorService.cargarFoto(id, foto);
    }

    /**
     * Elimina la foto de perfil del trabajador.
     */
    @DeleteMapping("/{id}/foto")
    public TrabajadorResponse eliminarFoto(
            @PathVariable Long id) {

        return trabajadorService.eliminarFoto(id);
    }

    /**
     * Descarga la foto de perfil del trabajador.
     */
    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> descargarFoto(
            @PathVariable Long id) {

        Resource recurso = trabajadorService.descargarFoto(id);
        MediaType mediaType = MediaTypeFactory.getMediaType(recurso).orElse(MediaType.IMAGE_JPEG);
        return ResponseEntity.ok()
            .contentType(mediaType)
            .body(recurso);
    }
}
