package ar.edu.grupo4.accesos.barrio.dto;

import ar.edu.grupo4.accesos.barrio.Barrio;

/** Barrio de la sesión (Operador o Guardia), para el encabezado "Nombre del Barrio" de sus pantallas. */
public record MiBarrioResponse(Long id, String nombre) {
    public static MiBarrioResponse from(Barrio b) {
        return new MiBarrioResponse(b.getId(), b.getNombre());
    }
}
