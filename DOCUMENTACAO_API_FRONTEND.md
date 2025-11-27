# 📚 Documentação da API SafePix - Endpoints para Frontend

## 🌐 Base URL

```
http://localhost:8080
```

**Nota:** Em produção, substitua `localhost:8080` pela URL do servidor.

---

## 🔐 Autenticação

A API utiliza **JWT (JSON Web Token)** para autenticação. A maioria dos endpoints requer um token válido no header `Authorization`.

### Fluxo de Autenticação:
1. Fazer login em `/api/auth/login` para obter o token
2. Armazenar o token (localStorage, sessionStorage, etc.)
3. Incluir o token em todas as requisições protegidas: `Authorization: Bearer <token>`

---

## 📍 Endpoints Disponíveis

### 1. Health Check

Verifica se a API está funcionando.

**Endpoint:** `GET /`

**Autenticação:** ❌ Não requerida

**Headers:**
```
Nenhum header especial necessário
```

**Resposta de Sucesso (200 OK):**
```json
{
  "status": "UP"
}
```

**Exemplo de Uso:**
```javascript
const response = await fetch('http://localhost:8080/');
const data = await response.json();
console.log(data); // { status: "UP" }
```

---

### 2. Login

Realiza autenticação e retorna token JWT.

**Endpoint:** `POST /api/auth/login`

**Autenticação:** ❌ Não requerida

**Headers:**
```
Content-Type: application/json
```

**Body (JSON):**
```json
{
  "username": "admin",
  "password": "password"
}
```

**Validações:**
- `username`: obrigatório, não pode ser vazio
- `password`: obrigatório, não pode ser vazio

**Resposta de Sucesso (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Resposta de Erro (401 Unauthorized):**
```json
{
  "timestamp": "2024-01-01T12:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Credenciais inválidas",
  "path": "/api/auth/login"
}
```

**Exemplo de Uso:**
```javascript
async function login(username, password) {
  try {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        username: username,
        password: password
      })
    });

    if (!response.ok) {
      throw new Error('Credenciais inválidas');
    }

    const data = await response.json();
    localStorage.setItem('token', data.token);
    return data.token;
  } catch (error) {
    console.error('Erro no login:', error);
    throw error;
  }
}

// Uso
const token = await login('admin', 'password');
```

---

### 3. Validar Transação PIX

Valida uma transação PIX com detecção avançada de fraudes.

**Endpoint:** `POST /api/pix/valida`

**Autenticação:** ✅ Requerida (JWT Token)

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <seu_token_jwt>
```

**Body (JSON):**
```json
{
  "pixKey": "12345678909",
  "recipientName": "Maria Santos",
  "recipientDocument": "12345678909",
  "amount": 150.50,
  "bankCode": "237"
}
```

**Validações:**
- `pixKey`: obrigatório, não pode ser vazio
  - Aceita: CPF (11 dígitos), CNPJ (14 dígitos), e-mail, telefone (+5511999999999), chave aleatória (UUID)
- `recipientName`: obrigatório, não pode ser vazio
- `recipientDocument`: obrigatório, não pode ser vazio (CPF ou CNPJ apenas números)
- `amount`: obrigatório, deve ser positivo (Double)
- `bankCode`: obrigatório, não pode ser vazio (3 dígitos)
  - Exemplos: "237" (Bradesco), "341" (Itaú), "001" (Banco do Brasil)

**Resposta de Sucesso (200 OK) - Transação Válida:**
```json
{
  "valid": true,
  "pixKey": "12345678909",
  "keyType": "CPF",
  "recipientName": "Maria Santos",
  "bankCode": "237",
  "bankName": "Bradesco",
  "message": "Transação PIX válida e segura. Score de risco: 0/100",
  "riskScore": 0
}
```

**Resposta de Sucesso (200 OK) - Transação Suspeita:**
```json
{
  "valid": false,
  "pixKey": "12345678900",
  "keyType": "CPF",
  "recipientName": "Teste Fraude",
  "bankCode": "237",
  "bankName": "Bradesco",
  "message": "⚠️ TRANSAÇÃO SUSPEITA DE FRAUDE! Motivos: Chave PIX está na lista negra. Nome contém palavra suspeita.",
  "riskScore": 100
}
```

**Códigos de Status HTTP:**
- `200 OK`: Validação realizada com sucesso
- `400 Bad Request`: Dados inválidos ou mal formatados
- `401 Unauthorized`: Token JWT inválido ou ausente
- `403 Forbidden`: Usuário sem permissão (não possui role USER ou ADMIN)

**Exemplo de Uso:**
```javascript
async function validatePix(pixData) {
  const token = localStorage.getItem('token');
  
  if (!token) {
    throw new Error('Token não encontrado. Faça login primeiro.');
  }

  try {
    const response = await fetch('http://localhost:8080/api/pix/valida', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        pixKey: pixData.pixKey,
        recipientName: pixData.recipientName,
        recipientDocument: pixData.recipientDocument,
        amount: pixData.amount,
        bankCode: pixData.bankCode
      })
    });

    if (response.status === 401) {
      // Token expirado ou inválido
      localStorage.removeItem('token');
      throw new Error('Sessão expirada. Faça login novamente.');
    }

    if (response.status === 403) {
      throw new Error('Você não tem permissão para acessar este recurso.');
    }

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Erro ao validar PIX');
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error('Erro ao validar PIX:', error);
    throw error;
  }
}

