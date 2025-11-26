package br.com.bradesco.safeboleto.controllers;

import br.com.bradesco.safeboleto.dto.BoletoValidationRequest;
import br.com.bradesco.safeboleto.dto.BoletoValidationResponse;
import br.com.bradesco.safeboleto.services.BoletoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/boleto")
@RequiredArgsConstructor
@Tag(name = "Boleto", description = "Operações relacionadas à validação de boletos")
public class BoletoController {

    private final BoletoService boletoService;

    @PostMapping("/valida")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Validar boleto",
        description = "Valida um boleto bancário completo conforme padrões FEBRABAN",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<BoletoValidationResponse> validateBoleto(
            @RequestBody BoletoValidationRequest request) {
        BoletoValidationResponse response = boletoService.validateBoleto(request.getBarcode());
        return ResponseEntity.ok(response);
    }
}
