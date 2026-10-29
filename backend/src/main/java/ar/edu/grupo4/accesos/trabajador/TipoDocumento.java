package ar.edu.grupo4.accesos.trabajador;

/**
 * Tipos de documento que una Empresa/Monotributista puede cargar para un trabajador.
 * Coincide con el CHECK de la tabla documento: tipo IN ('ART', 'SEGURO_VIDA').
 *
 * Cada trabajador puede tener como máximo uno de cada tipo (UNIQUE trabajador_id + tipo).
 * Si ambos existen, el estado documental se calcula con el vencimiento más próximo (RN-04).
 */
public enum TipoDocumento {
    ART,
    SEGURO_VIDA
}
