package ar.edu.grupo4.accesos.trabajador;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro de un trabajador dentro de una Empresa o Monotributista (perfil).
 *
 * Cada perfil carga sus propios datos de forma independiente (RN-11): aunque dos
 * Empresas registren el mismo DNI, cada registro es propio y no comparte nombre,
 * foto ni documentación. Por eso el UNIQUE es (perfil_id, dni), no solo (dni).
 *
 * El vínculo perfil–trabajador es lo que habilita o inhabilita al trabajador para
 * ingresar bajo esa Empresa/Monotributista. La baja (fecha_baja != null) corta
 * ese vínculo sin borrar el registro, y es independiente de la documentación y
 * de la habilitación del perfil (RN-05).
 *
 * El estado documental (VIGENTE / VENCIDO / PENDIENTE_CARGA) NO se almacena:
 * se calcula a partir de los documentos cargados y sus fechas de vencimiento
 * cada vez que se consulta (RNF-04). Esa lógica vive en TrabajadorService.
 */
@Entity
@Table(name = "trabajador")
@Getter @Setter @NoArgsConstructor
public class Trabajador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con el perfil (Empresa o Monotributista) como ID plano, no como entidad,
    // porque Perfil vive en otro módulo y la arquitectura prohíbe dependencias cruzadas
    // de entidades entre módulos. La FK la garantiza la BD.
    @Column(name = "perfil_id", nullable = false, updatable = false)
    private Long perfilId;

    private String nombre;

    private String apellido;

    private String dni;

    // Ruta relativa al STORAGE_PATH configurado (ej: "trabajadores/42/foto/uuid.jpg").
    // null = sin foto. El Guardia deberá solicitar el DNI físico en la garita (RF-10).
    @Column(name = "foto_ruta")
    private String fotoRuta;

    // null = activo en este vínculo. Con fecha = dado de baja (US-27, RN-05).
    // La baja es por vínculo: el trabajador puede seguir ingresando por otro perfil.
    @Column(name = "fecha_baja")
    private Instant fechaBaja;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    // Documentos cargados (ART y/o Seguro de Vida). La lista puede estar vacía
    // (estado documental = PENDIENTE_CARGA) o tener 1 o 2 documentos.
    // LAZY: no se cargan de la BD salvo que se acceda a la lista.
    @OneToMany(mappedBy = "trabajador", fetch = FetchType.LAZY)
    private List<Documento> documentos = new ArrayList<>();

    @PrePersist
    void prePersist() {
        this.creadoEn = Instant.now();
    }

    public boolean estaDadoDeBaja() {
        return this.fechaBaja != null;
    }

    public boolean tieneFoto() {
        return this.fotoRuta != null;
    }
}
