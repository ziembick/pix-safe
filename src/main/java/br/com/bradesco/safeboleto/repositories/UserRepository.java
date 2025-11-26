package br.com.bradesco.safeboleto.repositories;

import br.com.bradesco.safeboleto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}