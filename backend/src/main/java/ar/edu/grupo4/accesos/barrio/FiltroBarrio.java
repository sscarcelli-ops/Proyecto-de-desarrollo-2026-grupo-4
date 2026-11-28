package ar.edu.grupo4.accesos.barrio;

/**
 * Selector de estado del listado de barrios (RF-34): Activos, Inactivos o Sin Operador. Sin filtro, todos.
 * Es un enum para que un valor escrito a mano (?filtro=XYZ) responda 400 en lugar de llegar a la consulta.
 */
public enum FiltroBarrio {
    ACTIVO,
    INACTIVO,
    SIN_OPERADOR
}
