package ar.edu.grupo4.accesos.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Cuenta base (RF-08): email único y contraseña con hash bcrypt. Puede tener 0, 1 o más roles (RN-12). */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    // Lo completa la BD (DEFAULT now())
    @Column(name = "creado_en", insertable = false, updatable = false)
    private OffsetDateTime creadoEn;

    // Va en el token (claim "ver"); al cerrar sesión se incrementa y los tokens anteriores dejan de valer (T-03)
    @Column(name = "version_sesion")
    private int versionSesion;

    @ManyToMany
    @JoinTable(name = "usuario_rol",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new HashSet<>();

    /** Códigos de sus roles, ordenados (ej. ["ADMIN"]). Vacío si no tiene ningún rol aprobado (RN-12). */
    public List<String> codigosDeRoles() {
        return roles.stream().map(Rol::getCodigo).sorted().toList();
    }
}
