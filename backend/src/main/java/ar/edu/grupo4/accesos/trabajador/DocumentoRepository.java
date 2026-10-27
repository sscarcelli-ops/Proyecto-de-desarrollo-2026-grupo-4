package ar.edu.grupo4.accesos.trabajador;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de documentos legales (ART / Seguro de Vida).
 *
 * En S1 no se usa directamente (la documentación se carga en S2 con US-28),
 * pero el TrabajadorService accede a los documentos a través de la relación
 * @OneToMany de Trabajador para calcular el estado documental.
 */
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
}
