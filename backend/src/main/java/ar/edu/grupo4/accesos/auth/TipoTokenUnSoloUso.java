package ar.edu.grupo4.accesos.auth;

/** Para qué sirve un token de un solo uso. Mismos códigos que el CHECK de la tabla token. */
public enum TipoTokenUnSoloUso {
    /** Crear el Administrador inicial (US-01). Sin usuario: la cuenta todavía no existe. */
    ACTIVACION_ADMIN,
    /** Recuperar la contraseña de una cuenta (US-06). */
    RECUPERO
}
