# 📋 Documentação: Validação de PIX Fraudulento

## 📖 Visão Geral

Este documento descreve o sistema de validação de transações PIX para detecção de fraudes. O sistema utiliza um **score de risco** (0-100 pontos) e considera uma transação como **fraudulenta** quando o score é **≥ 35 pontos**.

---

## 🎯 Sistema de Score de Risco

### Decisão Final
- **Score < 35**: ✅ Transação **VÁLIDA**
- **Score ≥ 35**: ⚠️ Transação **FRAUDULENTA**

### Limite Máximo
O score é limitado a **100 pontos** (mesmo que a soma exceda).

---

## 🔍 Tipos de Chaves PIX Suportadas

O sistema detecta automaticamente o tipo da chave PIX através de padrões regex:

| Tipo | Padrão | Exemplo | Validação de Dígitos |
|------|--------|---------|---------------------|
| **CPF** | 11 dígitos numéricos | `12345678909` | ✅ Sim (algoritmo oficial) |
| **CNPJ** | 14 dígitos numéricos | `12345678000190` | ✅ Sim (algoritmo oficial) |
| **EMAIL** | Formato de email | `joao@email.com` | ❌ Não |
| **PHONE** | Formato internacional | `+5511999999999` | ❌ Não |
| **EVP** | UUID (chave aleatória) | `123e4567-e89b-12d3-a456-426614174000` | ❌ Não |

---

## 🛡️ Regras de Validação

### 1. Validação de Formato da Chave PIX
**Pontos:** Bloqueio imediato (score = 100)

- Verifica se a chave corresponde a um dos tipos suportados
- Se não corresponder a nenhum tipo → **FRAUDE IMEDIATA**

**Exemplo:**
```json
{
  "pixKey": "chave-invalida-123"
}
```
**Resultado:** `Formato de chave PIX inválido. Verifique o tipo da chave.`

---

### 2. Lista Negra de Chaves PIX
**Pontos:** +100

Chaves conhecidas por fraude são bloqueadas imediatamente.

**Chaves bloqueadas:**
- `12345678900` (CPF suspeito)
- `00000000000` (CPF inválido)
- `11111111111` (CPF sequencial)
- `fraudador@email.com`
- `golpe@teste.com`
- `+5511900000000`

**Exemplo:**
```json
{
  "pixKey": "12345678900"
}
```
**Resultado:** `Chave PIX está na lista negra de fraudes conhecidas.`

---

### 3. Lista Negra de Documentos
**Pontos:** +100

Documentos suspeitos são bloqueados.

**Documentos bloqueados:**
- `00000000000`
- `11111111111`
- `22222222222`
- `12345678900`

**Exemplo:**
```json
{
  "recipientDocument": "00000000000"
}
```
**Resultado:** `Documento do beneficiário está na lista negra.`

---

### 4. Validação de Banco Confiável
**Pontos:** +40

Verifica se o código do banco está na lista de bancos confiáveis.

**Bancos confiáveis:**
- `237` - Bradesco
- `341` - Itaú Unibanco
- `001` - Banco do Brasil
- `104` - Caixa Econômica Federal
- `033` - Santander
- `260` - Nu Pagamentos (Nubank)
- `077` - Banco Inter
- `290` - PagBank
- `323` - Mercado Pago
- `380` - PicPay

**Exemplo:**
```json
{
  "bankCode": "999"  // Banco desconhecido
}
```
**Resultado:** `Banco não reconhecido ou não confiável.`

---

### 5. Compatibilidade Chave-Documento
**Pontos:** +60

**Regra:** Se a chave PIX é **CPF** ou **CNPJ**, ela **DEVE** corresponder exatamente ao `recipientDocument`.

**Validação por tipo:**
- ✅ **CPF/CNPJ**: Deve corresponder ao documento
- ✅ **EMAIL/PHONE/EVP**: Não valida correspondência (sempre passa)

**Exemplo de fraude:**
```json
{
  "pixKey": "12345678909",      // CPF
  "recipientDocument": "98765432100"  // CPF diferente
}
```
**Resultado:** `Chave PIX não corresponde ao documento informado.`

---

### 6. Detecção de Valores Suspeitos
**Pontos:** +30

