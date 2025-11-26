package br.com.bradesco.safeboleto.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Getter
    @AllArgsConstructor
    private static class ErrorResponse implements Serializable {
        private final int status;
        private final String error;
        private final String message;
        private final Instant timestamp;
        private final Object details; // Campo adicionado para detalhes
    }

    // Captura a exceção personalizada para problemas com o token JWT
    @ExceptionHandler(JwtTokenException.class)
    public ResponseEntity<ErrorResponse> handleJwtTokenException(JwtTokenException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponse errorResponse = new ErrorResponse(status.value(), "Não Autorizado", ex.getMessage(), Instant.now(), null);
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponse errorResponse = new ErrorResponse(status.value(), "Não Autorizado", "Falha na autenticação: " + ex.getMessage(), Instant.now(), null);
        return new ResponseEntity<>(errorResponse, status);
    }

    // Captura erros de validação (ex: anotação @Valid em DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = new ErrorResponse(status.value(), "Validation Error", "Um ou mais campos são inválidos.", Instant.now(), fieldErrors);
        return new ResponseEntity<>(errorResponse, status);
    }

    // Captura erros de parsing do corpo da requisição
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse errorResponse = new ErrorResponse(status.value(), "Requisição Inválida", "Corpo da requisição mal formatado ou inválido.", Instant.now(), null);
        return new ResponseEntity<>(errorResponse, status);
    }

    // Captura erros de acesso negado (usuário autenticado, mas sem permissão)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        ErrorResponse errorResponse = new ErrorResponse(status.value(), "Acesso Negado", "Você não tem permissão para acessar este recurso.", Instant.now(), null);
        return new ResponseEntity<>(errorResponse, status);
    }

    // Manipulador genérico para outras exceções não tratadas
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        log.error("Erro inesperado no servidor", ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse errorResponse = new ErrorResponse(status.value(), "Erro Interno do Servidor", "Ocorreu um erro interno no servidor.", Instant.now(), null);
        return new ResponseEntity<>(errorResponse, status);
    }
}