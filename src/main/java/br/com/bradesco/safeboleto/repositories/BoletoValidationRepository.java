package br.com.bradesco.safeboleto.repositories;

import br.com.bradesco.safeboleto.model.BoletoValidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoletoValidationRepository extends JpaRepository<BoletoValidation, Long> {
}