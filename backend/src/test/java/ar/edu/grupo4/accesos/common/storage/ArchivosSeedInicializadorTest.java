package ar.edu.grupo4.accesos.common.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArchivosSeedInicializadorTest {

    private static final byte[] PDF = "%PDF-1.4 prueba".getBytes();
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};

    @TempDir
    Path raiz;

    private ArchivosSeedInicializador inicializador(List<String> rutas) {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(rutas);
        return new ArchivosSeedInicializador(jdbc, raiz.toString(), new ByteArrayResource(PDF), new ByteArrayResource(JPG));
    }

    @Test
    void copiaElPdfDePruebaEnCadaRutaDelSeed() throws Exception {
        inicializador(List.of("perfiles/seed/empresa/constancia.pdf", "trabajadores/seed/30111222/art.pdf")).run(null);

        assertThat(raiz.resolve("perfiles/seed/empresa/constancia.pdf")).hasBinaryContent(PDF);
        assertThat(raiz.resolve("trabajadores/seed/30111222/art.pdf")).hasBinaryContent(PDF);
    }

    @Test
    void copiaLaFotoDePruebaEnCadaFotoDelSeed() throws Exception {
        inicializador(List.of("trabajadores/seed/30444555/foto.jpg")).run(null);

        assertThat(raiz.resolve("trabajadores/seed/30444555/foto.jpg")).hasBinaryContent(JPG);
    }

    @Test
    void noPisaUnArchivoQueYaExiste() throws Exception {
        Path existente = raiz.resolve("perfiles/seed/empresa/constancia.pdf");
        Files.createDirectories(existente.getParent());
        Files.writeString(existente, "subido a mano");

        inicializador(List.of("perfiles/seed/empresa/constancia.pdf")).run(null);

        assertThat(existente).hasContent("subido a mano");
    }

    @Test
    void ignoraRutasDeOtroTipoOQueSalenDeLaCarpeta() throws Exception {
        inicializador(List.of("trabajadores/seed/30444555/foto.gif", "perfiles/seed/../../afuera.pdf")).run(null);

        assertThat(raiz.resolve("trabajadores/seed/30444555/foto.gif")).doesNotExist();
        assertThat(raiz.getParent().resolve("afuera.pdf")).doesNotExist();
    }
}
