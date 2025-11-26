package br.com.bradesco.safeboleto.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED) // Define o status HTTP padrão para esta exceção
public class JwtTokenException extends RuntimeException {
    public JwtTokenException(String message) { super(message); }
}