package ar.edu.grupo4.accesos.usuario;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Si una cuenta puede operar según el estado de su perfil, su Guardia y su barrio (US-04 CA-3, US-42 CA-4, US-43).
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
    OPERADOR_SIN_BARRIO;

    /**
     * 403 con el mensaje de la US si la cuenta no puede operar. Lo usan el login y el chequeo de cada petición,
     * así el usuario ve el mismo mensaje en los dos casos.
     */
    public void exigirHabilitado() {
        switch (this) {
            case SUSPENDIDO -> throw new BusinessException(
                "CUENTA_SUSPENDIDA", "Tu cuenta se encuentra suspendida", HttpStatus.FORBIDDEN);
            case DESHABILITADO -> throw new BusinessException(
                "USUARIO_DESHABILITADO", "Tu usuario se encuentra deshabilitado", HttpStatus.FORBIDDEN);
            case BARRIO_INACTIVO -> throw new BusinessException(
                "BARRIO_INACTIVO", "Tu barrio se encuentra inactivo", HttpStatus.FORBIDDEN);
            case OPERADOR_SIN_BARRIO -> throw new BusinessException(
                "OPERADOR_SIN_BARRIO", "No tenés un barrio asignado. Contactá al Administrador.", HttpStatus.FORBIDDEN);
            case HABILITADO -> {
                // Puede operar. Sin rol aprobado (pendiente o rechazado) también: cada función lo controla aparte
            }
        }
    }
}
