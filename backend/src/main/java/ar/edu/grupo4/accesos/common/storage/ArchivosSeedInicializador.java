package ar.edu.grupo4.accesos.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Datos de prueba (perfil seed): las constancias, documentos y fotos del seed apuntan a archivos que no existen en
 * una instalación nueva. Al arrancar se copia un PDF de ejemplo en cada ruta .pdf y una foto de ejemplo en cada ruta
 * .jpg, así se pueden abrir y descargar como cualquier archivo subido. Solo existe con el perfil seed: en producción no hay nada de esto.
 */
@Component
@Profile("seed")
@Slf4j
public class ArchivosSeedInicializador implements ApplicationRunner {

    static final String PDF_DE_PRUEBA = "seed/archivo-prueba.pdf";
    static final String FOTO_DE_PRUEBA = "seed/foto-prueba.jpg";

    private static final String RUTAS_DEL_SEED = """
        SELECT constancia_ruta FROM perfil WHERE constancia_ruta LIKE 'perfiles/seed/%'
        UNION
        SELECT archivo_ruta FROM documento WHERE archivo_ruta LIKE 'trabajadores/seed/%'
        UNION
        SELECT foto_ruta FROM trabajador WHERE foto_ruta LIKE 'trabajadores/seed/%'
        """;

    private final JdbcTemplate jdbc;
    private final Path raiz;
    private final Resource pdfDePrueba;
    private final Resource fotoDePrueba;

    @Autowired
    public ArchivosSeedInicializador(JdbcTemplate jdbc, @Value("${app.storage.base-path}") String basePath) {
        this(jdbc, basePath, new ClassPathResource(PDF_DE_PRUEBA), new ClassPathResource(FOTO_DE_PRUEBA));
    }

    ArchivosSeedInicializador(JdbcTemplate jdbc, String basePath, Resource pdfDePrueba, Resource fotoDePrueba) {
        this.jdbc = jdbc;
        this.raiz = Paths.get(basePath).toAbsolutePath().normalize();
        this.pdfDePrueba = pdfDePrueba;
        this.fotoDePrueba = fotoDePrueba;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        List<String> rutas = jdbc.queryForList(RUTAS_DEL_SEED, String.class);
        int copiados = 0;
        for (String ruta : rutas) {
            Path destino = raiz.resolve(ruta).normalize();
            Resource ejemplo = ruta.endsWith(".pdf") ? pdfDePrueba : ruta.endsWith(".jpg") ? fotoDePrueba : null;
            // Solo PDF o JPG, dentro de la carpeta de archivos y sin pisar uno que ya exista
            if (ejemplo == null || !destino.startsWith(raiz) || Files.exists(destino)) {
                continue;
            }
            Files.createDirectories(destino.getParent());
            try (InputStream origen = ejemplo.getInputStream()) {
                Files.copy(origen, destino);
            }
            copiados++;
        }
        if (copiados > 0) {
            log.info("Datos de prueba: se copiaron {} archivos de ejemplo en {}", copiados, raiz);
        }
    }
}
