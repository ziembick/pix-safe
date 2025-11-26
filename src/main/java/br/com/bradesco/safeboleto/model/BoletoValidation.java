package br.com.bradesco.safeboleto.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "boleto_validations")
@Data
@NoArgsConstructor
public class BoletoValidation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 48)
    private String barcode;

    @Column(name = "is_valid", nullable = false)
    private boolean isValid;

    @Column
    private String bank;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDateTime validationTimestamp;
}