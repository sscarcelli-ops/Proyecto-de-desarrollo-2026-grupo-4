package ar.edu.grupo4.accesos.perfil;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    Optional<Perfil> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioId(Long usuarioId);

    boolean existsByCuitCuil(String cuitCuil);

    boolean existsByDniTitularAndTipo(String dniTitular, TipoPerfil tipo);
}
