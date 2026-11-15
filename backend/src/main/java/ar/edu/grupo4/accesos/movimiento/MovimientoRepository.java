package ar.edu.grupo4.accesos.movimiento;

import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.movimiento.dto.EntidadFiltroResponse;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoFiltros;
import ar.edu.grupo4.accesos.movimiento.dto.MovimientoHistorialResponse;
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
     * US-36: Consulta paginada del historial de accesos del barrio con filtros opcionales (fechas, DNI, empresa).
     * Ordenado cronológicamente descendente (del más reciente al más antiguo).
     */
    public PageResponse<MovimientoHistorialResponse> consultarHistorial(Long barrioId, MovimientoFiltros filtros, Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE m.barrio_id = ? ");
        List<Object> params = new ArrayList<>();
        params.add(barrioId);

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
        if (filtros.perfilId() != null) {
            where.append(" AND t.perfil_id = ? ");
            params.add(filtros.perfilId());
        }

        String countSql = """
            SELECT count(*)
            FROM movimiento m
            JOIN trabajador t ON t.id = m.trabajador_id
            JOIN perfil p ON p.id = t.perfil_id
            """ + where;

        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());
        long totalElements = total != null ? total : 0;

        String querySql = """
            SELECT m.id, m.fecha_hora, m.tipo,
                   t.nombre AS trabajador_nombre, t.apellido AS trabajador_apellido, t.dni AS trabajador_dni,
                   p.tipo AS entidad_tipo,
                   COALESCE(p.razon_social, p.nombre || ' ' || p.apellido) AS entidad_nombre,
                   m.motivo
            FROM movimiento m
            JOIN trabajador t ON t.id = m.trabajador_id
            JOIN perfil p ON p.id = t.perfil_id
            """ + where + """
            ORDER BY m.fecha_hora DESC, m.id DESC
            LIMIT ? OFFSET ?
            """;

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(pageable.getPageSize());
        queryParams.add(pageable.getOffset());

        List<MovimientoHistorialResponse> content = jdbc.query(querySql, (rs, rowNum) -> new MovimientoHistorialResponse(
                rs.getLong("id"),
                rs.getObject("fecha_hora", OffsetDateTime.class),
                rs.getString("tipo"),
                rs.getString("trabajador_nombre"),
                rs.getString("trabajador_apellido"),
                rs.getString("trabajador_dni"),
                rs.getString("entidad_tipo"),
                rs.getString("entidad_nombre"),
                rs.getString("motivo")
        ), queryParams.toArray());

        int totalPages = pageable.getPageSize() > 0 ? (int) Math.ceil((double) totalElements / pageable.getPageSize()) : 0;
        return new PageResponse<>(content, pageable.getPageNumber(), pageable.getPageSize(), totalElements, totalPages);
    }

    /**
     * US-36: Entidades (Empresas y Monotributistas) habilitadas en el barrio para poblar el dropdown de filtro.
     */
    public List<EntidadFiltroResponse> listarEntidadesDelBarrio(Long barrioId) {
        String sql = """
            SELECT DISTINCT p.id,
                   COALESCE(p.razon_social, p.nombre || ' ' || p.apellido) AS nombre,
                   p.tipo
            FROM perfil p
            JOIN habilitacion_barrio hb ON hb.perfil_id = p.id
            WHERE hb.barrio_id = ? AND p.estado = 'APROBADO'
            ORDER BY nombre
            """;
        return jdbc.query(sql, (rs, rowNum) -> new EntidadFiltroResponse(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("tipo")
        ), barrioId);
    }
}
