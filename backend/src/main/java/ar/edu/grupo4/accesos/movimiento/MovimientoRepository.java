package ar.edu.grupo4.accesos.movimiento;

import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.movimiento.dto.BarrioFiltroResponse;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoPropioFiltros;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoPropioResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * US-31 y US-46: Consulta paginada del historial de accesos propio de una Empresa o Monotributista.
     * Solo devuelve movimientos de trabajadores pertenecientes a su perfil (aislamiento RI-02).
     * Ordenado cronológicamente descendente (del más reciente al más antiguo).
     */
    public PageResponse<MovimientoPropioResponse> consultarHistorialPropio(Long perfilId, MovimientoPropioFiltros filtros, Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE t.perfil_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(perfilId);

        if (filtros.desde() != null) {
            where.append(" AND m.fecha_hora >= ?::timestamptz ");
            params.add(filtros.desde().atStartOfDay().toString());
        }
        if (filtros.hasta() != null) {
            where.append(" AND m.fecha_hora < (?::date + interval '1 day') ");
            params.add(filtros.hasta().toString());
        }
        if (filtros.dni() != null && !filtros.dni().isBlank()) {
            where.append(" AND t.dni = ? ");
            params.add(filtros.dni().trim());
        }
        if (filtros.barrioId() != null) {
            where.append(" AND m.barrio_id = ? ");
            params.add(filtros.barrioId());
        }

        String countSql = """
            SELECT count(*)
            FROM movimiento m
            JOIN trabajador t ON t.id = m.trabajador_id
            JOIN barrio b ON b.id = m.barrio_id
            """ + where;

        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());
        long totalElements = total != null ? total : 0;

        String querySql = """
            SELECT m.id, m.fecha_hora, m.tipo,
                   t.nombre AS trabajador_nombre, t.apellido AS trabajador_apellido, t.dni AS trabajador_dni,
                   b.nombre AS barrio_nombre,
                   m.motivo
            FROM movimiento m
            JOIN trabajador t ON t.id = m.trabajador_id
            JOIN barrio b ON b.id = m.barrio_id
            """ + where + """
            ORDER BY m.fecha_hora DESC, m.id DESC
            LIMIT ? OFFSET ?
            """;

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(pageable.getPageSize());
        queryParams.add(pageable.getOffset());

        List<MovimientoPropioResponse> content = jdbc.query(querySql, (rs, rowNum) -> new MovimientoPropioResponse(
                rs.getLong("id"),
                rs.getObject("fecha_hora", OffsetDateTime.class),
                rs.getString("tipo"),
                rs.getString("trabajador_nombre"),
                rs.getString("trabajador_apellido"),
                rs.getString("trabajador_dni"),
                rs.getString("barrio_nombre"),
                rs.getString("motivo")
        ), queryParams.toArray());

        int totalPages = pageable.getPageSize() > 0 ? (int) Math.ceil((double) totalElements / pageable.getPageSize()) : 0;
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), totalElements, totalPages);
    }

    /**
     * US-31: Barrios donde la Empresa está habilitada para poblar el dropdown de filtro.
     */
    public List<BarrioFiltroResponse> listarBarriosHabilitados(Long perfilId) {
        String sql = """
            SELECT DISTINCT b.id, b.nombre
            FROM barrio b
            JOIN habilitacion_barrio hb ON hb.barrio_id = b.id
            WHERE hb.perfil_id = ?
            ORDER BY b.nombre
            """;
        return jdbc.query(sql, (rs, rowNum) -> new BarrioFiltroResponse(
                rs.getLong("id"),
                rs.getString("nombre")
        ), perfilId);
    }
}
