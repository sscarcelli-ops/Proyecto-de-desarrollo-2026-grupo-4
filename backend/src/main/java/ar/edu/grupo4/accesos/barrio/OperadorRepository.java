package ar.edu.grupo4.accesos.barrio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OperadorRepository extends JpaRepository<Operador, Long> {

    /** Operador con su email (está en usuario), para las listas del Administrador. */
    interface OperadorConEmail {
        Long getId();

        String getNombre();

        String getApellido();

        String getEmail();

        String getTelefono();
    }

    /** US-45: Operadores disponibles, o sea los que no tienen ningún barrio a cargo (RF-31). */
    @Query(nativeQuery = true, value = """
        SELECT o.usuario_id AS id, o.nombre AS nombre, o.apellido AS apellido, u.email AS email, o.telefono AS telefono
        FROM operador o
        JOIN usuario u ON u.id = o.usuario_id
        WHERE NOT EXISTS (SELECT 1 FROM barrio b WHERE b.operador_id = o.usuario_id)
        ORDER BY o.apellido, o.nombre
        """)
    List<OperadorConEmail> findDisponibles();
}
