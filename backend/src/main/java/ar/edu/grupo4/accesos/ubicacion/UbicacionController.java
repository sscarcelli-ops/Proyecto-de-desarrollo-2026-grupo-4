package ar.edu.grupo4.accesos.ubicacion;

import ar.edu.grupo4.accesos.ubicacion.dto.LocalidadResponse;
import ar.edu.grupo4.accesos.ubicacion.dto.ProvinciaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * T-11: catálogo para los selectores de provincia y localidad. Con sesión, para cualquier rol: son datos públicos
 * y los puede usar cualquier formulario que pida una ubicación. Sin paginar: una provincia tiene a lo sumo
 * algunos cientos de localidades y el selector las filtra mientras se escribe.
 */
@RestController
@RequestMapping("/api/ubicaciones")
@RequiredArgsConstructor
public class UbicacionController {

    private final UbicacionService ubicacionService;

    @GetMapping("/provincias")
    public List<ProvinciaResponse> provincias() {
        return ubicacionService.listarProvincias().stream().map(ProvinciaResponse::from).toList();
    }

    @GetMapping("/provincias/{provinciaId}/localidades")
    public List<LocalidadResponse> localidades(@PathVariable String provinciaId) {
        return ubicacionService.listarLocalidades(provinciaId).stream().map(LocalidadResponse::from).toList();
    }
}
