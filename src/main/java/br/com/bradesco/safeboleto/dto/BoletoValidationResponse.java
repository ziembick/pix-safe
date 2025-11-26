package br.com.bradesco.safeboleto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoletoValidationResponse {
    private boolean valid;
    private String bank;
    private String message;
}