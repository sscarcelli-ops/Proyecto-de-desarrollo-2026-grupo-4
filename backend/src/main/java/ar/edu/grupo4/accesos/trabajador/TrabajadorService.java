package ar.edu.grupo4.accesos.trabajador;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorEditarRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reglas de negocio del módulo de trabajadores (US-25, US-26, y base para US-27 a 30).
 *
 * Todas las operaciones reciben el usuarioId del token JWT. Internamente, el Service
 * resuelve el perfilId asociado. Ese perfilId NUNCA sale del request del frontend:
 * se obtiene desde la BD a partir del usuario autenticado, garantizando el aislamiento
 * entre Empresas (RF-23, US-41).
 */
@Service
@RequiredArgsConstructor
public class TrabajadorService {

    private final TrabajadorRepository trabajadorRepository;
    private final PerfilIdResolver perfilIdResolver;

    // ========================================================================================
    // US-25: Registrar trabajador
    // ========================================================================================

    /**
     * Registra un nuevo trabajador para el perfil del usuario autenticado.
     * En S1 solo se registran nombre, apellido y DNI (sin archivos).
     *
     * Reglas:
     * - El DNI no puede estar duplicado dentro del mismo perfil (RN-11).
     * - El mismo DNI SÍ puede existir en otro perfil (RN-08): cada Empresa
     *   tiene su propio registro independiente de la misma persona.
     */
    @Transactional
    public TrabajadorResponse crear(Long usuarioId, TrabajadorRequest request) {
        Long perfilId = perfilIdResolver.resolver(usuarioId);

        if (trabajadorRepository.existsByPerfilIdAndDniIgnoreCase(perfilId, request.dni())) {
            throw new BusinessException("TRABAJADOR_DUPLICADO",
                "Este trabajador ya está registrado en tu empresa.");
        }

        Trabajador trabajador = new Trabajador();
        trabajador.setPerfilId(perfilId);
        trabajador.setNombre(request.nombre().trim());
        trabajador.setApellido(request.apellido().trim());
        trabajador.setDni(request.dni().trim());

        trabajador = trabajadorRepository.save(trabajador);

        return TrabajadorResponse.from(trabajador, calcularEstadoDocumental(trabajador));
    }

    // ========================================================================================
    // US-26: Listar y modificar trabajadores
    // ========================================================================================

    /**
     * Listado paginado de los trabajadores del perfil del usuario autenticado (RNF-08).
     * Orden por defecto: alfabético por apellido (configurado en el Controller).
     */
    @Transactional(readOnly = true)
    public PageResponse<TrabajadorResponse> listar(Long usuarioId, Pageable pageable) {
        Long perfilId = perfilIdResolver.resolver(usuarioId);
        return PageResponse.from(
            trabajadorRepository.findByPerfilId(perfilId, pageable),
            t -> TrabajadorResponse.from(t, calcularEstadoDocumental(t))
        );
    }

    /**
     * Detalle de un trabajador, verificando que pertenezca al perfil del usuario autenticado.
     * Si el ID existe pero es de otro perfil, devuelve 404 (no 403), para no
     * revelar la existencia de trabajadores ajenos.
     */
    @Transactional(readOnly = true)
    public TrabajadorResponse obtener(Long usuarioId, Long trabajadorId) {
        Long perfilId = perfilIdResolver.resolver(usuarioId);
        Trabajador trabajador = buscarDelPerfil(perfilId, trabajadorId);
        return TrabajadorResponse.from(trabajador, calcularEstadoDocumental(trabajador));
    }

    /**
     * Modifica nombre y/o apellido de un trabajador existente.
     * Foto y documentación se modifican por separado (US-28, S2).
     */
    @Transactional
    public TrabajadorResponse editar(Long usuarioId, Long trabajadorId, TrabajadorEditarRequest request) {
        Long perfilId = perfilIdResolver.resolver(usuarioId);
        Trabajador trabajador = buscarDelPerfil(perfilId, trabajadorId);

        trabajador.setNombre(request.nombre().trim());
        trabajador.setApellido(request.apellido().trim());

        return TrabajadorResponse.from(trabajador, calcularEstadoDocumental(trabajador));
    }

    // ========================================================================================
    // Lógica compartida
    // ========================================================================================

    /**
     * Busca un trabajador verificando que pertenezca al perfil. Si no existe
     * o pertenece a otro perfil, lanza NotFoundException (404).
     */
    private Trabajador buscarDelPerfil(Long perfilId, Long trabajadorId) {
        return trabajadorRepository.findByIdAndPerfilId(trabajadorId, perfilId)
            .orElseThrow(NotFoundException::new);
    }

    /**
     * Calcula el estado documental de un trabajador en base a sus documentos (RN-04, RNF-04).
     *
     * Regla:
     *   - Sin documentos cargados              → PENDIENTE_CARGA
     *   - Algún documento con vencimiento < hoy → VENCIDO (se toma el más próximo)
     *   - Todos los documentos vigentes         → VIGENTE
     *
     * Este cálculo se hace cada vez que se consulta, nunca se almacena en la BD.
     * Así, cuando un documento vence, el estado cambia automáticamente sin que
     * nadie tenga que ejecutar un proceso ni actualizar un campo.
     */
    String calcularEstadoDocumental(Trabajador trabajador) {
        List<Documento> documentos = trabajador.getDocumentos();

        if (documentos.isEmpty()) {
            return "PENDIENTE_CARGA";
        }

        boolean algunoVencido = documentos.stream()
            .anyMatch(Documento::estaVencido);

        return algunoVencido ? "VENCIDO" : "VIGENTE";
    }
}
