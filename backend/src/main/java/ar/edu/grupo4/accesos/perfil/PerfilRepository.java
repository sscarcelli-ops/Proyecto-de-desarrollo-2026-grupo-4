package ar.edu.grupo4.accesos.perfil;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    Optional<Perfil> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);

    boolean existsByCuitCuil(String cuitCuil);

    boolean existsByDniTitularAndTipo(String dniTitular, TipoPerfil tipo);

    /**
     * US-18: Listado paginado de perfiles (Empresas y Monotributistas) para el Administrador.
     * Carga el usuario asociado para evitar consultas adicionales al mapear email e id de usuario.
     * Orden alfabético por razón social o nombre.
     */
    @EntityGraph(attributePaths = "usuario")
    @Query("""
        SELECT p FROM Perfil p
        WHERE (:estado IS NULL OR p.estado = :estado)
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(p.cuitCuil) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(p.razonSocial) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(p.apellido) LIKE LOWER(CONCAT('%', :q, '%'))
          )
        ORDER BY LOWER(COALESCE(p.razonSocial, p.nombre)) ASC
    """)
    Page<Perfil> listarConFiltros(
        @Param("estado") EstadoPerfil estado,
        @Param("q") String q,
        Pageable pageable
    );
}
