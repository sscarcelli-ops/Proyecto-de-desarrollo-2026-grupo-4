package ar.edu.grupo4.accesos.trabajador;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Acceso a datos de trabajadores. Spring Data JPA implementa cada método a partir
 * de su nombre; no hace falta escribir SQL ni la clase de implementación.
 *
 * Todas las consultas que salen de acá filtran por perfilId para respetar el
 * aislamiento entre Empresas/Monotributistas (RF-23). El perfilId siempre viene
 * del usuario autenticado, nunca del request del frontend.
 */
public interface TrabajadorRepository extends JpaRepository<Trabajador, Long> {

    /**
     * Listado paginado de los trabajadores de un perfil (US-26, RNF-08).
     * No lleva @EntityGraph para permitir que PostgreSQL aplique LIMIT/OFFSET en la BD sin
     * paginar en memoria (aviso HHH90003004). Los documentos se cargan en lote mediante
     * @BatchSize en la entidad Trabajador.
     */
    Page<Trabajador> findByPerfilId(Long perfilId, Pageable pageable);

    /**
     * US-29: Listado paginado de los trabajadores con filtros y búsqueda (RF-30).
     *
     * Permite:
     * - Búsqueda por nombre, apellido y DNI (case-insensitive).
     * - Filtrar u ocultar trabajadores dados de baja (por defecto ocultos si incluirBajas es false).
     * - Filtrar por estado documental calculado (VIGENTE, VENCIDO, PENDIENTE_CARGA).
     *
     * No lleva @EntityGraph para permitir que PostgreSQL aplique LIMIT/OFFSET en la BD sin
     * paginar en memoria. Los documentos se cargan en lote mediante @BatchSize en Trabajador.
     */
    @Query(value = """
        SELECT t FROM Trabajador t
        WHERE t.perfilId = :perfilId
          AND (:incluirBajas = true OR t.fechaBaja IS NULL)
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(t.nombre) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR
            LOWER(t.apellido) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR
            LOWER(t.dni) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR
            LOWER(CONCAT(t.nombre, ' ', t.apellido)) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR
            LOWER(CONCAT(t.apellido, ' ', t.nombre)) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\'
          )
          AND (
            :estadoDocumental IS NULL OR :estadoDocumental = '' OR
            (:estadoDocumental = 'PENDIENTE_CARGA' AND NOT EXISTS (
                SELECT d0 FROM Documento d0 WHERE d0.trabajador = t
            )) OR
            (:estadoDocumental = 'VENCIDO' AND EXISTS (
                SELECT d1 FROM Documento d1 WHERE d1.trabajador = t AND d1.fechaVencimiento < :hoy
            )) OR
            (:estadoDocumental = 'VIGENTE' AND EXISTS (
                SELECT d2 FROM Documento d2 WHERE d2.trabajador = t
            ) AND NOT EXISTS (
                SELECT d3 FROM Documento d3 WHERE d3.trabajador = t AND d3.fechaVencimiento < :hoy
            ))
          )
        """,
        countQuery = """
        SELECT COUNT(t) FROM Trabajador t
        WHERE t.perfilId = :perfilId
          AND (:incluirBajas = true OR t.fechaBaja IS NULL)
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(t.nombre) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR
            LOWER(t.apellido) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR
            LOWER(t.dni) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR
            LOWER(CONCAT(t.nombre, ' ', t.apellido)) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR
            LOWER(CONCAT(t.apellido, ' ', t.nombre)) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\'
          )
          AND (
            :estadoDocumental IS NULL OR :estadoDocumental = '' OR
            (:estadoDocumental = 'PENDIENTE_CARGA' AND NOT EXISTS (
                SELECT d0 FROM Documento d0 WHERE d0.trabajador = t
            )) OR
            (:estadoDocumental = 'VENCIDO' AND EXISTS (
                SELECT d1 FROM Documento d1 WHERE d1.trabajador = t AND d1.fechaVencimiento < :hoy
            )) OR
            (:estadoDocumental = 'VIGENTE' AND EXISTS (
                SELECT d2 FROM Documento d2 WHERE d2.trabajador = t
            ) AND NOT EXISTS (
                SELECT d3 FROM Documento d3 WHERE d3.trabajador = t AND d3.fechaVencimiento < :hoy
            ))
          )
        """)
    Page<Trabajador> listarConFiltros(
        @Param("perfilId") Long perfilId,
        @Param("q") String q,
        @Param("estadoDocumental") String estadoDocumental,
        @Param("incluirBajas") boolean incluirBajas,
        @Param("hoy") LocalDate hoy,
        Pageable pageable
    );

    /**
     * Buscar un trabajador verificando que pertenezca al perfil autenticado (aislamiento RF-23).
     * Al no ser paginado, @EntityGraph carga el trabajador y sus documentos en un único JOIN.
     */
    @EntityGraph(attributePaths = "documentos")
    Optional<Trabajador> findByIdAndPerfilId(Long id, Long perfilId);

    /**
     * Obtener el trabajador único vinculado a un perfil (US-24, Monotributista).
     */
    @EntityGraph(attributePaths = "documentos")
    Optional<Trabajador> findFirstByPerfilId(Long perfilId);

    /** Verificar DNI duplicado dentro del mismo perfil antes del alta (US-25, RN-11). */
    boolean existsByPerfilIdAndDniIgnoreCase(Long perfilId, String dni);
}
