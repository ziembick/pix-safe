package br.com.bradesco.safeboleto.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pix_validations")
@Data
@NoArgsConstructor
public class PixValidation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String pixKey; // Chave PIX (CPF, CNPJ, email, telefone, aleatória)

    @Column(nullable = false, length = 50)
    private String pixKeyType; // CPF, CNPJ, EMAIL, PHONE, EVP (aleatória)

    @Column
    private String recipientName; // Nome do beneficiário

    @Column
    private String recipientDocument; // CPF/CNPJ do beneficiário

    @Column
    private Double amount; // Valor da transação

    @Column(name = "is_valid", nullable = false)
    private boolean isValid;

    @Column(nullable = false, length = 500)
    private String fraudReason; // Motivo da suspeita de fraude ou validação

    @Column(length = 10)
    private String bankCode; // Código do banco da chave PIX

    @Column
    private String bankName; // Nome do banco

    @Column(nullable = false)
    private LocalDateTime validationTimestamp;
}

