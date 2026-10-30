package ar.edu.grupo4.accesos.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Short> {

    Optional<Rol> findByCodigo(String codigo);
}
