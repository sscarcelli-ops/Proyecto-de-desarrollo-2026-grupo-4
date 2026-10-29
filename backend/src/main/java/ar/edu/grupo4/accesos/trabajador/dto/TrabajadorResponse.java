package ar.edu.grupo4.accesos.trabajador.dto;

import ar.edu.grupo4.accesos.trabajador.Trabajador;

/**
 * Vista de un trabajador en listados y detalle (US-25, US-26).
 *
 * El estadoDocumental es un String calculado por el Service a partir de los
 * documentos del trabajador y sus fechas de vencimiento (RNF-04). Valores
 * posibles: "VIGENTE", "VENCIDO", "PENDIENTE_CARGA". En el frontend, el
 * componente EstadoTag traduce estos códigos al texto que se muestra al usuario.
 *
 * tieneFoto y dadoDeBaja son booleanos de conveniencia: el frontend los usa
 * para mostrar advertencias (sin foto → pedir DNI en garita) y para deshabilitar
 * acciones sobre trabajadores dados de baja.
 */
public record TrabajadorResponse(
    Long id,
    String nombre,
    String apellido,
    String dni,
    String estadoDocumental,
    boolean tieneFoto,
    boolean dadoDeBaja
) {
    /**
     * Convierte una entidad a su DTO de respuesta. El estadoDocumental se recibe
     * como parámetro porque su cálculo es lógica de negocio (vive en el Service,
     * no en el DTO).
     */
    public static TrabajadorResponse from(Trabajador t, String estadoDocumental) {
        return new TrabajadorResponse(
            t.getId(),
            t.getNombre(),
            t.getApellido(),
            t.getDni(),
            estadoDocumental,
            t.tieneFoto(),
            t.estaDadoDeBaja()
        );
    }
}
