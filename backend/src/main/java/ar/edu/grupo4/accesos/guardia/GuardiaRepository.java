package ar.edu.grupo4.accesos.guardia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuardiaRepository extends JpaRepository<Guardia, Long> {

    /**
     * US-32: Guardias del barrio, por apellido y nombre (el id desempata para que la paginación no repita ni saltee
     * filas). El orden lo fija el nombre del método: el Pageable que llega no lleva sort (RNF-08).
     */
    Page<Guardia> findByBarrioIdOrderByApellidoAscNombreAscUsuarioIdAsc(Long barrioId, Pageable pageable);

    /** Filtra por barrio: un Guardia de otro barrio no aparece y se responde 404 (US-42). */
    Optional<Guardia> findByUsuarioIdAndBarrioId(Long usuarioId, Long barrioId);
}
