package br.com.bradesco.safeboleto.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "Requisição de validação de transação PIX")
public class PixValidationRequest {

    @NotBlank(message = "A chave PIX não pode ser vazia.")
    @Schema(description = "Chave PIX (CPF, CNPJ, e-mail, telefone ou chave aleatória)", 
            example = "12345678901")
    private String pixKey;

    @NotBlank(message = "O nome do beneficiário não pode ser vazio.")
    @Schema(description = "Nome completo do beneficiário", 
            example = "João Silva")
    private String recipientName;

    @NotBlank(message = "O documento do beneficiário não pode ser vazio.")
    @Schema(description = "CPF ou CNPJ do beneficiário (apenas números)", 
            example = "12345678901")
    private String recipientDocument;

    @NotNull(message = "O valor da transação não pode ser nulo.")
    @Positive(message = "O valor da transação deve ser positivo.")
    @Schema(description = "Valor da transação em reais", 
            example = "150.00")
    private Double amount;

    @NotBlank(message = "O código do banco não pode ser vazio.")
    @Schema(description = "Código do banco (3 dígitos)", 
            example = "237")
    private String bankCode;
}

