package ar.edu.grupo4.accesos.barrio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos propios del rol Operador (tabla operador). La cuenta (email y contraseña) vive en usuario: comparten el id.
 * El barrio que tiene a cargo se guarda solo en barrio.operador_id (RN-10).
 */
@Entity
@Table(name = "operador")
@Getter
@Setter
@NoArgsConstructor
public class Operador {

    @Id
    @Column(name = "usuario_id")
    private Long usuarioId;

    private String nombre;

    private String apellido;

    private String telefono;

    public Operador(Long usuarioId, String nombre, String apellido, String telefono) {
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
    }
}
