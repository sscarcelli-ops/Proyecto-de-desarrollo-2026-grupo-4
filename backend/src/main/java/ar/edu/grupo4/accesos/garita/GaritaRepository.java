package ar.edu.grupo4.accesos.garita;

import ar.edu.grupo4.accesos.garita.dto.IngresoActivoInfo;
import ar.edu.grupo4.accesos.garita.dto.MovimientoResponse;
import ar.edu.grupo4.accesos.garita.dto.VerificacionResponse;
import ar.edu.grupo4.accesos.garita.dto.VinculoResultado;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a datos del módulo Garita.
 * Todo acceso usa JdbcTemplate para evitar dependencias cruzadas con repositorios JPA de otros módulos.
 */
@Repository
@RequiredArgsConstructor
public class GaritaRepository {

    private final JdbcTemplate jdbc;

    // ──────────────────────────────────────────────────────────────────
    // Búsqueda por DNI
    // ──────────────────────────────────────────────────────────────────

    /**
     * Devuelve todos los vínculos (trabajador+perfil) que tienen ese DNI.
     * Para EMPRESA usa razon_social; para MONOTRIBUTISTA construye "Juan Pérez (Monotributista)".
     */
    public List<VinculoResultado> buscarVinculosPorDni(String dni) {
        String sql = """
                SELECT t.id              AS trabajador_id,
                       p.id              AS perfil_id,
                       p.tipo            AS tipo,
                       p.razon_social,
                       p.nombre          AS perfil_nombre,
                       p.apellido        AS perfil_apellido
                FROM trabajador t
                JOIN perfil p ON p.id = t.perfil_id
                WHERE t.dni = ?
                ORDER BY p.tipo, t.id
                """;
        return jdbc.query(sql, (rs, rowNum) -> mapVinculo(rs), dni);
    }

    private VinculoResultado mapVinculo(ResultSet rs) throws SQLException {
        String tipo = rs.getString("tipo");
        String nombreVinculo;
        if ("EMPRESA".equals(tipo)) {
            nombreVinculo = rs.getString("razon_social");
        } else {
            nombreVinculo = rs.getString("perfil_nombre") + " " + rs.getString("perfil_apellido")
                    + " (Monotributista)";
        }
        return new VinculoResultado(
                rs.getLong("trabajador_id"),
                rs.getLong("perfil_id"),
                nombreVinculo,
                tipo
        );
    }

