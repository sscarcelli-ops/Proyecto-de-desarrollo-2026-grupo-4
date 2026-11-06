package ar.edu.grupo4.accesos.trabajador.dto;

import ar.edu.grupo4.accesos.trabajador.TipoDocumento;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * Datos requeridos para la carga o renovación de un documento legal (ART o Seguro de Vida) (US-28).
 * Se recibe como multipart/form-data.
 * La validación de que el archivo no esté vacío y cumpla el formato se realiza en FileStorageService.validarPdf.
 */
public record DocumentoRequest(
    @NotNull(message = "El tipo de documento es obligatorio")
    TipoDocumento tipo,

    MultipartFile archivo,

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate fechaVencimiento
) {}
