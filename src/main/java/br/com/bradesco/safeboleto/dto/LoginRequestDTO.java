package br.com.bradesco.safeboleto.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "O nome de usuário não pode ser vazio") String username,
        @NotBlank(message = "A senha não pode ser vazia") String password
) {
}