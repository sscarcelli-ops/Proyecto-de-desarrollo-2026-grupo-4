package ar.edu.grupo4.accesos.trabajador;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Documento legal de un trabajador: póliza de ART o Seguro de Vida.
 *
 * La vigencia se determina por la fecha declarada por la Empresa/Monotributista (RN-04),
 * tomando el vencimiento más próximo entre los documentos cargados del trabajador.
 * El cálculo se hace en tiempo real al consultar, no se persiste (RNF-04).
 *
 * Cada trabajador tiene como máximo un documento por tipo (UNIQUE trabajador_id + tipo).
 * Renovar un documento reemplaza el archivo anterior sin historial (decisiones técnicas §3.9).
 */
@Entity
@Table(name = "documento")
@Getter @Setter @NoArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación dentro del mismo módulo → se usa @ManyToOne con la entidad completa.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trabajador_id", nullable = false, updatable = false)
    private Trabajador trabajador;

    @Enumerated(EnumType.STRING)
    private TipoDocumento tipo;

    // Ruta relativa al STORAGE_PATH (ej: "trabajadores/42/documentos/uuid.pdf").
    @Column(name = "archivo_ruta", nullable = false)
    private String archivoRuta;

    // Fecha declarada por la Empresa/Monotributista. Si es anterior a hoy → VENCIDO.
    // Se usa LocalDate (sin hora) porque solo importa el día, no el momento exacto.
    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @PrePersist
    void prePersist() {
        this.actualizadoEn = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.actualizadoEn = Instant.now();
    }

    /**
     * Un documento está vencido si su fecha de vencimiento es anterior a hoy.
     * "Anterior" = estrictamente antes; el día de vencimiento todavía se considera vigente.
     */
    public boolean estaVencido() {
        return this.fechaVencimiento.isBefore(LocalDate.now());
    }
}
