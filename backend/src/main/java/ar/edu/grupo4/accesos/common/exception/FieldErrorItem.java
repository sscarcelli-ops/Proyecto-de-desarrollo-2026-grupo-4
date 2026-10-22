package ar.edu.grupo4.accesos.common.exception;

/** Error de validación de un campo: el frontend lo muestra debajo del input con ese nombre. */
public record FieldErrorItem(String field, String message) {
}
