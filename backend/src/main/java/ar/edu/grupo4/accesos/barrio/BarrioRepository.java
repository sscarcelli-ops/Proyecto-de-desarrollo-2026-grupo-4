package ar.edu.grupo4.accesos.barrio;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BarrioRepository extends JpaRepository<Barrio, Long> {

    /** Si el Operador ya tiene a cargo algún barrio (un Operador tiene un solo barrio, RN-10). */
    boolean existsByOperadorId(Long operadorId);

    /** US-45: trae el barrio bloqueándolo, así dos Administradores no lo asignan a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Barrio b where b.id = :id")
    Optional<Barrio> findByIdForUpdate(@Param("id") Long id);
}
