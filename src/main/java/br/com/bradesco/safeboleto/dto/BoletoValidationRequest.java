package br.com.bradesco.safeboleto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BoletoValidationRequest {

    @NotBlank(message = "O código de barras não pode ser vazio.")
    @Pattern(regexp = "^[0-9]{47}$", message = "A linha digitável deve conter exatamente 47 dígitos numéricos.")
    private String barcode;
}