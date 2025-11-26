package br.com.bradesco.safeboleto.controllers;

import br.com.bradesco.safeboleto.dto.PixValidationRequest;
import br.com.bradesco.safeboleto.dto.PixValidationResponse;
import br.com.bradesco.safeboleto.services.PixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pix")
@RequiredArgsConstructor
@Tag(name = "PIX", description = "Operações de validação de transações PIX e detecção de fraudes")
public class PixController {

    private final PixService pixService;

    @PostMapping("/valida")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Validar transação PIX",
        description = "Valida uma transação PIX verificando múltiplos indicadores de fraude, incluindo: " +
                     "formato da chave, lista negra, compatibilidade de documentos, valores suspeitos, " +
                     "nomes suspeitos e validação de CPF/CNPJ. Retorna um score de risco de 0-100.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Validação realizada com sucesso",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Transação válida",
                        value = "{\n" +
                               "  \"valid\": true,\n" +
                               "  \"pixKey\": \"12345678909\",\n" +
                               "  \"keyType\": \"CPF\",\n" +
                               "  \"recipientName\": \"Maria Santos\",\n" +
                               "  \"bankCode\": \"237\",\n" +
                               "  \"bankName\": \"Bradesco\",\n" +
                               "  \"message\": \"Transação PIX válida e segura. Score de risco: 0/100\",\n" +
                               "  \"riskScore\": 0\n" +
                               "}"
                    ),
                    @ExampleObject(
                        name = "Transação fraudulenta",
                        value = "{\n" +
                               "  \"valid\": false,\n" +
                               "  \"pixKey\": \"12345678900\",\n" +
                               "  \"keyType\": \"CPF\",\n" +
                               "  \"recipientName\": \"Teste Fraude\",\n" +
                               "  \"bankCode\": \"237\",\n" +
                               "  \"bankName\": \"Bradesco\",\n" +
                               "  \"message\": \"⚠️ TRANSAÇÃO SUSPEITA DE FRAUDE! Motivos: Chave PIX está na lista negra. Nome contém palavra suspeita.\",\n" +
                               "  \"riskScore\": 100\n" +
                               "}"
                    )
                }
            )
        ),
        @ApiResponse(responseCode = "400", description = "Requisição inválida (dados mal formatados)"),
        @ApiResponse(responseCode = "401", description = "Não autenticado (token JWT inválido ou ausente)"),
        @ApiResponse(responseCode = "403", description = "Sem permissão para acessar este recurso")
    })
    public ResponseEntity<PixValidationResponse> validatePix(
            @Valid @RequestBody PixValidationRequest request) {
        
        PixValidationResponse response = pixService.validatePix(
            request.getPixKey(),
            request.getRecipientName(),
            request.getRecipientDocument(),
            request.getAmount(),
            request.getBankCode()
        );
        
        return ResponseEntity.ok(response);
    }
}

