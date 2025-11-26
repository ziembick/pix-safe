package br.com.bradesco.safeboleto.controllers;

import br.com.bradesco.safeboleto.dto.LoginRequestDTO;
import br.com.bradesco.safeboleto.dto.LoginResponseDTO;
import br.com.bradesco.safeboleto.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints para autenticação e geração de tokens JWT")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    @Operation(summary = "Realiza o login do usuário e retorna um token JWT",
            description = "Recebe um nome de usuário e senha, autentica e, se bem-sucedido, gera um token de acesso.")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO loginRequest) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        String jwtToken = jwtService.generateToken((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal());

        return ResponseEntity.ok(new LoginResponseDTO(jwtToken));
    }
}
