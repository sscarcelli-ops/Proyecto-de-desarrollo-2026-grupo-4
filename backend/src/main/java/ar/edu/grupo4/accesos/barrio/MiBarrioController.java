package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.barrio.dto.MiBarrioResponse;
import ar.edu.grupo4.accesos.usuario.UsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Barrio del usuario de la sesión: el que tiene a cargo el Operador o donde trabaja el Guardia (US-42).
 * Lo usan las pantallas del Operador para mostrar el nombre del barrio arriba (wireframes de US-33 y US-34).
 */
@RestController
@RequestMapping("/api/barrio")
@PreAuthorize("hasAnyRole('OPERADOR', 'GUARDIA')")
@RequiredArgsConstructor
public class MiBarrioController {

    private final BarrioService barrioService;
    private final UsuarioActual usuarioActual;

    @GetMapping
    public MiBarrioResponse obtener() {
        return MiBarrioResponse.from(barrioService.obtener(usuarioActual.barrioId()));
    }
}
