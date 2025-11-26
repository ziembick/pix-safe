package br.com.bradesco.safeboleto.repositories;

import br.com.bradesco.safeboleto.model.TrustedBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrustedBankRepository extends JpaRepository<TrustedBank, String> {}