package ar.edu.grupo4.accesos.ubicacion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Provincia de Argentina (T-11). El id es el código de Georef (ej. "06" Buenos Aires). */
@Entity
@Table(name = "provincia")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Provincia {

    @Id
    @Column(length = 2)
    private String id;

    @Column(nullable = false, length = 100)
    private String nombre;
}
