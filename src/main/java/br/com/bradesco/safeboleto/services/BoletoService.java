package br.com.bradesco.safeboleto.services;

import br.com.bradesco.safeboleto.dto.BoletoValidationResponse;
import br.com.bradesco.safeboleto.model.BoletoValidation;
import br.com.bradesco.safeboleto.repositories.BoletoValidationRepository;
import br.com.bradesco.safeboleto.repositories.TrustedBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BoletoService {

    private final BoletoValidationRepository repository;
    private final TrustedBankRepository trustedBankRepository;

    // Padrão para validar se a linha digitável contém apenas 47 dígitos numéricos.
    private static final Pattern BARCODE_PATTERN = Pattern.compile("^\\d{47}$");
    
    // Flag para debug

    private static final String INVALID_BARCODE_MESSAGE = "Código de barras inválido. Verifique os dígitos.";
    private static final String UNTRUSTED_BANK_MESSAGE = "Emissor do boleto não é confiável ou não foi encontrado.";
    private static final String INVALID_CHECKSUM_MESSAGE = "Boleto inválido. O dígito verificador não confere.";
    private static final String LEGITIMATE_BOLETO_MESSAGE = "Boleto legítimo.";
    private static final String UNKNOWN_BANK_NAME = "Desconhecido"; // Nome padrão para bancos não identificados.

    public BoletoValidationResponse validateBoleto(String barcode) {
        // 1. Validação de formato básico.
        if (barcode == null || !BARCODE_PATTERN.matcher(barcode).matches()) {
            BoletoValidationResponse response = new BoletoValidationResponse(false, UNKNOWN_BANK_NAME, INVALID_BARCODE_MESSAGE);
            saveValidation(barcode, response);
            return response;
        }

        // A partir daqui, o formato é válido. Podemos extrair informações.
        String bankCode = barcode.substring(0, 3);
        String bankName = getBankNameFromBankCode(bankCode);
        boolean isTrusted = !UNKNOWN_BANK_NAME.equals(bankName);

        // DEBUG: imprime informações úteis para investigação de testes
        System.out.println("[DEBUG] barcode=" + barcode);
        System.out.println("[DEBUG] bankCode=" + bankCode + ", bankName=" + bankName + ", isTrusted=" + isTrusted);

        // 2. Checagem se o banco é confiável.
        if (!isTrusted) {
            BoletoValidationResponse response = new BoletoValidationResponse(false, UNKNOWN_BANK_NAME, UNTRUSTED_BANK_MESSAGE);
            saveValidation(barcode, response);
            return response;
        }

        // 3. Validação dos DVs da linha digitável.
        boolean typableValid = isValidTypableLine(barcode);
        System.out.println("[DEBUG] typableValid=" + typableValid);
        if (!typableValid) {
            BoletoValidationResponse response = new BoletoValidationResponse(false, bankName, INVALID_BARCODE_MESSAGE);
            saveValidation(barcode, response);
            return response;
        }

        // 4. Validação do DV geral do código de barras.
        boolean barcodeChecksumValid = isValidBarcodeChecksum(barcode);
        System.out.println("[DEBUG] barcodeChecksumValid=" + barcodeChecksumValid);
        if (!barcodeChecksumValid) {
            BoletoValidationResponse response = new BoletoValidationResponse(false, bankName, INVALID_CHECKSUM_MESSAGE);
            saveValidation(barcode, response);
            return response;
        }

        // 5. Se todas as validações passaram, o boleto é legítimo.
        BoletoValidationResponse response = new BoletoValidationResponse(true, bankName, LEGITIMATE_BOLETO_MESSAGE);
        saveValidation(barcode, response);
        return response;
    }

    private String getBankNameFromBankCode(String bankCode) {
        return trustedBankRepository.findById(bankCode)
                .map(trustedBank -> trustedBank.getName()) // Em Java moderno, pode ser trocado por .map(TrustedBank::getName)
                .orElse(UNKNOWN_BANK_NAME);
    }

    /**
     * Valida os 3 dígitos verificadores (DV) da linha digitável (47 posições)
     * usando o cálculo de Módulo 10 para cada um dos três campos.
     */
    private boolean isValidTypableLine(String typableLine) {
        String field1 = typableLine.substring(0, 9);
        char dv1 = typableLine.charAt(9);
        String field2 = typableLine.substring(10, 20);
        char dv2 = typableLine.charAt(20);
        String field3 = typableLine.substring(21, 31);
        char dv3 = typableLine.charAt(31);

        int calc1 = calculateMod10(field1);
        int calc2 = calculateMod10(field2);
        int calc3 = calculateMod10(field3);

        // DEBUG: imprime os campos e os dígitos calculados para ajudar na investigação
        System.out.println("[DEBUG] field1=" + field1 + ", dv1=" + dv1 + ", calc1=" + calc1);
        System.out.println("[DEBUG] field2=" + field2 + ", dv2=" + dv2 + ", calc2=" + calc2);
        System.out.println("[DEBUG] field3=" + field3 + ", dv3=" + dv3 + ", calc3=" + calc3);

        return calc1 == Character.getNumericValue(dv1) &&
                calc2 == Character.getNumericValue(dv2) &&
                calc3 == Character.getNumericValue(dv3);
    }

    /**
     * Calcula o dígito verificador de um campo usando o Módulo 10.
     */
    private int calculateMod10(String field) {
        int sum = 0;
        int multiplier = 2;
        StringBuilder debug = new StringBuilder("Mod10 para campo " + field + ":\n");
        
        for (int i = field.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(field.charAt(i));
            int product = digit * multiplier;
            int sumPart = (product > 9) ? (product / 10) + (product % 10) : product;
            
            debug.append(String.format("Pos %d: %d * %d = %d", i, digit, multiplier, product));
            if (product > 9) {
                debug.append(String.format(" -> %d + %d = %d", product / 10, product % 10, sumPart));
            }
            debug.append("\n");
            
            sum += sumPart;
            multiplier = (multiplier == 2) ? 1 : 2;
        }
        
        int remainder = sum % 10;
        int result = (remainder == 0) ? 0 : 10 - remainder;
        
        debug.append(String.format("Soma total: %d\n", sum));
        debug.append(String.format("Resto: %d\n", remainder));
        debug.append(String.format("DV calculado: %d", result));
        
        System.out.println(debug.toString());
        
        return result;
    }

    /**
     * Calcula o dígito verificador usando o algoritmo Módulo 11 conforme FEBRABAN.
     * Os pesos são aplicados da esquerda para a direita: 4,3,2,9,8,7,6,5,4,3,2
     */
    private int calculateMod11(String block) {
        int sum = 0;
        int multiplier = 2; // O multiplicador começa em 2 e vai até 9, ciclicamente.

        // O cálculo é feito da direita para a esquerda.
        for (int i = block.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(block.charAt(i));
            sum += digit * multiplier;

            // O multiplicador reinicia para 2 após chegar em 9.
            multiplier = (multiplier == 9) ? 2 : multiplier + 1;
        }

        int remainder = sum % 11;
        int calculatedDv;

        // Regra padrão FEBRABAN para o Módulo 11 de boletos.
        if (remainder == 0 || remainder == 1 || remainder == 10) {
            calculatedDv = 1;
        } else {
            calculatedDv = 11 - remainder;
        }

        return calculatedDv;
    }

    /**
     * Valida o dígito verificador geral (DV) do código de barras.
     * O código de barras (44 posições) é montado a partir da linha digitável.
     */
    private boolean isValidBarcodeChecksum(String typableLine) {
        // DV geral informado (posição 33 da linha digitável, índice 32)
        int dvInformado = Character.getNumericValue(typableLine.charAt(32));

        // Extrai partes para montar o bloco (43 dígitos) usado no cálculo do DV
        // A ordem correta do código de barras é: Banco+Moeda, Fator de Vencimento+Valor, e DEPOIS o Campo Livre
        String block = typableLine.substring(0, 4) +   // Banco (3) e Moeda (1)
                typableLine.substring(33, 47) + // Fator de Vencimento (4) e Valor (10)
                typableLine.substring(4, 9) +   // Campo Livre (parte 1)
                typableLine.substring(10, 20) + // Campo Livre (parte 2)
                typableLine.substring(21, 31);  // Campo Livre (parte 3)

        // DEBUG 
        System.out.println("[DEBUG] bloco para cálculo=" + block + " (len=" + block.length() + ")");
        System.out.println("[DEBUG] dvInformado=" + dvInformado);

        // Calcula DV com rotina centralizada
        int calculatedDv = calculateMod11(block);

        System.out.println("[DEBUG] DV calculado=" + calculatedDv + ", DV informado=" + dvInformado);

        return dvInformado == calculatedDv;
    }

    private void saveValidation(String barcode, BoletoValidationResponse response) {
        BoletoValidation validation = new BoletoValidation();
        validation.setBarcode(barcode != null ? barcode : "N/A"); // Boa prática para evitar nulos
        validation.setValid(response.isValid());
        validation.setBank(response.getBank());
        validation.setMessage(response.getMessage());
        validation.setValidationTimestamp(LocalDateTime.now());
        repository.save(validation);
    }
}
