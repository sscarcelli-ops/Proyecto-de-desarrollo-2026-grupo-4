package ar.edu.grupo4.accesos.ubicacion.dto;

import ar.edu.grupo4.accesos.ubicacion.Provincia;

public record ProvinciaResponse(String id, String nombre) {

    public static ProvinciaResponse from(Provincia p) {
        return new ProvinciaResponse(p.getId(), p.getNombre());
    }
}