**Valores considerados suspeitos:**
- ❌ Valor acima de **R$ 1.000,00** (limite PIX noturno)
- ❌ Valor acima de **R$ 10.000,00** (extremamente alto)
- ❌ Valor abaixo de **R$ 1,00** (teste de fraude)
- ❌ Valores como **R$ 999,99** (próximo ao limite)

**Exemplos:**
```json
{
  "amount": 1500.00  // Acima do limite noturno
}
```
**Resultado:** `Valor acima do limite PIX noturno (R$ 1.000,00).`

```json
{
  "amount": 0.50  // Muito baixo
}
```
**Resultado:** `Valor muito baixo, típico de teste de fraude (R$ 0.50).`

---

### 7. Verificação de Nomes Suspeitos
**Pontos:** +50

**Nomes considerados suspeitos:**
- Nome vazio ou muito curto (< 3 caracteres)
- Contém palavras suspeitas: `teste`, `test`, `golpe`, `fraude`, `fake`, `falso`, `laranja`
- Muitos números no nome (> 3 dígitos)
- Nome contendo apenas números

**Exemplos:**
```json
{
  "recipientName": "Teste Golpe"
}
```
**Resultado:** `Nome contém palavra suspeita: 'teste'.`

```json
{
  "recipientName": "João123456"
}
```
**Resultado:** `Nome contém muitos números (6 dígitos).`

---

### 8. Histórico de Fraud es
**Pontos:** +40

Verifica se a mesma chave PIX já teve **mais de 2 tentativas fraudulentas** anteriores no banco de dados.

**Exemplo:**
Se a chave `12345678900` já teve 3 tentativas inválidas:
```
Chave PIX tem histórico de tentativas fraudulentas (3 tentativas).
```

---

### 9. Validação de CPF (Dígitos Verificadores)
**Pontos:** +70

**Algoritmo de validação:**
1. Verifica se tem exatamente **11 dígitos**
2. Verifica se **não** tem todos os dígitos iguais (ex: `11111111111`)
3. Calcula o **10º dígito verificador**:
   - Multiplica os 9 primeiros dígitos por pesos: `10, 9, 8, 7, 6, 5, 4, 3, 2`
   - Soma os resultados
   - Calcula: `11 - (soma % 11)`
   - Se resultado ≥ 10, dígito = 0
4. Calcula o **11º dígito verificador**:
   - Multiplica os 10 primeiros dígitos por pesos: `11, 10, 9, 8, 7, 6, 5, 4, 3, 2`
   - Soma os resultados
   - Calcula: `11 - (soma % 11)`
   - Se resultado ≥ 10, dígito = 0
5. Compara os dígitos calculados com os informados

**Exemplo de CPF inválido:**
```json
{
  "pixKey": "12345678900"  // Dígitos verificadores errados
}
```
**Resultado:** `CPF com dígitos verificadores inválidos.`

**Exemplo de CPF válido:**
```json
{
  "pixKey": "12345678909"  // CPF válido
}
```
**Resultado:** ✅ Passa na validação

---

### 10. Validação de CNPJ (Dígitos Verificadores)
**Pontos:** +70

**Algoritmo de validação:**
1. Verifica se tem exatamente **14 dígitos**
2. Verifica se **não** tem todos os dígitos iguais (ex: `11111111111111`)
3. Calcula o **13º dígito verificador**:
   - Multiplica os 12 primeiros dígitos por pesos: `5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2`
   - Soma os resultados
   - Calcula: Se `soma % 11 < 2` → dígito = 0, senão → `11 - (soma % 11)`
4. Calcula o **14º dígito verificador**:
   - Multiplica os 13 primeiros dígitos por pesos: `6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2`
   - Soma os resultados
   - Calcula: Se `soma % 11 < 2` → dígito = 0, senão → `11 - (soma % 11)`
5. Compara os dígitos calculados com os informados

**Exemplo de CNPJ inválido:**
```json
{
  "pixKey": "12345678000100"  // Dígitos verificadores errados
}
```
**Resultado:** `CNPJ com dígitos verificadores inválidos.`

---

## 📊 Tabela Resumo de Validações

