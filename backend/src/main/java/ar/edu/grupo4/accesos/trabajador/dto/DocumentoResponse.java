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
    /**
     * archivoNombre es el nombre con el que se subió. Los documentos sin ese dato (seed, cargados antes)
     * muestran uno por tipo; la ruta en disco (UUID) nunca sale del backend.
     */
    public static DocumentoResponse from(Documento d) {
        String nombre = d.getArchivoNombre();
        if (nombre == null) {
            nombre = d.getTipo() == TipoDocumento.ART ? "ART.pdf" : "Seguro de Vida.pdf";
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
