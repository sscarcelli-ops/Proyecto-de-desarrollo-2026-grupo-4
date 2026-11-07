package ar.edu.grupo4.accesos.ubicacion.dto;

import ar.edu.grupo4.accesos.ubicacion.Localidad;

/** Localidad para el selector. El departamento se muestra cuando el nombre se repite en la provincia. */
public record LocalidadResponse(String id, String nombre, String departamento) {

    public static LocalidadResponse from(Localidad l) {
        return new LocalidadResponse(l.getId(), l.getNombre(), l.getDepartamento());
    }
}
