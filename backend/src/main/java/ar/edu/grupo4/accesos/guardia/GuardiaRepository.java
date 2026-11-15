package ar.edu.grupo4.accesos.guardia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuardiaRepository extends JpaRepository<Guardia, Long> {

    /** US-32: Guardias del barrio, por apellido y nombre. No se pagina (bajo volumen por barrio, RNF-08). */
    List<Guardia> findByBarrioIdOrderByApellidoAscNombreAsc(Long barrioId);

    /** Filtra por barrio: un Guardia de otro barrio no aparece y se responde 404 (US-42). */
    Optional<Guardia> findByUsuarioIdAndBarrioId(Long usuarioId, Long barrioId);
}
