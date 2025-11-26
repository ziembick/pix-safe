package br.com.bradesco.safeboleto.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta da validação de transação PIX")
public class PixValidationResponse {
    
    @Schema(description = "Indica se a transação PIX é válida (não fraudulenta)", 
            example = "true")
    private boolean valid;

    @Schema(description = "Chave PIX validada", 
            example = "12345678901")
    private String pixKey;

    @Schema(description = "Tipo da chave PIX", 
            example = "CPF")
    private String keyType;

    @Schema(description = "Nome do beneficiário", 
            example = "João Silva")
    private String recipientName;

    @Schema(description = "Código do banco", 
            example = "237")
    private String bankCode;

    @Schema(description = "Nome do banco", 
            example = "Bradesco")
    private String bankName;

    @Schema(description = "Mensagem descritiva do resultado da validação", 
            example = "Transação PIX válida e segura")
    private String message;

    @Schema(description = "Score de risco (0-100, onde 100 é alto risco)", 
            example = "15")
    private Integer riskScore;
}