| Regra | Pontos | Aplica para todos os tipos? | Observações |
|-------|--------|----------------------------|-------------|
| Formato inválido | 100 | ✅ Sim | Bloqueio imediato |
| Lista negra (chave) | 100 | ✅ Sim | Bloqueio imediato |
| Lista negra (documento) | 100 | ✅ Sim | Bloqueio imediato |
| Banco não confiável | 40 | ✅ Sim | - |
| Incompatibilidade chave-doc | 60 | ❌ Apenas CPF/CNPJ | Email/Phone/EVP não valida |
| Valor suspeito | 30 | ✅ Sim | - |
| Nome suspeito | 50 | ✅ Sim | - |
| Histórico de fraudes | 40 | ✅ Sim | - |
| CPF inválido | 70 | ❌ Apenas CPF | Valida dígitos verificadores |
| CNPJ inválido | 70 | ❌ Apenas CNPJ | Valida dígitos verificadores |

---

## 🔄 Fluxo de Validação

```
1. Recebe requisição PIX
   ↓
2. Detecta tipo da chave (CPF/CNPJ/EMAIL/PHONE/EVP)
   ↓
3. Se formato inválido → FRAUDE (100 pontos)
   ↓
4. Verifica lista negra de chaves → +100 pontos se encontrado
   ↓
5. Verifica lista negra de documentos → +100 pontos se encontrado
   ↓
6. Valida banco confiável → +40 pontos se não confiável
   ↓
7. Valida compatibilidade chave-documento → +60 pontos se incompatível
   ↓
8. Verifica valor suspeito → +30 pontos se suspeito
   ↓
9. Verifica nome suspeito → +50 pontos se suspeito
   ↓
10. Verifica histórico de fraudes → +40 pontos se > 2 tentativas
   ↓
11. Se CPF → Valida dígitos verificadores → +70 pontos se inválido
   ↓
12. Se CNPJ → Valida dígitos verificadores → +70 pontos se inválido
   ↓
13. Calcula score final (máximo 100)
   ↓
14. Se score < 35 → VÁLIDO
    Se score ≥ 35 → FRAUDULENTO
```

---

## 📝 Exemplos de Requisições

### Exemplo 1: Transação Válida (CPF)
```json
{
  "pixKey": "12345678909",
  "recipientName": "Maria Santos",
  "recipientDocument": "12345678909",
  "amount": 150.00,
  "bankCode": "237"
}
```
**Validação:**
- ✅ Formato válido (CPF)
- ✅ Não está na lista negra
- ✅ Banco confiável (Bradesco)
- ✅ Chave corresponde ao documento
- ✅ Valor normal
- ✅ Nome válido
- ✅ CPF com dígitos verificadores válidos

**Score:** 0 pontos  
**Resultado:** ✅ **VÁLIDO**

---

### Exemplo 2: Transação Fraudulenta (CPF Inválido)
```json
{
  "pixKey": "12345678900",
  "recipientName": "Teste Fraude",
  "recipientDocument": "12345678900",
  "amount": 999.99,
  "bankCode": "237"
}
```
**Validação:**
- ✅ Formato válido (CPF)
- ❌ Está na lista negra (+100 pontos)
- ✅ Banco confiável
- ✅ Chave corresponde ao documento
- ⚠️ Valor suspeito (+30 pontos)
- ❌ Nome suspeito (+50 pontos)
- ❌ CPF com dígitos verificadores inválidos (+70 pontos)

**Score:** 100 pontos (limitado)  
**Resultado:** ⚠️ **FRAUDULENTO**

---

### Exemplo 3: Transação com Chave Aleatória (EVP)
```json
{
  "pixKey": "123e4567-e89b-12d3-a456-426614174000",
  "recipientName": "João Silva",
  "recipientDocument": "98765432100",
  "amount": 200.00,
  "bankCode": "341"
}
```
**Validação:**
- ✅ Formato válido (EVP)
- ✅ Não está na lista negra
- ✅ Banco confiável (Itaú)
- ✅ Compatibilidade não valida para EVP (sempre passa)
- ✅ Valor normal
- ✅ Nome válido
- ✅ EVP não valida dígitos verificadores (não se aplica)

**Score:** 0 pontos  
**Resultado:** ✅ **VÁLIDO**

---