// Uso
const pixData = {
  pixKey: '12345678909',
  recipientName: 'Maria Santos',
  recipientDocument: '12345678909',
  amount: 150.50,
  bankCode: '237'
};

const result = await validatePix(pixData);
console.log('Transação válida:', result.valid);
console.log('Score de risco:', result.riskScore);
console.log('Mensagem:', result.message);
```

---

## 🔧 Função Helper Completa para Frontend

Aqui está uma classe completa para facilitar o uso da API no frontend:

```javascript
class SafePixAPI {
  constructor(baseURL = 'http://localhost:8080') {
    this.baseURL = baseURL;
  }

  // Obter token do localStorage
  getToken() {
    return localStorage.getItem('token');
  }

  // Salvar token no localStorage
  setToken(token) {
    localStorage.setItem('token', token);
  }

  // Remover token
  removeToken() {
    localStorage.removeItem('token');
  }

  // Verificar se está autenticado
  isAuthenticated() {
    return !!this.getToken();
  }

  // Fazer requisição autenticada
  async authenticatedFetch(endpoint, options = {}) {
    const token = this.getToken();
    
    if (!token) {
      throw new Error('Não autenticado. Faça login primeiro.');
    }

    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      ...options.headers
    };

    const response = await fetch(`${this.baseURL}${endpoint}`, {
      ...options,
      headers
    });

    if (response.status === 401) {
      this.removeToken();
      throw new Error('Sessão expirada. Faça login novamente.');
    }

