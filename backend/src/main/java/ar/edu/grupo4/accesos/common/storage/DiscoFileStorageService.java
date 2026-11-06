package ar.edu.grupo4.accesos.common.storage;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import ar.edu.grupo4.accesos.common.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * T-05: implementación en disco de FileStorageService.
 * Almacena en la ruta base configurada por app.storage.base-path (${STORAGE_PATH}).
 */
@Service
@Slf4j
public class DiscoFileStorageService implements FileStorageService {

    private static final long MAX_PDF_SIZE_BYTES = 8 * 1024 * 1024; // 8 MB (RNF-10)
    private static final byte[] PDF_MAGIC_BYTES = new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF

    private static final long MAX_FOTO_SIZE_BYTES = 2 * 1024 * 1024; // 2 MB (RNF-10)
    private static final byte[] JPG_MAGIC_BYTES = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};

    private final Path rootLocation;

    public DiscoFileStorageService(@Value("${app.storage.base-path}") String basePath) {
        this.rootLocation = Paths.get(basePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            log.error("No se pudo inicializar el directorio base de almacenamiento: {}", this.rootLocation, e);
            throw new IllegalStateException("No se pudo crear el directorio de almacenamiento", e);
        }
    }

    @Override
    public String guardar(MultipartFile archivo, String subdirectorio) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("ARCHIVO_VACIO", "El archivo no puede estar vacío", HttpStatus.BAD_REQUEST);
        }

        String extensionOriginal = StringUtils.getFilenameExtension(archivo.getOriginalFilename());
        String extension = extensionOriginal != null ? "." + extensionOriginal.toLowerCase() : "";
        String nombreUnico = UUID.randomUUID() + extension;

        Path destinoDir = this.rootLocation.resolve(subdirectorio).normalize();
        if (!destinoDir.startsWith(this.rootLocation)) {
            throw new BusinessException("PATH_INVALIDO", "Intento de escape de directorio no permitido", HttpStatus.BAD_REQUEST);
        }

        try {
            Files.createDirectories(destinoDir);
            Path destinoArchivo = destinoDir.resolve(nombreUnico).normalize();
            try (InputStream inputStream = archivo.getInputStream()) {
                Files.copy(inputStream, destinoArchivo, StandardCopyOption.REPLACE_EXISTING);
            }
            // Devolvemos la ruta relativa normalizada con '/'
            return Paths.get(subdirectorio, nombreUnico).toString().replace('\\', '/');
        } catch (IOException e) {
            log.error("Error al guardar archivo en {}", subdirectorio, e);
            throw new BusinessException("STORAGE_ERROR", "No se pudo guardar el archivo adjunto", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Resource cargar(String rutaRelativa) {
        try {
            Path archivo = this.rootLocation.resolve(rutaRelativa).normalize();
            if (!archivo.startsWith(this.rootLocation)) {
                throw new BusinessException("PATH_INVALIDO", "Ruta de archivo no permitida", HttpStatus.BAD_REQUEST);
            }
            Resource resource = new UrlResource(archivo.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new NotFoundException();
        } catch (MalformedURLException e) {
            throw new NotFoundException();
        }
    }

    @Override
    public void eliminar(String rutaRelativa) {
        if (rutaRelativa == null || rutaRelativa.isBlank()) {
            return;
        }
        try {
            Path archivo = this.rootLocation.resolve(rutaRelativa).normalize();
            if (archivo.startsWith(this.rootLocation)) {
                Files.deleteIfExists(archivo);
            }
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo: {}", rutaRelativa, e);
        }
    }

    @Override
    public void validarPdf(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("ARCHIVO_VACIO", "El archivo no puede estar vacío", HttpStatus.BAD_REQUEST);
        }

        if (archivo.getSize() > MAX_PDF_SIZE_BYTES) {
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
            log.error("Error al leer archivo PDF", e);
            throw new BusinessException("ARCHIVO_FORMATO", "El archivo debe ser un PDF de hasta 8 MB", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public void validarFoto(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("ARCHIVO_VACIO", "El archivo no puede estar vacío", HttpStatus.BAD_REQUEST);
        }

        if (archivo.getSize() > MAX_FOTO_SIZE_BYTES) {
            throw new BusinessException("ARCHIVO_TAMANIO", "La foto debe ser JPG o PNG de hasta 2 MB", HttpStatus.BAD_REQUEST);
        }

        String originalName = archivo.getOriginalFilename();
        String extension = originalName != null ? StringUtils.getFilenameExtension(originalName) : null;
        if (extension == null || (!extension.equalsIgnoreCase("jpg") && !extension.equalsIgnoreCase("jpeg") && !extension.equalsIgnoreCase("png"))) {
            throw new BusinessException("ARCHIVO_FORMATO", "La foto debe ser JPG o PNG de hasta 2 MB", HttpStatus.BAD_REQUEST);
        }

        // Validación estricta de magic bytes para JPG (FF D8 FF) y PNG (89 50 4E 47)
        try (InputStream is = archivo.getInputStream()) {
            byte[] header = new byte[4];
            int read = is.read(header);
            boolean isJpg = read >= 3 && header[0] == JPG_MAGIC_BYTES[0]
                                      && header[1] == JPG_MAGIC_BYTES[1]
                                      && header[2] == JPG_MAGIC_BYTES[2];
            boolean isPng = read >= 4 && header[0] == PNG_MAGIC_BYTES[0]
                                      && header[1] == PNG_MAGIC_BYTES[1]
                                      && header[2] == PNG_MAGIC_BYTES[2]
                                      && header[3] == PNG_MAGIC_BYTES[3];
            if (!isJpg && !isPng) {
                throw new BusinessException("ARCHIVO_FORMATO", "La foto debe ser JPG o PNG de hasta 2 MB", HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            log.error("Error al leer archivo de foto", e);
            throw new BusinessException("ARCHIVO_FORMATO", "La foto debe ser JPG o PNG de hasta 2 MB", HttpStatus.BAD_REQUEST);
        }
    }
}