### Exemplo 4: Transação com Email
```json
{
  "pixKey": "joao.silva@email.com",
  "recipientName": "João Silva",
  "recipientDocument": "12345678909",
  "amount": 50.00,
  "bankCode": "260"
}
```
**Validação:**
- ✅ Formato válido (EMAIL)
- ✅ Não está na lista negra
- ✅ Banco confiável (Nubank)
- ✅ Compatibilidade não valida para EMAIL (sempre passa)
- ✅ Valor normal
- ✅ Nome válido
- ✅ EMAIL não valida dígitos verificadores (não se aplica)

**Score:** 0 pontos  
**Resultado:** ✅ **VÁLIDO**

---

## 🚨 Casos Especiais

### CPF/CNPJ com Todos os Dígitos Iguais
São automaticamente considerados inválidos:
- `11111111111` (CPF)
- `22222222222` (CPF)
- `11111111111111` (CNPJ)

**Resultado:** `CPF/CNPJ com dígitos verificadores inválidos.` (+70 pontos)

---

### Chave PIX Não Correspondente ao Documento
**Apenas para CPF e CNPJ:**
```json
{
  "pixKey": "12345678909",      // CPF
  "recipientDocument": "98765432100"  // CPF diferente
}
```
**Resultado:** `Chave PIX não corresponde ao documento informado.` (+60 pontos)

**Para EMAIL/PHONE/EVP:** Não valida correspondência (sempre passa)

---

## 📈 Score de Risco Detalhado

### Como o Score é Calculado

1. Cada regra violada adiciona pontos ao score
2. O score é acumulativo (soma de todas as violações)
3. O score máximo é limitado a **100 pontos**
4. Se o score final for **< 35**, a transação é **VÁLIDA**
5. Se o score final for **≥ 35**, a transação é **FRAUDULENTA**

### Exemplo de Cálculo

```
Transação com:
- Banco não confiável: +40 pontos
- Valor suspeito: +30 pontos
- Nome suspeito: +50 pontos
Total: 120 pontos → Limitado a 100 pontos
Score final: 100 pontos
Resultado: FRAUDULENTO
```

---

## 🔧 Configuração e Personalização

### Lista Negra de Chaves
Localização: `PixService.java` - linha 30-37

```java
private static final Set<String> BLACKLISTED_KEYS = new HashSet<>(Set.of(
    "12345678900",
    "00000000000",
    // Adicione mais chaves aqui
));
```

### Lista Negra de Documentos
Localização: `PixService.java` - linha 40-45

```java
private static final Set<String> BLACKLISTED_DOCUMENTS = new HashSet<>(Set.of(
    "00000000000",
    "11111111111",
    // Adicione mais documentos aqui
));
```

### Palavras Suspeitas em Nomes
Localização: `PixService.java` - linha 48-50

```java
private static final Set<String> SUSPICIOUS_NAME_KEYWORDS = new HashSet<>(Set.of(
    "teste", "test", "golpe", "fraude", "fake", "falso", "laranja"
    // Adicione mais palavras aqui
));
```

### Threshold de Score
Localização: `PixService.java` - linha 140

```java
boolean isValid = riskScore < 35;  // Altere o valor aqui
```

---

## 📚 Referências

- **Algoritmo CPF**: Baseado no padrão oficial da Receita Federal
- **Algoritmo CNPJ**: Baseado no padrão oficial da Receita Federal
- **Padrões PIX**: Conforme especificação do Banco Central (DICT)

---

## 🔄 Melhorias Futuras

### Recomendações para Produção:

1. **Integração com API DICT**: Consultar chaves PIX em tempo real no Banco Central
2. **Lista Negra em Banco de Dados**: Mover listas negras para tabelas no banco
3. **Machine Learning**: Implementar detecção de padrões anômalos
4. **Validação de IP**: Verificar geolocalização e histórico de IPs suspeitos
5. **Rate Limiting**: Limitar tentativas por chave PIX
6. **Integração Serasa/Receita**: Validar CPF/CNPJ com fontes oficiais
7. **Análise de Comportamento**: Detectar padrões temporais suspeitos

---

## 📞 Suporte

Para dúvidas ou sugestões sobre a validação de PIX, consulte:
- Código-fonte: `src/main/java/br/com/bradesco/safeboleto/services/PixService.java`
- Controller: `src/main/java/br/com/bradesco/safeboleto/controllers/PixController.java`
- DTOs: `src/main/java/br/com/bradesco/safeboleto/dto/PixValidationRequest.java`

---

**Última atualização:** Novembro 2025  
**Versão:** 1.0

