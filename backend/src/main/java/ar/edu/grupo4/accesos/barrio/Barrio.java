package ar.edu.grupo4.accesos.barrio;

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

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "localidad_provincia", nullable = false, length = 150)
    private String localidadProvincia;

    @Column(nullable = false, length = 255)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoBarrio estado = EstadoBarrio.INACTIVO;

    // Se pasa a @ManyToOne cuando exista la entidad Operador (US-13 / US-45).
    @Column(name = "operador_id")
    private Long operadorId;

    public Barrio(String nombre, String localidadProvincia, String direccion) {
        this.nombre = nombre;
        this.localidadProvincia = localidadProvincia;
        this.direccion = direccion;
    }
}
