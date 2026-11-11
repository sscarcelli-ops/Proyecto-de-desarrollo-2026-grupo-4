package ar.edu.grupo4.accesos.perfil;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
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
     * Orden alfabético por razón social o nombre y apellido; el id desempata nombres iguales para que la paginación
     * no repita ni saltee filas (US-33).
     */
    @EntityGraph(attributePaths = "usuario")
    @Query("""
        SELECT p FROM Perfil p
        WHERE (:estado IS NULL OR p.estado = :estado)
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(p.cuitCuil) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR
            LOWER(p.razonSocial) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR
            LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR
            LOWER(p.apellido) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR
            LOWER(CONCAT(COALESCE(p.nombre, ''), ' ', COALESCE(p.apellido, ''))) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\' OR
            LOWER(CONCAT(COALESCE(p.apellido, ''), ' ', COALESCE(p.nombre, ''))) LIKE LOWER(CONCAT('%', :q, '%')) ESCAPE '\\'
          )
        ORDER BY LOWER(COALESCE(p.razonSocial, p.nombre)) ASC, LOWER(COALESCE(p.apellido, '')) ASC, p.id ASC
    """)
    Page<Perfil> listarConFiltros(
        @Param("estado") EstadoPerfil estado,
        @Param("q") String q,
        Pageable pageable
    );

    /**
     * US-33: de los perfiles indicados, los que están habilitados en el barrio.
     * Sin fila en habilitacion_barrio el perfil está deshabilitado (RF-05: al aprobarse no tiene ninguna).
     */
    @Query(value = """
        SELECT perfil_id FROM habilitacion_barrio
        WHERE barrio_id = :barrioId AND perfil_id IN (:perfilIds)
        """, nativeQuery = true)
    List<Long> perfilesHabilitadosEnBarrio(
        @Param("barrioId") Long barrioId,
        @Param("perfilIds") Collection<Long> perfilIds
    );

    /** US-34: habilita el perfil en el barrio. Si ya estaba habilitado no cambia nada. */
    @Modifying
    @Query(value = """
        INSERT INTO habilitacion_barrio (perfil_id, barrio_id) VALUES (:perfilId, :barrioId)
        ON CONFLICT (perfil_id, barrio_id) DO NOTHING
        """, nativeQuery = true)
    void habilitarEnBarrio(@Param("perfilId") Long perfilId, @Param("barrioId") Long barrioId);

    /** US-34: deshabilita el perfil en el barrio. Solo borra la fila de ese barrio (RN-02). */
    @Modifying
    @Query(value = "DELETE FROM habilitacion_barrio WHERE perfil_id = :perfilId AND barrio_id = :barrioId",
        nativeQuery = true)
    void deshabilitarEnBarrio(@Param("perfilId") Long perfilId, @Param("barrioId") Long barrioId);
}
