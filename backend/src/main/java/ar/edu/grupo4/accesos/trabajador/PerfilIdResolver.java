package ar.edu.grupo4.accesos.trabajador;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Puente entre el módulo de trabajadores y la tabla "perfil" (gestionada por Persona 3).
 *
 * ¿Por qué existe este componente?
 * ---------------------------------
 * La entidad Perfil pertenece a otro módulo. Si hiciéramos un @ManyToOne hacia ella,
 * cualquier cambio que Persona 3 haga en su entidad nos rompería a nosotros.
 * En cambio, esta clase hace una query nativa directa a la tabla y nos devuelve solo
 * el Long que necesitamos (perfilId). Así el acoplamiento entre módulos es mínimo.
 *
 * ¿Qué valida?
 * -------------
 * - Que el usuario tenga un perfil con estado APROBADO (RN-14).
 * - Si el perfil no existe o no está aprobado, se lanza una excepción de negocio
 *   con HTTP 403, porque el problema no es que falten datos (400) ni que no se
 *   encuentre un recurso (404), sino que el usuario no tiene permiso operativo.
 *
 * Nota para la integración:
 * Cuando Persona 3 termine su entidad Perfil y su PR sea mergeado, se reemplazará
 * esta query nativa por una llamada al servicio correspondiente de perfil.
 */
@Component
@RequiredArgsConstructor
public class PerfilIdResolver {

    private final EntityManager entityManager;

    /**
     * Resuelve el perfilId aprobado del usuario autenticado.
     *
     * @param usuarioId el ID extraído del claim "sub" del JWT
     * @return el ID del perfil con estado APROBADO
     * @throws BusinessException si no existe un perfil aprobado para ese usuario
     */
    public Long resolver(Long usuarioId) {
        try {
            // Query nativa: evita depender de una entidad Perfil que no es nuestra.
            // La tabla y columnas están definidas en V202610211000__modelo_inicial.sql.
            return (Long) entityManager.createNativeQuery(
                    "SELECT id FROM perfil WHERE usuario_id = :usuarioId AND estado = 'APROBADO'")
                .setParameter("usuarioId", usuarioId)
                .getSingleResult();
        } catch (NoResultException e) {
            // El usuario no tiene perfil aprobado (PENDIENTE, RECHAZADO, SUSPENDIDO).
            // La aprobación de perfiles corresponde al Administrador de la plataforma (RF-04).
            throw new BusinessException(
                "PERFIL_NO_APROBADO",
                "Tu perfil no está aprobado. Contactá al Administrador de la plataforma.",
                HttpStatus.FORBIDDEN
            );
        }
    }
}
