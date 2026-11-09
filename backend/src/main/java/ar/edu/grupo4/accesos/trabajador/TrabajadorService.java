package ar.edu.grupo4.accesos.trabajador;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.common.paging.PageResponse;
import ar.edu.grupo4.accesos.common.storage.FileStorageService;
import ar.edu.grupo4.accesos.trabajador.dto.DocumentoRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorEditarRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorRequest;
import ar.edu.grupo4.accesos.trabajador.dto.TrabajadorResponse;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Reglas de negocio del módulo de trabajadores (US-25, US-26, US-28).
 *
 * Todas las operaciones trabajan sobre el perfil aprobado del usuario autenticado (UsuarioActual, T-03).
 * Ese perfilId NUNCA sale del request del frontend: se obtiene desde la BD a partir del token,
 * garantizando el aislamiento entre Empresas (RF-23, US-41).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrabajadorService {

    private final TrabajadorRepository trabajadorRepository;
    private final DocumentoRepository documentoRepository;
    private final FileStorageService fileStorageService;
    private final UsuarioActual usuarioActual;

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
    public TrabajadorResponse crear(TrabajadorRequest request) {
        Long perfilId = usuarioActual.perfilIdAprobado();

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
    // US-23: Vincular automáticamente a Monotributista como su único trabajador
    // ========================================================================================

    /**
     * Registra automáticamente al Monotributista como su propio y único trabajador
     * al ser aprobado su perfil por el Administrador (RF-09, RN-07).
     *
     * No comparte datos con otros registros aunque el DNI coincida (RN-08, RN-11).
     * Foto y documentación quedan pendientes para ser cargadas por el propio usuario (US-24, US-28).
     */
    @Transactional
    public Trabajador registrarMonotributista(Long perfilId, String nombre, String apellido, String dni) {
        Trabajador trabajador = new Trabajador();
        trabajador.setPerfilId(perfilId);
        trabajador.setNombre(nombre.trim());
        trabajador.setApellido(apellido.trim());
        trabajador.setDni(dni.trim());

        trabajador = trabajadorRepository.save(trabajador);
        log.info("Trabajador automático {} registrado para perfil Monotributista {}", trabajador.getId(), perfilId);
        return trabajador;
    }

    // ========================================================================================
    // US-26: Listar y modificar trabajadores
    // ========================================================================================

    /**
     * Listado paginado de los trabajadores del perfil del usuario autenticado (RNF-08).
     * Orden por defecto: alfabético por apellido (configurado en el Controller).
     */
    @Transactional(readOnly = true)
    public PageResponse<TrabajadorResponse> listar(Pageable pageable) {
        Long perfilId = usuarioActual.perfilIdAprobado();
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
    public TrabajadorResponse obtener(Long trabajadorId) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = buscarDelPerfil(perfilId, trabajadorId);
        return TrabajadorResponse.from(trabajador, calcularEstadoDocumental(trabajador));
    }

    /**
     * Modifica nombre y/o apellido de un trabajador existente.
     * Foto y documentación se modifican por separado (US-28, S2).
     */
    @Transactional
    public TrabajadorResponse editar(Long trabajadorId, TrabajadorEditarRequest request) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = buscarDelPerfil(perfilId, trabajadorId);

        trabajador.setNombre(request.nombre().trim());
        trabajador.setApellido(request.apellido().trim());

        return TrabajadorResponse.from(trabajador, calcularEstadoDocumental(trabajador));
    }

    // ========================================================================================
    // US-27: Dar de baja trabajador
    // ========================================================================================

    /**
     * Da de baja a un trabajador para el perfil del usuario autenticado (RF-12, RN-05).
     * Setea fecha_baja a now(), inhabilitando el vínculo para futuros ingresos.
     * La baja inhabilita únicamente este vínculo (RN-05).
     */
    @Transactional
    public TrabajadorResponse darDeBaja(Long trabajadorId) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = buscarDelPerfil(perfilId, trabajadorId);

        if (trabajador.estaDadoDeBaja()) {
            throw new BusinessException("TRABAJADOR_YA_DADO_DE_BAJA",
                "El trabajador ya se encuentra dado de baja.");
        }

        trabajador.setFechaBaja(Instant.now());
        trabajador = trabajadorRepository.save(trabajador);
        log.info("Trabajador {} dado de baja en perfil {}", trabajadorId, perfilId);

        return TrabajadorResponse.from(trabajador, calcularEstadoDocumental(trabajador));
    }

    // ========================================================================================
    // US-28: Cargar, actualizar, renovar y eliminar documentación
    // ========================================================================================

    /**
     * Carga o renueva un documento (ART o Seguro de Vida) para un trabajador.
     * Si ya existe un documento del mismo tipo, se reemplaza el archivo en disco
     * y se actualizan los datos en la BD.
     */
    @Transactional
    public TrabajadorResponse cargarDocumento(Long trabajadorId, DocumentoRequest request) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = buscarDelPerfil(perfilId, trabajadorId);

        // Sin archivo: mensaje propio de US-28 (validarPdf es común y usa uno genérico)
        if (request.archivo() == null || request.archivo().isEmpty()) {
            throw new BusinessException("ARCHIVO_OBLIGATORIO", "Debe adjuntar el archivo del documento", HttpStatus.BAD_REQUEST);
        }

        // Validación de tipo y tamaño del archivo PDF (T-05, RNF-10)
        fileStorageService.validarPdf(request.archivo());

        Optional<Documento> optDoc = documentoRepository.findByTrabajadorIdAndTipo(trabajadorId, request.tipo());
        String rutaVieja = optDoc.map(Documento::getArchivoRuta).orElse(null);

        // Subir nuevo archivo a disco: trabajadores/{id}/documentos/<uuid>.pdf
        String subdirectorio = "trabajadores/" + trabajadorId + "/documentos";
        String nuevaRuta = fileStorageService.guardar(request.archivo(), subdirectorio);

        Documento doc = optDoc.orElseGet(() -> {
            Documento nuevo = new Documento();
            nuevo.setTrabajador(trabajador);
            nuevo.setTipo(request.tipo());
            trabajador.getDocumentos().add(nuevo);
            return nuevo;
        });
        doc.setArchivoRuta(nuevaRuta);
        doc.setArchivoNombre(nombreOriginal(request.archivo()));
        doc.setFechaVencimiento(request.fechaVencimiento());

        try {
            documentoRepository.saveAndFlush(doc);
        } catch (RuntimeException e) {
            fileStorageService.eliminar(nuevaRuta);
            throw e;
        }

        // Si se reemplazó un documento anterior, borramos el archivo viejo de disco
        if (rutaVieja != null && !rutaVieja.equals(nuevaRuta)) {
            fileStorageService.eliminar(rutaVieja);
        }

        return TrabajadorResponse.from(trabajador, calcularEstadoDocumental(trabajador));
    }

    /**
     * Elimina un documento específico de un trabajador y su archivo de disco.
     */
    @Transactional
    public TrabajadorResponse eliminarDocumento(Long trabajadorId, TipoDocumento tipo) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = buscarDelPerfil(perfilId, trabajadorId);

        Documento doc = documentoRepository.findByTrabajadorIdAndTipo(trabajadorId, tipo)
            .orElseThrow(NotFoundException::new);

        String ruta = doc.getArchivoRuta();
        trabajador.getDocumentos().remove(doc);
        documentoRepository.delete(doc);
        documentoRepository.flush();
        fileStorageService.eliminar(ruta);

        return TrabajadorResponse.from(trabajador, calcularEstadoDocumental(trabajador));
    }

    /**
     * Descarga el archivo PDF de un documento legal de un trabajador.
     */
    @Transactional(readOnly = true)
    public Resource descargarDocumento(Long trabajadorId, TipoDocumento tipo) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        buscarDelPerfil(perfilId, trabajadorId);

        Documento doc = documentoRepository.findByTrabajadorIdAndTipo(trabajadorId, tipo)
            .orElseThrow(NotFoundException::new);

        return fileStorageService.cargar(doc.getArchivoRuta());
    }

    // ========================================================================================
    // US-25 / US-26: Carga, reemplazo, eliminación y descarga de Foto
    // ========================================================================================

    /**
     * Sube o reemplaza la foto del trabajador (JPG o PNG hasta 2 MB).
     */
    @Transactional
    public TrabajadorResponse cargarFoto(Long trabajadorId, MultipartFile foto) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = buscarDelPerfil(perfilId, trabajadorId);

        // Validación de tipo y tamaño de la foto (T-05, RNF-10)
        fileStorageService.validarFoto(foto);

        String rutaVieja = trabajador.getFotoRuta();
        String subdirectorio = "trabajadores/" + trabajadorId + "/foto";
        String nuevaRuta = fileStorageService.guardar(foto, subdirectorio);

        trabajador.setFotoRuta(nuevaRuta);

        try {
            trabajadorRepository.saveAndFlush(trabajador);
        } catch (RuntimeException e) {
            fileStorageService.eliminar(nuevaRuta);
            throw e;
        }

        if (rutaVieja != null && !rutaVieja.equals(nuevaRuta)) {
            fileStorageService.eliminar(rutaVieja);
        }

        return TrabajadorResponse.from(trabajador, calcularEstadoDocumental(trabajador));
    }

    /**
     * Elimina la foto del trabajador y borra el archivo de disco.
     */
    @Transactional
    public TrabajadorResponse eliminarFoto(Long trabajadorId) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = buscarDelPerfil(perfilId, trabajadorId);

        String rutaVieja = trabajador.getFotoRuta();
        if (rutaVieja != null) {
            trabajador.setFotoRuta(null);
            trabajadorRepository.saveAndFlush(trabajador);
            fileStorageService.eliminar(rutaVieja);
        }

        return TrabajadorResponse.from(trabajador, calcularEstadoDocumental(trabajador));
    }

    /**
     * Descarga la foto del trabajador.
     */
    @Transactional(readOnly = true)
    public Resource descargarFoto(Long trabajadorId) {
        Long perfilId = usuarioActual.perfilIdAprobado();
        Trabajador trabajador = buscarDelPerfil(perfilId, trabajadorId);

        if (!trabajador.tieneFoto()) {
            throw new NotFoundException();
        }

        return fileStorageService.cargar(trabajador.getFotoRuta());
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
     * Nombre con el que la Empresa subió el archivo, para mostrarlo en la edición (US-26 CA-3).
     * Se queda solo con el nombre (sin carpetas que mande el navegador) y lo recorta al largo de la columna.
     */
    private static String nombreOriginal(MultipartFile archivo) {
        String nombre = StringUtils.getFilename(archivo.getOriginalFilename());
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        nombre = nombre.substring(nombre.lastIndexOf('\\') + 1).strip();
        return nombre.length() > 255 ? nombre.substring(nombre.length() - 255) : nombre;
    }

    /**
     * Calcula el estado documental de un trabajador en base a sus documentos (RN-03, RN-04, RF-10, RF-18).
     *
     * Regla de negocio (US-28: "ART y/o Seguro de Vida"):
     *   - Sin documentos cargados → PENDIENTE_CARGA
     *   - Algún documento cargado con vencimiento < hoy → VENCIDO (se toma el vencimiento más próximo)
     *   - Al menos uno cargado y todos vigentes → VIGENTE
     *
     * Es la misma regla que aplica la garita en la función verificar_acceso (paso 6).
     *
     * Este cálculo se hace cada vez que se consulta, nunca se almacena en la BD (RNF-04).
     * Así, cuando un documento vence, el estado cambia automáticamente sin que
     * nadie tenga que ejecutar un proceso batch ni actualizar un campo.
     */
    String calcularEstadoDocumental(Trabajador trabajador) {
        List<Documento> documentos = trabajador.getDocumentos();

        if (documentos == null || documentos.isEmpty()) {
            return "PENDIENTE_CARGA";
        }

        // Si alguno de los documentos cargados está vencido, no habilita el ingreso (RN-03)
        boolean algunoVencido = documentos.stream()
            .anyMatch(Documento::estaVencido);

        return algunoVencido ? "VENCIDO" : "VIGENTE";
    }
}
