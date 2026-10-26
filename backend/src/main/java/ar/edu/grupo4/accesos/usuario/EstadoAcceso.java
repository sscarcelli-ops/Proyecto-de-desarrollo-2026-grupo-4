package ar.edu.grupo4.accesos.usuario;

/**
 * Si una cuenta puede operar según el estado de su perfil, su Guardia y su barrio (US-04 CA-3, US-43).
 * Un usuario sin rol aprobado (sin perfil, pendiente o rechazado) está HABILITADO: entra con roles vacíos (RN-12).
 */
public enum EstadoAcceso {
    HABILITADO,
    /** Perfil de Empresa o Monotributista suspendido (RF-06) */
    SUSPENDIDO,
    /** Guardia deshabilitado por su Operador (RN-09) */
    DESHABILITADO,
    /** Guardia u Operador de un barrio Inactivo (RN-13, RN-15) */
    BARRIO_INACTIVO,
    /** Operador sin barrio asignado ("Disponible", RF-03, RF-31) (RN-15) */
    OPERADOR_SIN_BARRIO
}
