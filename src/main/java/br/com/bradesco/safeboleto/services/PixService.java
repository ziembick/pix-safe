package br.com.bradesco.safeboleto.services;

import br.com.bradesco.safeboleto.dto.PixValidationResponse;
import br.com.bradesco.safeboleto.model.PixValidation;
import br.com.bradesco.safeboleto.repositories.PixValidationRepository;
import br.com.bradesco.safeboleto.repositories.TrustedBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PixService {

    private final PixValidationRepository repository;
    private final TrustedBankRepository trustedBankRepository;

    // Padrões de validação de chaves PIX
    private static final Pattern CPF_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern CNPJ_PATTERN = Pattern.compile("^\\d{14}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{10,14}$");
    private static final Pattern EVP_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);

    // Lista negra de chaves PIX conhecidas por fraude (em produção, usar base de dados)
    private static final Set<String> BLACKLISTED_KEYS = new HashSet<>(Set.of(
        "12345678900", // CPF suspeito
        "00000000000", // CPF inválido
        "11111111111", // CPF sequencial
        "fraudador@email.com",
        "golpe@teste.com",
        "+5511900000000"
    ));

    // Documentos suspeitos
    private static final Set<String> BLACKLISTED_DOCUMENTS = new HashSet<>(Set.of(
        "00000000000",
        "11111111111",
        "22222222222",
        "12345678900"
    ));

    // Palavras suspeitas em nomes
    private static final Set<String> SUSPICIOUS_NAME_KEYWORDS = new HashSet<>(Set.of(
        "teste", "test", "golpe", "fraude", "fake", "falso", "laranja"
    ));

    private static final String UNKNOWN_BANK_NAME = "Desconhecido";

    public PixValidationResponse validatePix(String pixKey, String recipientName, 
                                              String recipientDocument, Double amount, 
                                              String bankCode) {
        
        System.out.println("[DEBUG PIX] Iniciando validação - pixKey: " + pixKey);
        
        int riskScore = 0;
        StringBuilder fraudReasons = new StringBuilder();

        // 1. Validação de formato da chave PIX
        String keyType = detectPixKeyType(pixKey);
        if (keyType == null) {
            return createInvalidResponse(pixKey, null, recipientName, recipientDocument, 
                                        amount, bankCode, 
                                        "Formato de chave PIX inválido. Verifique o tipo da chave.", 
                                        100);
        }

        System.out.println("[DEBUG PIX] Tipo de chave detectado: " + keyType);

        // 2. Verificação de lista negra de chaves
        if (BLACKLISTED_KEYS.contains(pixKey.toLowerCase())) {
            riskScore += 100;
            fraudReasons.append("Chave PIX está na lista negra de fraudes conhecidas. ");
        }

        // 3. Verificação de lista negra de documentos
        if (BLACKLISTED_DOCUMENTS.contains(recipientDocument)) {
            riskScore += 100;
            fraudReasons.append("Documento do beneficiário está na lista negra. ");
        }

        // 4. Validação de banco confiável
        String bankName = getBankNameFromCode(bankCode);
        if (UNKNOWN_BANK_NAME.equals(bankName)) {
            riskScore += 40;
            fraudReasons.append("Banco não reconhecido ou não confiável. ");
        }

        // 5. Validação de compatibilidade chave-documento
        if (!validateKeyDocumentMatch(pixKey, keyType, recipientDocument)) {
            riskScore += 60;
            fraudReasons.append("Chave PIX não corresponde ao documento informado. ");
        }

        // 6. Detecção de valor suspeito
        String amountIssue = checkSuspiciousAmount(amount);
        if (amountIssue != null) {
            riskScore += 30;
            fraudReasons.append(amountIssue).append(" ");
        }

        // 7. Verificação de nomes suspeitos
        String nameIssue = checkSuspiciousName(recipientName);
        if (nameIssue != null) {
            riskScore += 50;
            fraudReasons.append(nameIssue).append(" ");
        }

        // 8. Verificação de histórico de fraudes (chaves com múltiplas tentativas inválidas)
        long previousFrauds = repository.countByPixKeyAndIsValidFalse(pixKey);
        if (previousFrauds > 2) {
            riskScore += 40;
            fraudReasons.append("Chave PIX tem histórico de tentativas fraudulentas (")
                       .append(previousFrauds).append(" tentativas). ");
        }

        // 9. Validação específica de CPF (algoritmo básico)
        if ("CPF".equals(keyType) && !isValidCPF(pixKey)) {
            riskScore += 70;
            fraudReasons.append("CPF com dígitos verificadores inválidos. ");
        }

        // 10. Validação específica de CNPJ (algoritmo básico)
        if ("CNPJ".equals(keyType) && !isValidCNPJ(pixKey)) {
            riskScore += 70;
            fraudReasons.append("CNPJ com dígitos verificadores inválidos. ");
        }

        // Limita o score em 100
        riskScore = Math.min(riskScore, 100);

        System.out.println("[DEBUG PIX] Risk Score calculado: " + riskScore);
        System.out.println("[DEBUG PIX] Motivos de fraude: " + fraudReasons);

        // Decisão final: se riskScore >= 50, considerar fraudulento
        boolean isValid = riskScore < 35;
        String message;
        
        if (isValid) {
            message = "Transação PIX válida e segura. Score de risco: " + riskScore + "/100";
            if (riskScore > 0) {
                message += " (Baixo risco detectado: " + fraudReasons.toString().trim() + ")";
            }
        } else {
            message = "⚠️ TRANSAÇÃO SUSPEITA DE FRAUDE! Motivos: " + fraudReasons.toString().trim();
        }

        PixValidationResponse response = new PixValidationResponse(
            isValid, pixKey, keyType, recipientName, bankCode, bankName, message, riskScore
        );

        saveValidation(pixKey, keyType, recipientName, recipientDocument, 
                      amount, bankCode, bankName, isValid, message);

        return response;
    }

    private String detectPixKeyType(String key) {
        if (key == null || key.isBlank()) return null;
        
        String cleanKey = key.trim();
        
        if (CPF_PATTERN.matcher(cleanKey).matches()) return "CPF";
        if (CNPJ_PATTERN.matcher(cleanKey).matches()) return "CNPJ";
        if (EMAIL_PATTERN.matcher(cleanKey).matches()) return "EMAIL";
        if (PHONE_PATTERN.matcher(cleanKey).matches()) return "PHONE";
        if (EVP_PATTERN.matcher(cleanKey).matches()) return "EVP";
        
        return null;
    }

    private boolean validateKeyDocumentMatch(String key, String keyType, String document) {
        if (keyType == null || document == null) return false;
        
        // Se a chave é CPF ou CNPJ, deve corresponder exatamente ao documento
        if ("CPF".equals(keyType) || "CNPJ".equals(keyType)) {
            return key.equals(document);
        }
        
        // Para email, telefone e EVP, não há validação direta com o documento
        return true;
    }

    private String checkSuspiciousAmount(Double amount) {
        if (amount == null) return "Valor da transação não informado.";
        
        // Valores muito altos (acima do limite PIX noturno de R$ 1.000,00)
        if (amount > 1000.0) {
            return "Valor acima do limite PIX noturno (R$ 1.000,00).";
        }
        
        // Valores extremamente altos
        if (amount > 10000.0) {
            return "Valor extremamente alto para transação PIX (R$ " + amount + ").";
        }
        
        // Valores fracionados suspeitos (testes de fraude)
        if (amount < 1.0) {
            return "Valor muito baixo, típico de teste de fraude (R$ " + amount + ").";
        }
        
        // Valores "quebrados" suspeitos (ex: R$ 999,99)
        if (amount > 900 && amount < 1000 && amount % 1 == 0.99) {
            return "Valor suspeito próximo ao limite.";
        }
        
        return null;
    }

    private String checkSuspiciousName(String name) {
        if (name == null || name.isBlank()) {
            return "Nome do beneficiário não informado.";
        }
        
        String nameLower = name.toLowerCase().trim();
        
        // Nomes muito curtos
        if (nameLower.length() < 3) {
            return "Nome do beneficiário muito curto.";
        }
        
        // Verifica palavras suspeitas
        for (String keyword : SUSPICIOUS_NAME_KEYWORDS) {
            if (nameLower.contains(keyword)) {
                return "Nome contém palavra suspeita: '" + keyword + "'.";
            }
        }
        
        // Muitos números no nome (ex: "João123456")
        long digitCount = nameLower.chars().filter(Character::isDigit).count();
        if (digitCount > 3) {
            return "Nome contém muitos números (" + digitCount + " dígitos).";
        }
        
        // Nome com apenas números
        if (nameLower.matches("^[0-9]+$")) {
            return "Nome contém apenas números.";
        }
        
        return null;
    }

    private boolean isValidCPF(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}")) return false;
        
        // CPFs com todos os dígitos iguais são inválidos
        if (cpf.matches("(\\d)\\1{10}")) return false;
        
        try {
            // Calcula o primeiro dígito verificador
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
            }
            int firstDigit = 11 - (sum % 11);
            if (firstDigit >= 10) firstDigit = 0;
            
            // Calcula o segundo dígito verificador
            sum = 0;
            for (int i = 0; i < 10; i++) {
                sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
            }
            int secondDigit = 11 - (sum % 11);
            if (secondDigit >= 10) secondDigit = 0;
            
            // Verifica se os dígitos calculados correspondem aos informados
            return firstDigit == Character.getNumericValue(cpf.charAt(9)) &&
                   secondDigit == Character.getNumericValue(cpf.charAt(10));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidCNPJ(String cnpj) {
        if (cnpj == null || !cnpj.matches("\\d{14}")) return false;
        
        // CNPJs com todos os dígitos iguais são inválidos
        if (cnpj.matches("(\\d)\\1{13}")) return false;
        
        try {
            // Calcula o primeiro dígito verificador
            int[] weight1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                sum += Character.getNumericValue(cnpj.charAt(i)) * weight1[i];
            }
            int firstDigit = sum % 11 < 2 ? 0 : 11 - (sum % 11);
            
            // Calcula o segundo dígito verificador
            int[] weight2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            sum = 0;
            for (int i = 0; i < 13; i++) {
                sum += Character.getNumericValue(cnpj.charAt(i)) * weight2[i];
            }
            int secondDigit = sum % 11 < 2 ? 0 : 11 - (sum % 11);
            
            // Verifica se os dígitos calculados correspondem aos informados
            return firstDigit == Character.getNumericValue(cnpj.charAt(12)) &&
                   secondDigit == Character.getNumericValue(cnpj.charAt(13));
        } catch (Exception e) {
            return false;
        }
    }

    private String getBankNameFromCode(String bankCode) {
        if (bankCode == null) return UNKNOWN_BANK_NAME;
        return trustedBankRepository.findById(bankCode)
                .map(bank -> bank.getName())
                .orElse(UNKNOWN_BANK_NAME);
    }

    private PixValidationResponse createInvalidResponse(String pixKey, String keyType, 
                                                        String recipientName, String recipientDocument, 
                                                        Double amount, String bankCode, 
                                                        String fraudReason, int riskScore) {
        String bankName = getBankNameFromCode(bankCode);
        
        PixValidationResponse response = new PixValidationResponse(
            false, pixKey, keyType, recipientName, bankCode, bankName, fraudReason, riskScore
        );
        
        saveValidation(pixKey, keyType, recipientName, recipientDocument, 
                      amount, bankCode, bankName, false, fraudReason);
        
        return response;
    }

    private void saveValidation(String pixKey, String keyType, String recipientName, 
                               String recipientDocument, Double amount, String bankCode, 
                               String bankName, boolean isValid, String message) {
        PixValidation validation = new PixValidation();
        validation.setPixKey(pixKey != null ? pixKey : "N/A");
        validation.setPixKeyType(keyType != null ? keyType : "UNKNOWN");
        validation.setRecipientName(recipientName);
        validation.setRecipientDocument(recipientDocument);
        validation.setAmount(amount);
        validation.setValid(isValid);
        validation.setFraudReason(message);
        validation.setBankCode(bankCode);
        validation.setBankName(bankName);
        validation.setValidationTimestamp(LocalDateTime.now());
        
        repository.save(validation);
        
        System.out.println("[DEBUG PIX] Validação salva no banco de dados - ID: " + validation.getId());
    }
}

