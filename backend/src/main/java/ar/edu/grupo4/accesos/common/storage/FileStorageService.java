package ar.edu.grupo4.accesos.common.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * T-05: servicio transversal de almacenamiento de archivos.
 * Implementado sobre disco local (base-path configurable), desacoplando los controladores
 * y servicios de la infraestructura de almacenamiento.
 */
public interface FileStorageService {

    /**
     * Guarda un archivo MultipartFile y devuelve su ruta relativa normalizada.
     *
     * @param archivo      archivo recibido en la petición
     * @param subdirectorio carpeta lógica (ej: "perfiles/1/constancia")
     * @return ruta relativa guardada (ej: "perfiles/1/constancia/uuid.pdf")
     */
    String guardar(MultipartFile archivo, String subdirectorio);

    /**
     * Carga un archivo almacenado como recurso descargable/legible.
     *
     * @param rutaRelativa ruta relativa almacenada en la base de datos
     * @return Resource accesible
     */
    Resource cargar(String rutaRelativa);

    /**
     * Elimina un archivo si existe (para reemplazos o rollback).
     *
     * @param rutaRelativa ruta relativa almacenada
     */
    void eliminar(String rutaRelativa);
}
