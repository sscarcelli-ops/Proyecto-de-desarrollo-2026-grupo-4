package ar.edu.grupo4.accesos.barrio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OperadorRepository extends JpaRepository<Operador, Long> {

    /** Operador con su email (está en usuario), para las listas del Administrador. */
    interface OperadorConEmail {
        Long getId();

        String getNombre();

        String getApellido();

        String getEmail();

        String getTelefono();
    }

    /** US-45: Operadores disponibles, o sea los que no tienen ningún barrio a cargo (RF-31). */
    @Query(nativeQuery = true, value = """
        SELECT o.usuario_id AS id, o.nombre AS nombre, o.apellido AS apellido, u.email AS email, o.telefono AS telefono
        FROM operador o
        JOIN usuario u ON u.id = o.usuario_id
        WHERE NOT EXISTS (SELECT 1 FROM barrio b WHERE b.operador_id = o.usuario_id)
        ORDER BY o.apellido, o.nombre
        """)
    List<OperadorConEmail> findDisponibles();

    /** US-15: Operador con su email y el barrio que tiene a cargo (null si está disponible). */
    interface OperadorListado extends OperadorConEmail {
        Long getBarrioId();

        String getBarrioNombre();
    }

    // Partes de la consulta del listado, compartidas con la de conteo para que paginen sobre las mismas filas
    String LISTADO_DESDE = """
        FROM operador o
        JOIN usuario u ON u.id = o.usuario_id
        LEFT JOIN barrio b ON b.operador_id = o.usuario_id
        WHERE (CAST(:estado AS text) IS NULL
               OR (CAST(:estado AS text) = 'DISPONIBLE' AND b.id IS NULL)
               OR (CAST(:estado AS text) = 'OCUPADO' AND b.id IS NOT NULL))
          AND (CAST(:q AS text) IS NULL
               OR LOWER(o.nombre) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
               OR LOWER(o.apellido) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
               OR LOWER(CONCAT(o.nombre, ' ', o.apellido)) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
               OR LOWER(CONCAT(o.apellido, ' ', o.nombre)) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
        """;

    /**
     * US-15: listado paginado de Operadores para el Administrador, con el barrio a cargo, filtrado por estado
     * (DISPONIBLE: sin barrio; OCUPADO: con barrio) y por un texto que se busca en nombre, apellido o email.
     * El texto llega con los comodines de LIKE ya escapados (la barra invertida es el escape por defecto de
     * PostgreSQL). Orden alfabético por apellido y nombre; el id desempata para que la paginación no repita ni
     * saltee filas (RNF-08).
     */
    @Query(nativeQuery = true,
        value = """
            SELECT o.usuario_id AS "id", o.nombre AS "nombre", o.apellido AS "apellido", u.email AS "email",
                   o.telefono AS "telefono", b.id AS "barrioId", b.nombre AS "barrioNombre"
            """ + LISTADO_DESDE + """
            ORDER BY LOWER(o.apellido), LOWER(o.nombre), o.usuario_id
            """,
        countQuery = "SELECT COUNT(*) " + LISTADO_DESDE)
    Page<OperadorListado> listarConFiltros(@Param("estado") String estado, @Param("q") String q, Pageable pageable);
}
