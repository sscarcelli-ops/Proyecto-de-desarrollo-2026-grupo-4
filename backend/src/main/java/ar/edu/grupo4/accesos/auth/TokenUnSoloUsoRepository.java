package ar.edu.grupo4.accesos.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TokenUnSoloUsoRepository extends JpaRepository<TokenUnSoloUso, Long> {

    Optional<TokenUnSoloUso> findByTipoAndTokenHash(TipoTokenUnSoloUso tipo, String tokenHash);

    /**
     * Consume el token solo si nadie lo usó antes: devuelve 1 si lo marcó y 0 si ya estaba usado.
     * Si dos personas confirman a la vez, la base bloquea la fila y la segunda ve 0 (US-01 CA-6).
     */
    @Modifying
    @Query("update TokenUnSoloUso t set t.usadoEn = :ahora where t.id = :id and t.usadoEn is null")
    int marcarUsado(@Param("id") Long id, @Param("ahora") Instant ahora);

    /** Vence los tokens de ese tipo que todavía servían: así queda un único token vigente. */
    @Modifying
    @Query("""
        update TokenUnSoloUso t set t.expiraEn = :ahora
        where t.tipo = :tipo and t.usadoEn is null and t.expiraEn > :ahora
        """)
    int vencerVigentes(@Param("tipo") TipoTokenUnSoloUso tipo, @Param("ahora") Instant ahora);
}
