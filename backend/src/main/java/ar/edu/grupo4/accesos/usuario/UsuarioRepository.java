package ar.edu.grupo4.accesos.usuario;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // El email es único sin distinguir mayúsculas (índice lower(email)). Trae los roles en la misma consulta.
    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findWithRolesById(Long id);
}
