package br.com.bradesco.safeboleto.repositories;

import br.com.bradesco.safeboleto.model.PixValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PixValidationRepository extends JpaRepository<PixValidation, Long> {
    List<PixValidation> findByPixKey(String pixKey);
    List<PixValidation> findByRecipientDocument(String recipientDocument);
    long countByPixKeyAndIsValidFalse(String pixKey);
}

