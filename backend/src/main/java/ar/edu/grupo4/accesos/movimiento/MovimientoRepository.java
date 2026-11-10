package ar.edu.grupo4.accesos.movimiento;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Acceso a datos de los movimientos (ingresos y egresos). Solo inserción: nunca se modifica ni se borra (RN-06). */
@Repository
@RequiredArgsConstructor
public class MovimientoRepository {

    private final JdbcTemplate jdbc;

    /**
     * Registra un EGRESO automático (sin Guardia, con motivo) por cada trabajador cuyo último movimiento en el barrio
     * es un INGRESO, o sea con el ingreso abierto. Devuelve cuántos egresos registró.
     */
    public int registrarEgresosAutomaticos(Long barrioId, String motivo) {
        return jdbc.update("""
            INSERT INTO movimiento (trabajador_id, barrio_id, guardia_id, tipo, motivo)
            SELECT u.trabajador_id, u.barrio_id, NULL, 'EGRESO', ?::varchar
            FROM (
                SELECT DISTINCT ON (trabajador_id) trabajador_id, barrio_id, tipo
                FROM movimiento
                WHERE barrio_id = ?
                ORDER BY trabajador_id, fecha_hora DESC, id DESC
            ) u
            WHERE u.tipo = 'INGRESO'
            """, motivo, barrioId);
    }
}
