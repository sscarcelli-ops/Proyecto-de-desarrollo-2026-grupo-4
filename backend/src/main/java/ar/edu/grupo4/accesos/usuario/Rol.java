package ar.edu.grupo4.accesos.usuario;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Rol fijo del sistema (tabla rol, cargada por la migración inicial). Solo lectura. */
@Entity
@Table(name = "rol")
@Getter
@NoArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    /** ADMIN, OPERADOR, GUARDIA, EMPRESA o MONOTRIBUTISTA (mismos códigos que el frontend). */
    private String codigo;

    private String nombre;
}