    return response;
  }

  // Health Check
  async healthCheck() {
    const response = await fetch(`${this.baseURL}/`);
    return await response.json();
  }

  // Login
  async login(username, password) {
    const response = await fetch(`${this.baseURL}/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ username, password })
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Erro ao fazer login');
    }

    const data = await response.json();
    this.setToken(data.token);
    return data;
  }

  // Validar PIX
  async validatePix(pixData) {
    const response = await this.authenticatedFetch('/api/pix/valida', {
      method: 'POST',
      body: JSON.stringify(pixData)
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Erro ao validar PIX');
    }

    return await response.json();
  }
}

// Uso da classe
const api = new SafePixAPI('http://localhost:8080');

// Exemplo completo de fluxo
async function exemploCompleto() {
  try {
    // 1. Verificar saúde da API
    const health = await api.healthCheck();
    console.log('API está funcionando:', health.status === 'UP');

    // 2. Fazer login
    await api.login('admin', 'password');
    console.log('Login realizado com sucesso!');

    // 3. Validar PIX
    const resultado = await api.validatePix({
      pixKey: '12345678909',
      recipientName: 'Maria Santos',
      recipientDocument: '12345678909',
      amount: 150.50,
      bankCode: '237'
    });

    console.log('Validação PIX:', resultado);
    console.log('Transação válida:', resultado.valid);
    console.log('Score de risco:', resultado.riskScore);
  } catch (error) {
    console.error('Erro:', error.message);
  }
}
```

---

## 📋 Códigos de Bancos Comuns

| Código | Banco |
|--------|-------|
| 237 | Bradesco |
| 341 | Itaú Unibanco |
| 001 | Banco do Brasil |
| 104 | Caixa Econômica Federal |
| 033 | Santander |
| 260 | Nu Pagamentos (Nubank) |
| 077 | Banco Inter |
| 290 | PagBank |
| 323 | Mercado Pago |
| 380 | PicPay |

---

## ⚠️ Tratamento de Erros

Sempre trate os seguintes cenários:

1. **401 Unauthorized**: Token expirado ou inválido
   - Remova o token do storage
   - Redirecione para a página de login

2. **403 Forbidden**: Usuário sem permissão
   - Mostre mensagem de erro apropriada

3. **400 Bad Request**: Dados inválidos
   - Valide os dados antes de enviar
   - Mostre mensagens de erro específicas

4. **500 Internal Server Error**: Erro no servidor
   - Mostre mensagem genérica de erro
   - Tente novamente mais tarde

---

## 🎯 Exemplo com React/TypeScript

```typescript
import { useState } from 'react';

interface PixValidationRequest {
  pixKey: string;
  recipientName: string;
  recipientDocument: string;
  amount: number;
  bankCode: string;
}

interface PixValidationResponse {
  valid: boolean;
  pixKey: string;
  keyType: string;
  recipientName: string;
  bankCode: string;
  bankName: string;
  message: string;
  riskScore: number;
}

function usePixValidation() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const validatePix = async (data: PixValidationRequest): Promise<PixValidationResponse> => {
    setLoading(true);
    setError(null);

    try {
      const token = localStorage.getItem('token');
      
      if (!token) {
        throw new Error('Não autenticado');
      }

      const response = await fetch('http://localhost:8080/api/pix/valida', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(data)
      });

      if (!response.ok) {
        if (response.status === 401) {
          localStorage.removeItem('token');
          throw new Error('Sessão expirada');
        }
        throw new Error('Erro ao validar PIX');
      }

      const result = await response.json();
      return result;
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Erro desconhecido';
      setError(message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return { validatePix, loading, error };
}
```

---

## 📝 Notas Importantes

1. **Token JWT**: O token tem validade limitada (padrão: 10 horas). Implemente renovação automática se necessário.

2. **CORS**: Se houver problemas de CORS, configure o backend para aceitar requisições do seu domínio frontend.

3. **Rate Limiting**: A API pode ter limitações de taxa. Implemente retry com backoff exponencial se necessário.

4. **Validação no Frontend**: Sempre valide os dados no frontend antes de enviar, mas não confie apenas nisso - o backend também valida.

5. **Segurança**: Nunca exponha o token JWT em logs ou mensagens de erro. Armazene-o de forma segura.

---

## 🔄 Exemplo de Integração Completa (React)

```typescript
import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080';

interface LoginFormData {
  username: string;
  password: string;
}

interface PixFormData {
  pixKey: string;
  recipientName: string;
  recipientDocument: string;
  amount: number;
  bankCode: string;
}

const PixValidationApp: React.FC = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationResult, setValidationResult] = useState<any>(null);

  useEffect(() => {
    // Verificar se há token salvo
    const token = localStorage.getItem('token');
    setIsAuthenticated(!!token);
  }, []);

  const handleLogin = async (formData: LoginFormData) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
      });

      if (!response.ok) {
        throw new Error('Credenciais inválidas');
      }

      const data = await response.json();
      localStorage.setItem('token', data.token);
      setIsAuthenticated(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao fazer login');
    } finally {
      setLoading(false);
    }
  };

  const handleValidatePix = async (formData: PixFormData) => {
    setLoading(true);
    setError(null);
    setValidationResult(null);

    try {
      const token = localStorage.getItem('token');
      
      const response = await fetch(`${API_BASE_URL}/api/pix/valida`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(formData)
      });

      if (response.status === 401) {
        localStorage.removeItem('token');
        setIsAuthenticated(false);
        throw new Error('Sessão expirada. Faça login novamente.');
      }

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Erro ao validar PIX');
      }

      const data = await response.json();
      setValidationResult(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao validar PIX');
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    setIsAuthenticated(false);
    setValidationResult(null);
  };

  if (!isAuthenticated) {
    return (
      <LoginForm 
        onSubmit={handleLogin} 
        loading={loading} 
        error={error} 
      />
    );
  }

  return (
    <div>
      <button onClick={handleLogout}>Sair</button>
      <PixValidationForm 
        onSubmit={handleValidatePix} 
        loading={loading} 
        error={error}
        result={validationResult}
      />
    </div>
  );
};

export default PixValidationApp;
```

---

## 📞 Suporte

Para mais informações sobre a API, consulte:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Documentação OpenAPI**: http://localhost:8080/v3/api-docs

