package ar.edu.grupo4.accesos.barrio;

/**
 * US-15: estado de un Operador en el listado del Administrador (RF-31). No se guarda: se deduce de si tiene barrio
 * a cargo (RN-10). Disponible = sin barrio; Ocupado = con barrio.
 */
public enum EstadoOperador {
    DISPONIBLE,
    OCUPADO
}
