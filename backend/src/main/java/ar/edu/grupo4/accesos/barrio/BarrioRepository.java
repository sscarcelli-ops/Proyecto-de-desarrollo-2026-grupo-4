package ar.edu.grupo4.accesos.barrio;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BarrioRepository extends JpaRepository<Barrio, Long> {

    /** Si el Operador ya tiene a cargo algún barrio (un Operador tiene un solo barrio, RN-10). */
    boolean existsByOperadorId(Long operadorId);

    /** Si ya hay un barrio con ese nombre en esa localidad (T-11), sin distinguir mayúsculas. */
    boolean existsByNombreIgnoreCaseAndLocalidadId(String nombre, String localidadId);

    /** Lo mismo, pero ignorando al propio barrio (para modificarlo sin chocar consigo mismo). */
    boolean existsByNombreIgnoreCaseAndLocalidadIdAndIdNot(String nombre, String localidadId, Long id);

    /**
     * US-10 y US-45: listado paginado (RNF-08) con el selector de estado. Sin estado, todos; con sinOperador, solo
     * los que no tienen Operador. El orden lo pone el Service.
     */
    @Query("""
        select b from Barrio b
        where (:estado is null or b.estado = :estado)
          and (:sinOperador = false or b.operadorId is null)
        """)
    Page<Barrio> listarConFiltro(@Param("estado") EstadoBarrio estado, @Param("sinOperador") boolean sinOperador,
                                 Pageable pageable);

    /** US-45: trae el barrio bloqueándolo, así dos Administradores no lo asignan a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Barrio b where b.id = :id")
    Optional<Barrio> findByIdForUpdate(@Param("id") Long id);
}
