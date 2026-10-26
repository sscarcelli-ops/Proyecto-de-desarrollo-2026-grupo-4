package ar.edu.grupo4.accesos.usuario;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // El email es único sin distinguir mayúsculas (índice lower(email)). Trae los roles en la misma consulta.
    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findWithRolesById(Long id);

    /**
     * Estado de acceso de una cuenta (nombre de EstadoAcceso), en una sola consulta de solo lectura sobre las
     * tablas de perfil, guardia y barrio: así no depende de las entidades de esos módulos.
     * Prioridad si se cumple más de uno: suspendido, Guardia deshabilitado, barrio inactivo, Operador sin barrio.
     */
    @Query(nativeQuery = true, value = """
        SELECT CASE
            WHEN EXISTS (SELECT 1 FROM perfil p
                         WHERE p.usuario_id = :usuarioId AND p.estado = 'SUSPENDIDO') THEN 'SUSPENDIDO'
            WHEN EXISTS (SELECT 1 FROM guardia g
                         WHERE g.usuario_id = :usuarioId AND NOT g.habilitado) THEN 'DESHABILITADO'
            WHEN EXISTS (SELECT 1 FROM guardia g JOIN barrio b ON b.id = g.barrio_id
                         WHERE g.usuario_id = :usuarioId AND b.estado = 'INACTIVO')
              OR EXISTS (SELECT 1 FROM barrio b
                         WHERE b.operador_id = :usuarioId AND b.estado = 'INACTIVO') THEN 'BARRIO_INACTIVO'
            WHEN EXISTS (SELECT 1 FROM operador o
                         WHERE o.usuario_id = :usuarioId
                           AND NOT EXISTS (SELECT 1 FROM barrio b WHERE b.operador_id = o.usuario_id))
                THEN 'OPERADOR_SIN_BARRIO'
            ELSE 'HABILITADO'
        END
        """)
    String estadoDeAcceso(@Param("usuarioId") Long usuarioId);

    /**
     * Nombre para mostrar (ej. "Guardia: Carlos Gómez"): nombre y apellido del Guardia u Operador, razón social
     * de la Empresa o nombre del Monotributista. NULL si la cuenta no tiene esos datos (Administrador, sin perfil).
     */
    @Query(nativeQuery = true, value = """
        SELECT COALESCE(
            (SELECT g.nombre || ' ' || g.apellido FROM guardia g WHERE g.usuario_id = :usuarioId),
            (SELECT o.nombre || ' ' || o.apellido FROM operador o WHERE o.usuario_id = :usuarioId),
            (SELECT COALESCE(p.razon_social, p.nombre || ' ' || p.apellido) FROM perfil p WHERE p.usuario_id = :usuarioId)
        )
        """)
    String nombreVisible(@Param("usuarioId") Long usuarioId);
}
