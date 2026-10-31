package ar.edu.grupo4.accesos.garita;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.garita.dto.BusquedaDniResponse;
import ar.edu.grupo4.accesos.garita.dto.IngresoActivoInfo;
import ar.edu.grupo4.accesos.garita.dto.MovimientoResponse;
import ar.edu.grupo4.accesos.garita.dto.VerificacionResponse;
import ar.edu.grupo4.accesos.garita.dto.VinculoResultado;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Lógica de negocio de la Garita (US-37, US-38, US-39, US-40).
 * Nunca accede a repositorios de otros módulos; delega todo en {@link GaritaRepository}.
 * El barrio y el Guardia salen siempre del usuario autenticado ({@link UsuarioActual}, T-03): el Guardia solo
 * busca, verifica y registra movimientos en su barrio (US-42 CA-3).
 */
@Service
@RequiredArgsConstructor
public class GaritaService {

    private final GaritaRepository repository;
    private final UsuarioActual usuarioActual;

    // ──────────────────────────────────────────────────────────────────
    // US-37 – Buscar trabajador por DNI
    // ──────────────────────────────────────────────────────────────────

    /**
     * Busca todos los vínculos laborales para el DNI dado y detecta si hay un ingreso activo
     * en el barrio del guardia.
     *
     * @throws BusinessException TRABAJADOR_NO_ENCONTRADO si no existe ningún vínculo con ese DNI
     */
    public BusquedaDniResponse buscarPorDni(String dni) {
        List<VinculoResultado> vinculos = repository.buscarVinculosPorDni(dni);

        if (vinculos.isEmpty()) {
            throw new BusinessException(
                    "TRABAJADOR_NO_ENCONTRADO",
                    "No se encontró un trabajador con ese DNI"
            );
        }

        GaritaRepository.TrabajadorResumen resumen = repository.buscarResumenPorDni(dni)
                .orElseThrow(() -> new BusinessException(
                        "TRABAJADOR_NO_ENCONTRADO",
                        "No se encontró un trabajador con ese DNI"
                ));

        Optional<IngresoActivoInfo> ingresoActivo = repository.buscarIngresoActivo(dni, usuarioActual.barrioId());

        return new BusquedaDniResponse(
                resumen.nombre(),
                resumen.apellido(),
                dni,
                resumen.fotoRuta() != null && !resumen.fotoRuta().isBlank(),
                vinculos,
                ingresoActivo.orElse(null)
        );
    }

    // ──────────────────────────────────────────────────────────────────
    // US-38 – Verificar acceso
    // ──────────────────────────────────────────────────────────────────

    /**
     * Delega en la función PostgreSQL {@code verificar_acceso} para determinar si un trabajador
     * puede ingresar al barrio.
     */
    public VerificacionResponse verificarAcceso(Long trabajadorId) {
        return repository.verificarAcceso(trabajadorId, usuarioActual.barrioId());
    }

    // ──────────────────────────────────────────────────────────────────
    // US-39 – Registrar ingreso
    // ──────────────────────────────────────────────────────────────────

    /**
     * Re-verifica habilitación (evita race condition entre verificar y registrar) y registra el INGRESO.
     *
     * @throws BusinessException INGRESO_DENEGADO si la verificación falla
     */
    @Transactional
    public MovimientoResponse registrarIngreso(Long trabajadorId) {
        Long barrioId = usuarioActual.barrioId();
        VerificacionResponse verificacion = repository.verificarAcceso(trabajadorId, barrioId);
        if (!verificacion.habilitado()) {
            throw new BusinessException(
                    "INGRESO_DENEGADO",
                    "No se puede registrar el ingreso: " + verificacion.motivo()
            );
        }
        return repository.registrarIngreso(trabajadorId, barrioId, usuarioActual.id());
    }

    // ──────────────────────────────────────────────────────────────────
    // US-40 – Registrar egreso
    // ──────────────────────────────────────────────────────────────────

    /**
     * Verifica que exista un ingreso activo para el trabajador específico en el barrio y registra el EGRESO.
     *
     * @throws BusinessException SIN_INGRESO_ACTIVO si el trabajador no tiene ingreso abierto
     */
    @Transactional
    public MovimientoResponse registrarEgreso(Long trabajadorId) {
        Long barrioId = usuarioActual.barrioId();
        repository.buscarIngresoActivoPorTrabajador(trabajadorId, barrioId)
                .orElseThrow(() -> new BusinessException(
                        "SIN_INGRESO_ACTIVO",
                        "El trabajador no tiene un ingreso activo en este barrio"
                ));
        return repository.registrarEgreso(trabajadorId, barrioId, usuarioActual.id());
    }
}
