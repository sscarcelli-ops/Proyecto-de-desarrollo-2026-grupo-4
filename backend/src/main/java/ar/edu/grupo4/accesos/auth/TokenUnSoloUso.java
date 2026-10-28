package ar.edu.grupo4.accesos.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Token de un solo uso (tabla token): activación del Administrador (US-01) y recupero de contraseña (US-06).
 * Se guarda el hash SHA-256 del código, nunca el código: con la base no se puede activar ni recuperar nada.
 * Un token sirve si existe, no fue usado y no venció.
 */
@Entity
@Table(name = "token")
@Getter
@Setter
@NoArgsConstructor
public class TokenUnSoloUso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoTokenUnSoloUso tipo;

    // null en la activación del Administrador (la cuenta todavía no existe)
    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "expira_en")
    private Instant expiraEn;

    // null mientras no se usó
    @Column(name = "usado_en")
    private Instant usadoEn;

    public boolean fueUsado() {
        return usadoEn != null;
    }

    public boolean estaVencido(Instant ahora) {
        return !expiraEn.isAfter(ahora);
    }

    /**
     * Hash con el que se guarda y se busca un código. Ignora espacios alrededor y mayúsculas/minúsculas:
     * "grupo4-activar-2026 " y "GRUPO4-ACTIVAR-2026" son el mismo código (se tipea a mano desde el manual).
     * SHA-256 y no bcrypt: el hash tiene que ser siempre el mismo para poder buscarlo en la base.
     */
    public static String hashDe(String codigo) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(codigo.strip().toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
