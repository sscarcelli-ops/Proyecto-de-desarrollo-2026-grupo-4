package ar.edu.grupo4.accesos.trabajador;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * Buscar un trabajador verificando que pertenezca al perfil autenticado (aislamiento RF-23).
     * Al no ser paginado, @EntityGraph carga el trabajador y sus documentos en un único JOIN.
     */
    @EntityGraph(attributePaths = "documentos")
    Optional<Trabajador> findByIdAndPerfilId(Long id, Long perfilId);

    /** Verificar DNI duplicado dentro del mismo perfil antes del alta (US-25, RN-11). */
    boolean existsByPerfilIdAndDniIgnoreCase(Long perfilId, String dni);
}
