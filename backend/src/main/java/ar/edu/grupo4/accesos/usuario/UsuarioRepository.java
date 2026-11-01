package ar.edu.grupo4.accesos.usuario;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // El email es único sin distinguir mayúsculas (índice lower(email)). Trae los roles en la misma consulta.
    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Si alguna cuenta tiene ese rol (ej. "ADMIN"). */
    boolean existsByRoles_Codigo(String codigoRol);

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findWithRolesById(Long id);

    /**
     * Estado de acceso de una cuenta (nombre de EstadoAcceso), sobre las tablas de perfil, guardia y barrio: así no
     * depende de las entidades de esos módulos.
     * Prioridad si se cumple más de uno: suspendido, Guardia deshabilitado, barrio inactivo, Operador sin barrio.
     */
    String ESTADO_DE_ACCESO = """
        CASE
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
        """;

    @Query(nativeQuery = true, value = "SELECT " + ESTADO_DE_ACCESO)
    String estadoDeAcceso(@Param("usuarioId") Long usuarioId);

    /**
     * Lo que cada petición necesita saber del usuario del token, en una sola consulta (T-03): la versión de sesión,
     * su estado de acceso, su barrio (el del Guardia o el que tiene a cargo el Operador) y su perfil aprobado.
     * Vacío si la cuenta ya no existe.
     */
    @Query(nativeQuery = true, value = """
        SELECT u.version_sesion AS versionSesion,
               """ + ESTADO_DE_ACCESO + """
                AS estado,
               coalesce((SELECT g.barrio_id FROM guardia g WHERE g.usuario_id = u.id),
                        (SELECT b.id FROM barrio b WHERE b.operador_id = u.id)) AS barrioId,
               (SELECT p.id FROM perfil p WHERE p.usuario_id = u.id AND p.estado = 'APROBADO') AS perfilAprobadoId
        FROM usuario u
        WHERE u.id = :usuarioId
        """)
    Optional<DatosDeSesion> datosDeSesion(@Param("usuarioId") Long usuarioId);

    interface DatosDeSesion {
        int getVersionSesion();

        String getEstado();

        Long getBarrioId();

        Long getPerfilAprobadoId();
    }

    /** Cierra todas las sesiones del usuario: los tokens emitidos con la versión anterior dejan de valer. */
    @Modifying
    @Query(nativeQuery = true, value = "UPDATE usuario SET version_sesion = version_sesion + 1 WHERE id = :usuarioId")
    void incrementarVersionSesion(@Param("usuarioId") Long usuarioId);

    /** Nombre para mostrar en el encabezado. null para Administrador o una cuenta que todavía no cargó el perfil. */
    @Query(nativeQuery = true, value = """
        SELECT coalesce(
            (SELECT p.razon_social FROM perfil p WHERE p.usuario_id = :usuarioId AND p.tipo = 'EMPRESA'),
            (SELECT p.nombre || ' ' || p.apellido FROM perfil p WHERE p.usuario_id = :usuarioId AND p.tipo = 'MONOTRIBUTISTA'),
            (SELECT g.nombre || ' ' || g.apellido FROM guardia g WHERE g.usuario_id = :usuarioId),
            (SELECT o.nombre || ' ' || o.apellido FROM operador o WHERE o.usuario_id = :usuarioId)
        )
        """)
    String nombreVisible(@Param("usuarioId") Long usuarioId);
}
