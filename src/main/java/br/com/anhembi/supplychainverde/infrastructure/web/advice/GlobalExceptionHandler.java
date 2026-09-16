package br.com.anhembi.supplychainverde.infrastructure.web.advice;

import br.com.anhembi.supplychainverde.application.exception.ApplicationException;
import br.com.anhembi.supplychainverde.application.exception.UnauthorizedActionException;
import br.com.anhembi.supplychainverde.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({DomainException.class, ApplicationException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBusinessException(RuntimeException exception) {
        HttpStatus status = exception instanceof UnauthorizedActionException
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(error(status, exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado."));
    }

    private Map<String, Object> error(HttpStatus status, String message) {
        return Map.of(
                "timestamp", LocalDateTime.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? status.getReasonPhrase() : message
        );
    }
}
