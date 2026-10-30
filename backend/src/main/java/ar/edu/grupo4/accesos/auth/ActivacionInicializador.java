package ar.edu.grupo4.accesos.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** US-01: al arrancar, carga el código de activación del .env (ACTIVACION_TOKEN). Ver ActivacionService. */
@Component
@RequiredArgsConstructor
public class ActivacionInicializador implements ApplicationRunner {

    private final ActivacionService activacionService;
    private final ActivacionProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        activacionService.cargarCodigoConfigurado(properties.getToken(), properties.getVigenciaDias());
    }
}
