package ar.edu.grupo4.accesos.trabajador;

import ar.edu.grupo4.accesos.perfil.PerfilService;
import ar.edu.grupo4.accesos.trabajador.dto.DocumentoRequest;
import ar.edu.grupo4.accesos.trabajador.dto.MonotributistaPerfilModificarRequest;
import ar.edu.grupo4.accesos.trabajador.dto.MonotributistaPerfilResponse;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorResponse;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints para que el Monotributista gestione su propio perfil, foto y documentación (US-24).
 *
 * Seguridad:
 * - Requiere rol MONOTRIBUTISTA y perfil APROBADO (verificado vía UsuarioActual.perfilIdAprobado()).
 * - Si el perfil está pendiente o rechazado, devuelve 403 Forbidden (CA-4).
 * - No permite gestionar trabajadores de terceros ni agregar trabajadores adicionales (CA-3).
 */
@RestController
@RequestMapping("/api/monotributista/perfil")
@PreAuthorize("hasRole('MONOTRIBUTISTA')")
@RequiredArgsConstructor
@Tag(name = "Monotributista - Perfil", description = "Gestión del perfil propio y documentación del Monotributista (US-24)")
public class MonotributistaPerfilController {

    private final PerfilService perfilService;
    private final TrabajadorService trabajadorService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    @Operation(summary = "Obtener perfil propio del Monotributista")
    public MonotributistaPerfilResponse obtener() {
        Long perfilId = usuarioActual.perfilIdAprobado();
        return perfilService.obtenerPerfilMonotributista(perfilId);
    }

    @PutMapping
    @Operation(summary = "Modificar nombre, apellido y teléfono del Monotributista")
    public MonotributistaPerfilResponse modificar(@Valid @RequestBody MonotributistaPerfilModificarRequest request) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        return perfilService.modificarPerfilMonotributista(perfilId, request);
    }

    @PostMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cargar o reemplazar foto de perfil")
    public TrabajadorResponse cargarFoto(@RequestParam("foto") MultipartFile foto) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = trabajadorService.obtenerEntidadPorPerfilId(perfilId);
        return trabajadorService.cargarFoto(trabajador.getId(), foto);
    }

    @DeleteMapping("/foto")
    @Operation(summary = "Eliminar foto de perfil")
    public TrabajadorResponse eliminarFoto() {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = trabajadorService.obtenerEntidadPorPerfilId(perfilId);
        return trabajadorService.eliminarFoto(trabajador.getId());
    }

    @GetMapping("/foto")
    @Operation(summary = "Descargar o visualizar foto de perfil")
    public ResponseEntity<Resource> descargarFoto() {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = trabajadorService.obtenerEntidadPorPerfilId(perfilId);
        Resource recurso = trabajadorService.descargarFoto(trabajador.getId());
        String contentType = MediaTypeFactory.getMediaType(recurso)
            .map(MediaType::toString)
            .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .body(recurso);
    }

    @PostMapping(value = "/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cargar o renovar póliza de ART o Seguro de Vida")
    public TrabajadorResponse cargarDocumento(@Valid @ModelAttribute DocumentoRequest request) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = trabajadorService.obtenerEntidadPorPerfilId(perfilId);
        return trabajadorService.cargarDocumento(trabajador.getId(), request);
    }

    @DeleteMapping("/documentos/{tipo}")
    @Operation(summary = "Eliminar póliza de ART o Seguro de Vida")
    public TrabajadorResponse eliminarDocumento(@PathVariable TipoDocumento tipo) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = trabajadorService.obtenerEntidadPorPerfilId(perfilId);
        return trabajadorService.eliminarDocumento(trabajador.getId(), tipo);
    }

    @GetMapping("/documentos/{tipo}")
    @Operation(summary = "Descargar PDF de póliza de ART o Seguro de Vida")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable TipoDocumento tipo) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = trabajadorService.obtenerEntidadPorPerfilId(perfilId);
        Resource recurso = trabajadorService.descargarDocumento(trabajador.getId(), tipo);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tipo.name() + ".pdf\"")
            .body(recurso);
    }
}
