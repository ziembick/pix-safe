package br.com.bradesco.safeboleto.config;

import br.com.bradesco.safeboleto.model.TrustedBank;
import br.com.bradesco.safeboleto.model.User;
import br.com.bradesco.safeboleto.repositories.TrustedBankRepository;
import br.com.bradesco.safeboleto.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TrustedBankRepository trustedBankRepository;

    @Value("${app.admin.initial-password:}") // Pega da variável de ambiente, com um valor padrão vazio
    private String adminInitialPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, TrustedBankRepository trustedBankRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.trustedBankRepository = trustedBankRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Cria um usuário 'admin' se ele não existir e uma senha inicial foi fornecida
        if (adminInitialPassword != null && !adminInitialPassword.isBlank() && userRepository.findByUsername("admin").isEmpty()) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode(adminInitialPassword));
            adminUser.setRole("ADMIN"); // Adiciona a permissão de ADMIN
            userRepository.save(adminUser);
        }
        
        // Popula a tabela de bancos confiáveis, caso esteja vazia
        if (trustedBankRepository.count() == 0) {
            List<TrustedBank> banks = List.of(
                new TrustedBank("237", "Bradesco"),
                new TrustedBank("341", "Itaú Unibanco"),
                new TrustedBank("001", "Banco do Brasil"),
                new TrustedBank("104", "Caixa Econômica Federal"),
                new TrustedBank("033", "Santander"),
                new TrustedBank("260", "Nu Pagamentos (Nubank)"),
                new TrustedBank("077", "Banco Inter"),
                new TrustedBank("290", "PagBank"),
                new TrustedBank("323", "Mercado Pago"),
                new TrustedBank("380", "PicPay")
            );
            trustedBankRepository.saveAll(banks);
            System.out.println("[INIT] Bancos confiáveis carregados: " + banks.size() + " bancos.");
        }
        
        System.out.println("[INIT] Inicialização completa. Sistema pronto para validar transações PIX!");
    }
}