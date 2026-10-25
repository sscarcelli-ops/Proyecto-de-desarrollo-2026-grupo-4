package ar.edu.grupo4.accesos.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api")
public class PingController {

    /** DTO de ejemplo: aparece en Swagger y el frontend recibe su tipo generado (npm run api:types). */
    public record PingResponse(String status, String message, OffsetDateTime timestamp) {
    }

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("ok", "pong", OffsetDateTime.now());
    }
}
