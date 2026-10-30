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

    private final Path rootLocation;

    public DiscoFileStorageService(@Value("${app.storage.base-path:./uploads}") String basePath) {
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
}