    /**
     * Devuelve información del nombre/apellido/foto del trabajador (primer vínculo encontrado para ese DNI).
     * Retorna null si no existe.
     */
    public Optional<TrabajadorResumen> buscarResumenPorDni(String dni) {
        String sql = """
                SELECT t.nombre, t.apellido, t.foto_ruta
                FROM trabajador t
                WHERE t.dni = ?
                ORDER BY t.id
                LIMIT 1
                """;
        List<TrabajadorResumen> rows = jdbc.query(sql, (rs, rowNum) ->
                new TrabajadorResumen(
                        rs.getString("nombre"),
                        rs.getString("apellido"),
                        rs.getString("foto_ruta")
                ), dni);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** Proyección interna: datos básicos del trabajador para armar la respuesta de búsqueda. */
    public record TrabajadorResumen(String nombre, String apellido, String fotoRuta) {}

    // ──────────────────────────────────────────────────────────────────
    // Ingreso activo
    // ──────────────────────────────────────────────────────────────────

    /**
     * Busca el último movimiento de cualquier trabajador con ese DNI en el barrio.
     * Si ese movimiento es INGRESO → hay ingreso activo.
     */
    public Optional<IngresoActivoInfo> buscarIngresoActivo(String dni, Long barrioId) {
        String sql = """
                SELECT m.id              AS movimiento_id,
                       m.trabajador_id,
                       m.tipo            AS mov_tipo,
                       p.tipo            AS perfil_tipo,
                       p.razon_social,
                       p.nombre          AS perfil_nombre,
                       p.apellido        AS perfil_apellido
                FROM movimiento m
                JOIN trabajador t ON t.id = m.trabajador_id
                JOIN perfil     p ON p.id = t.perfil_id
                WHERE t.dni = ?
                  AND m.barrio_id = ?
                ORDER BY m.fecha_hora DESC
                LIMIT 1
                """;
        List<IngresoActivoInfo> rows = jdbc.query(sql, (rs, rowNum) -> {
            String movTipo = rs.getString("mov_tipo");
            if (!"INGRESO".equals(movTipo)) {
                return null; // último movimiento es EGRESO → no hay ingreso activo
            }
            String perfilTipo = rs.getString("perfil_tipo");
            String nombreVinculo;
            if ("EMPRESA".equals(perfilTipo)) {
                nombreVinculo = rs.getString("razon_social");
            } else {
                nombreVinculo = rs.getString("perfil_nombre") + " " + rs.getString("perfil_apellido")
                        + " (Monotributista)";
            }
            return new IngresoActivoInfo(
                    rs.getLong("movimiento_id"),
                    rs.getLong("trabajador_id"),
                    nombreVinculo,
                    perfilTipo
            );
        }, dni, barrioId);

        return rows.stream().filter(r -> r != null).findFirst();
    }

    /**
     * Busca si el trabajador específico (por trabajadorId) tiene un ingreso activo en ese barrio.
     * Usado al registrar egreso para encontrar el movimiento a cerrar.
     */
    public Optional<Long> buscarIngresoActivoPorTrabajador(Long trabajadorId, Long barrioId) {
        String sql = """
                SELECT id, tipo
                FROM movimiento
                WHERE trabajador_id = ?
                  AND barrio_id = ?
                ORDER BY fecha_hora DESC
                LIMIT 1
                """;
        List<Optional<Long>> rows = jdbc.query(sql, (rs, rowNum) -> {
            String tipo = rs.getString("tipo");
            if ("INGRESO".equals(tipo)) {
                return Optional.of(rs.getLong("id"));
            }
            return Optional.<Long>empty();
        }, trabajadorId, barrioId);

        return rows.isEmpty() ? Optional.empty() : rows.get(0);
    }

    // ──────────────────────────────────────────────────────────────────
    // Verificación de acceso (stored procedure)
    // ──────────────────────────────────────────────────────────────────

    /**
     * Llama a la función PostgreSQL {@code verificar_acceso(p_trabajador_id, p_barrio_id)}
     * y retorna el resultado mapeado al DTO.
     */
    public VerificacionResponse verificarAcceso(Long trabajadorId, Long barrioId) {
        String sql = "SELECT habilitado, motivo FROM verificar_acceso(?, ?)";
        return jdbc.queryForObject(sql,
                (rs, rowNum) -> new VerificacionResponse(
                        rs.getBoolean("habilitado"),
                        rs.getString("motivo")
                ),
                trabajadorId, barrioId);
    }

    // ──────────────────────────────────────────────────────────────────
    // Registro de movimientos
    // ──────────────────────────────────────────────────────────────────

    /**
     * Inserta un INGRESO y retorna el MovimientoResponse completo.
     */
    public MovimientoResponse registrarIngreso(Long trabajadorId, Long barrioId, Long guardiaId) {
        String sql = """
                INSERT INTO movimiento (trabajador_id, barrio_id, guardia_id, tipo)
                VALUES (?, ?, ?, 'INGRESO')
                RETURNING id, fecha_hora
                """;
        return jdbc.queryForObject(sql, (rs, rowNum) -> {
            long id = rs.getLong("id");
            OffsetDateTime fechaHora = rs.getTimestamp("fecha_hora")
                    .toInstant().atOffset(ZoneOffset.UTC);
            return buildMovimientoResponse(id, "INGRESO", fechaHora, trabajadorId);
        }, trabajadorId, barrioId, guardiaId);
    }

    /**
     * Inserta un EGRESO y retorna el MovimientoResponse completo.
     */
    public MovimientoResponse registrarEgreso(Long trabajadorId, Long barrioId, Long guardiaId) {
        String sql = """
                INSERT INTO movimiento (trabajador_id, barrio_id, guardia_id, tipo)
                VALUES (?, ?, ?, 'EGRESO')
                RETURNING id, fecha_hora
                """;
        return jdbc.queryForObject(sql, (rs, rowNum) -> {
            long id = rs.getLong("id");
            OffsetDateTime fechaHora = rs.getTimestamp("fecha_hora")
                    .toInstant().atOffset(ZoneOffset.UTC);
            return buildMovimientoResponse(id, "EGRESO", fechaHora, trabajadorId);
        }, trabajadorId, barrioId, guardiaId);
    }

    /** Construye el MovimientoResponse buscando nombre/apellido del trabajador y vínculo. */
    private MovimientoResponse buildMovimientoResponse(
            long movId, String tipo, OffsetDateTime fechaHora, Long trabajadorId) {
        String sql = """
                SELECT t.nombre, t.apellido,
                       p.tipo      AS perfil_tipo,
                       p.razon_social,
                       p.nombre    AS perfil_nombre,
                       p.apellido  AS perfil_apellido
                FROM trabajador t
                JOIN perfil p ON p.id = t.perfil_id
                WHERE t.id = ?
                """;
        return jdbc.queryForObject(sql, (rs, rowNum) -> {
            String nombreTrabajador = rs.getString("nombre") + " " + rs.getString("apellido");
            String perfilTipo = rs.getString("perfil_tipo");
            String nombreVinculo;
            if ("EMPRESA".equals(perfilTipo)) {
                nombreVinculo = rs.getString("razon_social");
            } else {
                nombreVinculo = rs.getString("perfil_nombre") + " " + rs.getString("perfil_apellido")
                        + " (Monotributista)";
            }
            return new MovimientoResponse(movId, tipo, fechaHora, nombreTrabajador, nombreVinculo);
        }, trabajadorId);
    }
}
