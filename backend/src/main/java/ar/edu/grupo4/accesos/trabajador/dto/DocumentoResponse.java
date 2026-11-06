package ar.edu.grupo4.accesos.trabajador.dto;

import ar.edu.grupo4.accesos.trabajador.Documento;
import ar.edu.grupo4.accesos.trabajador.TipoDocumento;

import java.time.LocalDate;

/**
 * Vista de un documento de un trabajador (US-28).
 */
public record DocumentoResponse(
    Long id,
    TipoDocumento tipo,
    String archivoNombre,
    LocalDate fechaVencimiento,
    boolean vencido
) {
    public static DocumentoResponse from(Documento d) {
        String nombre = d.getArchivoRuta();
        if (nombre != null && nombre.contains("/")) {
            nombre = nombre.substring(nombre.lastIndexOf('/') + 1);
        }
        return new DocumentoResponse(
            d.getId(),
            d.getTipo(),
            nombre,
            d.getFechaVencimiento(),
            d.estaVencido()
        );
    }
}
