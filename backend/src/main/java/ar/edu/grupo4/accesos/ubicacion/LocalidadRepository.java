package ar.edu.grupo4.accesos.ubicacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocalidadRepository extends JpaRepository<Localidad, String> {

    List<Localidad> findByProvinciaIdOrderByNombreAscDepartamentoAsc(String provinciaId);
}
