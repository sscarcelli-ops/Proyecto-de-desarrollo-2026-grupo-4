package ar.edu.grupo4.accesos.guardia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos propios del rol Guardia (tabla guardia). La cuenta (email y contraseña) vive en usuario: comparten el id.
 * La baja es lógica y reversible (RN-09): habilitado = false.
 */
@Entity
@Table(name = "guardia")
@Getter
@Setter
@NoArgsConstructor
public class Guardia {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "barrio_id")
    private Long barrioId;

    private String nombre;

    private String apellido;

    private boolean habilitado = true;

    public Guardia(Long usuarioId, Long barrioId, String nombre, String apellido) {
        this.usuarioId = usuarioId;
        this.barrioId = barrioId;
        this.nombre = nombre;
        this.apellido = apellido;
    }
}
