package ar.edu.grupo4.accesos.barrio;

import ar.edu.grupo4.accesos.ubicacion.Localidad;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Barrio privado. El vínculo con el Operador vive solo en operador_id (RN-10). */
@Entity
@Table(name = "barrio")
@Getter
@Setter
@NoArgsConstructor
public class Barrio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String nombre;

    // T-11: localidad del catálogo; con ella viene la provincia. Puede faltar solo en barrios anteriores a T-11 cuyo
    // texto no coincidió con el catálogo: el alta y la edición siempre la piden.
    @ManyToOne
    @JoinColumn(name = "localidad_id")
    private Localidad localidad;

    @Column(nullable = false, length = 120)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoBarrio estado = EstadoBarrio.INACTIVO;

    // Se pasa a @ManyToOne cuando exista la entidad Operador (US-13 / US-45).
    @Column(name = "operador_id")
    private Long operadorId;

    public Barrio(String nombre, Localidad localidad, String direccion) {
        this.nombre = nombre;
        this.localidad = localidad;
        this.direccion = direccion;
    }
}
