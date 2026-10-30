package ar.edu.grupo4.accesos.perfil;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import ar.edu.grupo4.accesos.common.storage.FileStorageService;
import ar.edu.grupo4.accesos.perfil.dto.PerfilResponse;
import ar.edu.grupo4.accesos.perfil.dto.SolicitarPerfilRequest;
import ar.edu.grupo4.accesos.usuario.Usuario;
import ar.edu.grupo4.accesos.usuario.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerfilService {

    private static final long MAX_FILE_SIZE_BYTES = 8 * 1024 * 1024; // 8 MB (RNF-10)
    private static final byte[] PDF_MAGIC_BYTES = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final Pattern PATTERN_CUIT_CUIL = Pattern.compile("^\\d{2}-?\\d{8}-?\\d$");
    private static final Pattern PATTERN_DNI = Pattern.compile("^\\d{7,8}$");

    private final PerfilRepository perfilRepository;
    private final UsuarioService usuarioService;
    private final FileStorageService fileStorageService;

    /**
     * US-03: Solicita perfil de Empresa o Monotributista para el usuario autenticado.
     */
    @Transactional
    public PerfilResponse solicitar(Long usuarioId, SolicitarPerfilRequest request) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId)
            .orElseThrow(NotFoundException::new);

        // Bloqueo multirol: quien ya tiene roles no puede solicitar perfil (Admin, Operador, Guardia, etc.)
        if (!usuario.getRoles().isEmpty()) {
            throw new BusinessException("USUARIO_CON_ROL",
                "Los usuarios con rol asignado no pueden solicitar un perfil", HttpStatus.FORBIDDEN);
        }

        if (perfilRepository.existsByUsuarioId(usuarioId)) {
            throw new BusinessException("SOLICITUD_DUPLICADA",
                "Ya posees una solicitud de perfil registrada");
        }

        validarArchivoConstancia(request.constancia());

        Perfil perfil;
        if (request.tipo() == TipoPerfil.EMPRESA) {
            perfil = procesarEmpresa(usuario, request);
        } else if (request.tipo() == TipoPerfil.MONOTRIBUTISTA) {
            perfil = procesarMonotributista(usuario, request);
        } else {
            throw new BusinessException("TIPO_INVALIDO", "Tipo de perfil inválido", HttpStatus.BAD_REQUEST);
        }

        // Subir constancia a disco (perfiles/{usuarioId}/constancia)
        String subdirectorio = "perfiles/" + usuarioId + "/constancia";
        String rutaArchivo = fileStorageService.guardar(request.constancia(), subdirectorio);
        perfil.setConstanciaRuta(rutaArchivo);

        if (request.telefono() != null && !request.telefono().isBlank()) {
            perfil.setTelefono(request.telefono().trim());
        }

        // saveAndFlush: si la base rechaza el insert (ej. CUIT cargado a la vez por otra cuenta), el error sale acá
        // y se borra la constancia para no dejar archivos sin solicitud
        try {
            Perfil guardado = perfilRepository.saveAndFlush(perfil);
            return PerfilResponse.from(guardado);
        } catch (RuntimeException e) {
            fileStorageService.eliminar(rutaArchivo);
            throw e;
        }
    }

    /**
     * Consulta el perfil del usuario autenticado (para redirección o vista de estado).
     */
    @Transactional(readOnly = true)
    public PerfilResponse obtenerPorUsuarioId(Long usuarioId) {
        return perfilRepository.findByUsuarioId(usuarioId)
            .map(PerfilResponse::from)
            .orElseThrow(NotFoundException::new);
    }

    private Perfil procesarEmpresa(Usuario usuario, SolicitarPerfilRequest request) {
        if (request.cuit() == null || request.cuit().isBlank()) {
            throw new BusinessException("CAMPO_OBLIGATORIO", "El CUIT es obligatorio", HttpStatus.BAD_REQUEST);
        }
        String cuit = normalizarCuitCuil(request.cuit());
        if (!PATTERN_CUIT_CUIL.matcher(request.cuit().trim()).matches()) {
            throw new BusinessException("FORMATO_INVALIDO", "Formato de CUIT no válido", HttpStatus.BAD_REQUEST);
        }
        if (perfilRepository.existsByCuitCuil(cuit)) {
            throw new BusinessException("CUIT_DUPLICADO", "Este CUIT ya se encuentra registrado");
        }

        if (request.razonSocial() == null || request.razonSocial().isBlank()) {
            throw new BusinessException("CAMPO_OBLIGATORIO", "La razón social es obligatoria", HttpStatus.BAD_REQUEST);
        }

        Perfil p = new Perfil();
        p.setUsuario(usuario);
        p.setTipo(TipoPerfil.EMPRESA);
        p.setEstado(EstadoPerfil.PENDIENTE);
        p.setCuitCuil(cuit);
        p.setRazonSocial(request.razonSocial().trim());
        return p;
    }

    private Perfil procesarMonotributista(Usuario usuario, SolicitarPerfilRequest request) {
        if (request.cuil() == null || request.cuil().isBlank()) {
            throw new BusinessException("CAMPO_OBLIGATORIO", "El CUIL es obligatorio", HttpStatus.BAD_REQUEST);
        }
        String cuil = normalizarCuitCuil(request.cuil());
        if (!PATTERN_CUIT_CUIL.matcher(request.cuil().trim()).matches()) {
            throw new BusinessException("FORMATO_INVALIDO", "Formato de CUIL no válido", HttpStatus.BAD_REQUEST);
        }
        if (perfilRepository.existsByCuitCuil(cuil)) {
            throw new BusinessException("CUIL_DUPLICADO", "Este CUIL ya se encuentra registrado");
        }

        if (request.nombre() == null || request.nombre().isBlank()) {
            throw new BusinessException("CAMPO_OBLIGATORIO", "El nombre es obligatorio", HttpStatus.BAD_REQUEST);
        }
        if (request.apellido() == null || request.apellido().isBlank()) {
            throw new BusinessException("CAMPO_OBLIGATORIO", "El apellido es obligatorio", HttpStatus.BAD_REQUEST);
        }
        if (request.dni() == null || request.dni().isBlank()) {
            throw new BusinessException("CAMPO_OBLIGATORIO", "El DNI es obligatorio", HttpStatus.BAD_REQUEST);
        }

        String dni = request.dni().trim();
        if (!PATTERN_DNI.matcher(dni).matches()) {
            throw new BusinessException("FORMATO_INVALIDO", "Formato de DNI no válido", HttpStatus.BAD_REQUEST);
        }

        if (perfilRepository.existsByDniTitularAndTipo(dni, TipoPerfil.MONOTRIBUTISTA)) {
            throw new BusinessException("DNI_DUPLICADO", "Este DNI ya está asociado a un Monotributista");
        }

        Perfil p = new Perfil();
        p.setUsuario(usuario);
        p.setTipo(TipoPerfil.MONOTRIBUTISTA);
        p.setEstado(EstadoPerfil.PENDIENTE);
        p.setCuitCuil(cuil);
        p.setNombre(request.nombre().trim());
        p.setApellido(request.apellido().trim());
        p.setDniTitular(dni);
        return p;
    }

    private void validarArchivoConstancia(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("CONSTANCIA_OBLIGATORIA", "Debe adjuntar la constancia de inscripción", HttpStatus.BAD_REQUEST);
        }

        if (archivo.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("ARCHIVO_TAMANIO", "El archivo debe ser un PDF de hasta 8 MB", HttpStatus.BAD_REQUEST);
        }

        String originalName = archivo.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException("ARCHIVO_FORMATO", "El archivo debe ser un PDF de hasta 8 MB", HttpStatus.BAD_REQUEST);
        }

        // Validación estricta del encabezado mágico %PDF
        try (InputStream is = archivo.getInputStream()) {
            byte[] header = new byte[4];
            int read = is.read(header);
            if (read < 4 || header[0] != PDF_MAGIC_BYTES[0]
                         || header[1] != PDF_MAGIC_BYTES[1]
                         || header[2] != PDF_MAGIC_BYTES[2]
                         || header[3] != PDF_MAGIC_BYTES[3]) {
                throw new BusinessException("ARCHIVO_FORMATO", "El archivo debe ser un PDF de hasta 8 MB", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            log.error("Error al leer archivo de constancia", e);
            throw new BusinessException("ARCHIVO_FORMATO", "El archivo debe ser un PDF de hasta 8 MB", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizarCuitCuil(String valor) {
        String limpio = valor.replaceAll("[^0-9]", "");
        if (limpio.length() == 11) {
            return limpio.substring(0, 2) + "-" + limpio.substring(2, 10) + "-" + limpio.substring(10);
        }
        return valor.trim();
    }
}
