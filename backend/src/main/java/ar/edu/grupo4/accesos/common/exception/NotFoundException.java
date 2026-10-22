package ar.edu.grupo4.accesos.common.exception;

import org.springframework.http.HttpStatus;

/** El recurso pedido no existe o no pertenece al usuario autenticado (404). */
public class NotFoundException extends BusinessException {

    public NotFoundException() {
        this("El recurso solicitado no existe.");
    }

    public NotFoundException(String message) {
        super("NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
