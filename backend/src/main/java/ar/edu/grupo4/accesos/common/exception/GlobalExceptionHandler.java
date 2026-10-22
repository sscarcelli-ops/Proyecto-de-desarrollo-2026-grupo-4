package ar.edu.grupo4.accesos.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Formato único de error de toda la API (RFC 9457, application/problem+json):
 *
 * <pre>
 * {
 *   "status": 400,
 *   "title": "Bad Request",
 *   "detail": "Hay datos inválidos en la solicitud.",   // mensaje para mostrar al usuario
 *   "instance": "/api/barrios",
 *   "code": "VALIDATION_ERROR",                        // código estable para el frontend
 *   "timestamp": "2026-10-22T22:30:00Z",
 *   "errors": [ { "field": "nombre", "message": "no debe estar vacío" } ]   // solo en validaciones
 * }
 * </pre>
 *
 * Los Services lanzan {@link BusinessException} / {@link NotFoundException}; nunca arman respuestas de error.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String VALIDATION_MESSAGE = "Hay datos inválidos en la solicitud.";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Object> handleBusiness(BusinessException ex) {
        return problem(ex.getStatus(), ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Tenés que iniciar sesión para continuar.", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", "No tenés permiso para realizar esta acción.", null);
    }

    // Último recurso: se registra el detalle en el log y al usuario no se le muestra nada interno
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
            "Ocurrió un error inesperado. Intentá nuevamente más tarde.", null);
    }

    // @Valid sobre un @RequestBody
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorItem> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(e -> errors.add(new FieldErrorItem(e.getField(), e.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
            .forEach(e -> errors.add(new FieldErrorItem(e.getObjectName(), e.getDefaultMessage())));
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", VALIDATION_MESSAGE, errors);
    }

    // Validaciones sobre @RequestParam / @PathVariable (ej. @Min en un parámetro)
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorItem> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> result.getResolvableErrors().forEach(e ->
            errors.add(new FieldErrorItem(result.getMethodParameter().getParameterName(), e.getDefaultMessage()))));
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", VALIDATION_MESSAGE, errors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
            "El cuerpo de la solicitud no tiene un formato válido.", null);
    }

    // RNF-10: límite configurado en spring.servlet.multipart
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return problem(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE",
            "El archivo supera el tamaño máximo permitido.", null);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "El recurso solicitado no existe.", null);
    }

    // El resto de los errores de Spring MVC (405, 415, etc.) también llevan code y timestamp
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail pd) {
            addStandardProperties(pd, defaultCode(statusCode));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ResponseEntity<Object> problem(HttpStatus status, String code, String detail, List<FieldErrorItem> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        addStandardProperties(pd, code);
        if (errors != null) {
            pd.setProperty("errors", errors);
        }
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }

    private static void addStandardProperties(ProblemDetail pd, String code) {
        if (pd.getProperties() == null || !pd.getProperties().containsKey("code")) {
            pd.setProperty("code", code);
        }
        pd.setProperty("timestamp", Instant.now().toString());
    }

    private static String defaultCode(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status != null ? status.name() : "HTTP_" + statusCode.value();
    }
}
