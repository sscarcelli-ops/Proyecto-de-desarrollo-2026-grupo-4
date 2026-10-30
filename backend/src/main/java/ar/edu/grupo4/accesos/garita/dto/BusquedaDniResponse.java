package ar.edu.grupo4.accesos.garita.dto;

import java.util.List;

/**
 * Resultado completo de la búsqueda por DNI (US-37).
 *
 * @param nombre          nombre del trabajador (todos los vínculos comparten nombre/apellido/dni)
 * @param apellido        apellido del trabajador
 * @param dni             DNI buscado
 * @param tieneFoto       true si al menos el primer vínculo tiene foto cargada
 * @param vinculosBusqueda lista de vínculos laborales encontrados para ese DNI
 * @param ingresoActivo   ingreso activo detectado (puede ser null)
 */
public record BusquedaDniResponse(
        String nombre,
        String apellido,
        String dni,
        boolean tieneFoto,
        List<VinculoResultado> vinculosBusqueda,
        IngresoActivoInfo ingresoActivo
) {}
