package ar.edu.grupo4.accesos.ubicacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Localidad de Argentina (T-11). El id es el código de Georef (ej. "06070010" Baradero). */
@Entity
@Table(name = "localidad")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Localidad {

    @Id
    @Column(length = 10)
    private String id;

    @Column(nullable = false, length = 100)
    private String nombre;

    /** Partido o departamento: distingue localidades con el mismo nombre en una provincia. */
    @Column(nullable = false, length = 100)
    private String departamento;

    // Se usa siempre junto con la localidad ("Buenos Aires, Baradero"): se carga con ella
    @ManyToOne(optional = false)
    @JoinColumn(name = "provincia_id", nullable = false)
    private Provincia provincia;
}
