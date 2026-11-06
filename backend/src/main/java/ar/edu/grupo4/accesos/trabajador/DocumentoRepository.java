package ar.edu.grupo4.accesos.trabajador;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acceso a datos de documentos legales (ART / Seguro de Vida).
 */
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    /**
     * Busca un documento de un trabajador por su tipo (máximo uno por tipo según UNIQUE trabajador_id + tipo).
     */
    Optional<Documento> findByTrabajadorIdAndTipo(Long trabajadorId, TipoDocumento tipo);
}
