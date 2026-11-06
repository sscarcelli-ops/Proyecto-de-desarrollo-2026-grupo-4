package ar.edu.grupo4.accesos.trabajador.dto;

import ar.edu.grupo4.accesos.trabajador.Trabajador;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collections;
import java.util.List;

/**
 * Vista de un trabajador en listados y detalle (US-25, US-26, US-28).
 *
 * El estadoDocumental es un String calculado por el Service a partir de los
 * documentos del trabajador y sus fechas de vencimiento (RNF-04). Valores
 * posibles: "VIGENTE", "VENCIDO", "PENDIENTE_CARGA". En el frontend, el
 * componente EstadoTag traduce estos códigos al texto que se muestra al usuario.
 *
 * tieneFoto y dadoDeBaja son booleanos de conveniencia: el frontend los usa
 * para mostrar advertencias (sin foto → pedir DNI en garita) y para deshabilitar
 * acciones sobre trabajadores dados de baja.
 *
 * documentos lista la documentación adjunta (ART / Seguro de Vida) con sus vencimientos.
 * fotoUrl contiene la ruta relativa del endpoint para descargar la foto si la tiene, o null.
 */
public record TrabajadorResponse(
    Long id,
    String nombre,
    String apellido,
    String dni,
    String estadoDocumental,
    boolean tieneFoto,
    boolean dadoDeBaja,
    List<DocumentoResponse> documentos,
    @Schema(types = {"string", "null"})
    String fotoUrl
) {
    /**
     * Convierte una entidad a su DTO de respuesta. El estadoDocumental se recibe
     * como parámetro porque su cálculo es lógica de negocio (vive en el Service,
     * no en el DTO).
     */
    public static TrabajadorResponse from(Trabajador t, String estadoDocumental) {
        List<DocumentoResponse> docs = (t.getDocumentos() != null)
            ? t.getDocumentos().stream().map(DocumentoResponse::from).toList()
            : Collections.emptyList();

        String fotoUrl = t.tieneFoto() ? "/api/trabajadores/" + t.getId() + "/foto" : null;

        return new TrabajadorResponse(
            t.getId(),
            t.getNombre(),
            t.getApellido(),
            t.getDni(),
            estadoDocumental,
            t.tieneFoto(),
            t.estaDadoDeBaja(),
            docs,
            fotoUrl
        );
    }
}
