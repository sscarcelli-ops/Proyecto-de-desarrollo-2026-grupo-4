package ar.edu.grupo4.accesos.common.storage;

import ar.edu.grupo4.accesos.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validaciones comunes de archivos (T-05, RNF-10): tipo real por encabezado y tamaño máximo.
 * Las usan la constancia (US-03), los documentos (US-28) y la foto del trabajador (US-25, US-26).
 */
class DiscoFileStorageServiceTest {

    private static final byte[] PDF = "%PDF-1.4 prueba".getBytes();
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};

    @TempDir
    Path raiz;

    private DiscoFileStorageService servicio() {
        return new DiscoFileStorageService(raiz.toString());
    }

    private static byte[] relleno(byte[] inicio, int largo) {
        return Arrays.copyOf(inicio, largo);
    }

    @Test
    @DisplayName("PDF real de hasta 8 MB pasa")
    void pdfValido() {
        assertThatCode(() -> servicio().validarPdf(new MockMultipartFile("a", "a.pdf", "application/pdf", PDF)))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Archivo con extensión .pdf pero sin encabezado %PDF se rechaza")
    void pdfFalso() {
        var archivo = new MockMultipartFile("a", "a.pdf", "application/pdf", "hola".getBytes());
        assertThatThrownBy(() -> servicio().validarPdf(archivo))
            .isInstanceOf(BusinessException.class)
            .hasMessage("El archivo debe ser un PDF de hasta 8 MB");
    }

    @Test
    @DisplayName("PDF de más de 8 MB se rechaza")
    void pdfGrande() {
        var archivo = new MockMultipartFile("a", "a.pdf", "application/pdf", relleno(PDF, 8 * 1024 * 1024 + 1));
        assertThatThrownBy(() -> servicio().validarPdf(archivo))
            .isInstanceOf(BusinessException.class)
            .hasMessage("El archivo debe ser un PDF de hasta 8 MB");
    }

    @Test
    @DisplayName("PDF real con otra extensión se rechaza")
    void pdfOtraExtension() {
        var archivo = new MockMultipartFile("a", "a.png", "image/png", PDF);
        assertThatThrownBy(() -> servicio().validarPdf(archivo))
            .isInstanceOf(BusinessException.class)
            .hasMessage("El archivo debe ser un PDF de hasta 8 MB");
    }

    @Test
    @DisplayName("Foto PNG o JPG real de hasta 2 MB pasa")
    void fotoValida() {
        assertThatCode(() -> {
            servicio().validarFoto(new MockMultipartFile("f", "f.png", "image/png", PNG));
            servicio().validarFoto(new MockMultipartFile("f", "f.jpg", "image/jpeg", JPG));
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Foto con extensión .png pero sin encabezado de imagen se rechaza")
    void fotoFalsa() {
        var archivo = new MockMultipartFile("f", "f.png", "image/png", PDF);
        assertThatThrownBy(() -> servicio().validarFoto(archivo))
            .isInstanceOf(BusinessException.class)
            .hasMessage("La foto debe ser JPG o PNG de hasta 2 MB");
    }

    @Test
    @DisplayName("Foto de más de 2 MB se rechaza")
    void fotoGrande() {
        var archivo = new MockMultipartFile("f", "f.png", "image/png", relleno(PNG, 2 * 1024 * 1024 + 1));
        assertThatThrownBy(() -> servicio().validarFoto(archivo))
            .isInstanceOf(BusinessException.class)
            .hasMessage("La foto debe ser JPG o PNG de hasta 2 MB");
    }
}
