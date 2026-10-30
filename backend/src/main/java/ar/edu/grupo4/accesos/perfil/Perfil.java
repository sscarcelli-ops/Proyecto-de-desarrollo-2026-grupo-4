package ar.edu.grupo4.accesos.perfil;

import ar.edu.grupo4.accesos.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Perfil fiscal solicitado por una cuenta (Empresa o Monotributista) para operar (RF-08, RN-12).
 */
@Entity
@Table(name = "perfil")
@Getter
@Setter
@NoArgsConstructor
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoPerfil tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoPerfil estado = EstadoPerfil.PENDIENTE;

    @Column(name = "cuit_cuil", nullable = false, length = 13, unique = true)
    private String cuitCuil;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(length = 100)
    private String nombre;

    @Column(length = 100)
    private String apellido;

    @Column(name = "dni_titular", length = 10)
    private String dniTitular;

    @Column(length = 30)
    private String telefono;

    @Column(name = "constancia_ruta", nullable = false, length = 300)
    private String constanciaRuta;

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "motivo_suspension", length = 500)
    private String motivoSuspension;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "resuelto_en")
    private OffsetDateTime resueltoEn;

    public Perfil(Usuario usuario, TipoPerfil tipo, String cuitCuil, String constanciaRuta) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.cuitCuil = cuitCuil;
        this.constanciaRuta = constanciaRuta;
    }
}
