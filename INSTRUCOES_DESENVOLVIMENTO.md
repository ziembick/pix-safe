# Manual Técnico e de Desenvolvimento - SafeBoleto API

Este documento serve como um guia completo para desenvolvedores que trabalham no projeto SafeBoleto API. Ele cobre a estrutura do projeto, o gerenciamento com Docker e Git, e a arquitetura do backend.

## 1. Guia de Comandos Essenciais

### 1.1. Docker

O Docker é usado para criar um ambiente de desenvolvimento e produção consistente e isolado.

-   **Construir e Iniciar os Contêineres (App + Banco de Dados):**
    ```bash
    # Executa em segundo plano (-d) e força a reconstrução da imagem (--build)
    docker-compose up -d --build
    ```

-   **Parar e Remover os Contêineres:**
    ```bash
    docker-compose down
    ```

-   **Visualizar Logs da Aplicação em Tempo Real:**
    ```bash
    # O '-f' (follow) mostra os logs continuamente
    docker-compose logs -f app
    ```

-   **Acessar o Banco de Dados PostgreSQL Diretamente:**
    ```bash
    # Abre um shell interativo dentro do contêiner do banco
    docker-compose exec db psql -U ${POSTGRES_USER} -d safeboleto_db
    ```
    *(Você pode precisar substituir `${POSTGRES_USER}` pelo valor real do seu arquivo `.env`)*

### 1.2. Git (Fluxo de Trabalho)

O Git é usado para versionamento de código.

-   **Verificar o Status dos Arquivos:**
    ```bash
    git status
    ```

-   **Adicionar Arquivos para o Próximo Commit:**
    ```bash
    # Adiciona um arquivo específico
    git add <caminho/do/arquivo>

    # Adiciona todos os arquivos modificados
    git add .
    ```

-   **Salvar as Alterações (Commit):**
    ```bash
    git commit -m "Sua mensagem descritiva aqui"
    ```

-   **Enviar as Alterações para o Repositório Remoto (GitHub):**
    ```bash
    git push origin <nome-da-branch>
    ```

-   **Criar uma Nova Branch para uma Funcionalidade:**
    ```bash
    git checkout -b nome-da-nova-feature
    ```

## 2. Arquitetura e Funcionalidades do Backend

### 2.1. Estrutura de Pacotes

-   `controllers`: Define os endpoints da API REST. Recebe as requisições HTTP e retorna as respostas.
-   `services`: Contém a lógica de negócio principal da aplicação.
-   `model`: Define as entidades JPA que representam as tabelas do banco de dados.
-   `repositories`: Interfaces do Spring Data JPA para interagir com o banco de dados.
-   `dto`: (Data Transfer Objects) Objetos simples para transferir dados entre as camadas (ex: requisições e respostas da API).
-   `security`: Classes relacionadas à configuração do Spring Security e gerenciamento de tokens JWT.
-   `config`: Classes de configuração geral, como a inicialização de dados.

### 2.2. Fluxo de Autenticação e Segurança

1.  **Requisição de Login**: O usuário envia `username` e `password` para `POST /api/auth/login`.
2.  **`AuthenticationController`**: Recebe a requisição e usa o `AuthenticationManager` do Spring Security para validar as credenciais.
3.  **`JwtService`**: Se a autenticação for bem-sucedida, este serviço é chamado para gerar um token JWT. O token inclui o nome do usuário (`sub`) e suas permissões (`roles`) como "claims".
4.  **Requisições Protegidas**: Para acessar endpoints como `POST /api/boleto/valida`, o cliente deve enviar o token no cabeçalho `Authorization: Bearer <token>`.
5.  **`JwtAuthenticationFilter`**: Este filtro intercepta todas as requisições. Se um token JWT válido é encontrado, ele extrai as informações (usuário e `roles`) e cria um objeto de autenticação, inserindo-o no `SecurityContextHolder`. Isso torna o usuário "logado" para aquela requisição.
6.  **`@PreAuthorize`**: A anotação no `BoletoController` (`@PreAuthorize("hasAnyRole('USER', 'ADMIN')")`) verifica se o usuário autenticado pelo filtro possui a permissão necessária para executar o método.

### 2.3. Fluxo de Validação de Boletos

1.  **`BoletoController`**: Recebe a requisição em `POST /api/boleto/valida` com a linha digitável.
2.  **`BoletoService`**: Orquestra toda a lógica de validação:
    a.  **Validação de Formato**: Verifica se a linha tem 47 dígitos numéricos.
    b.  **Checagem de Banco Confiável**: Extrai o código do banco (3 primeiros dígitos) e consulta a tabela `trusted_banks` através do `TrustedBankRepository`. Se não encontrar, a validação falha.
    c.  **Validação de Dígitos Verificadores (Módulo 10)**: Calcula o dígito verificador de cada um dos três primeiros campos da linha digitável e os compara com os dígitos informados.
    d.  **Validação do Dígito Verificador Geral (Módulo 11)**: Remonta o código de barras de 44 posições e calcula seu dígito verificador geral, comparando-o com o dígito informado na linha digitável.
3.  **Persistência**: O resultado de cada tentativa de validação (seja sucesso ou falha) é salvo na tabela `boleto_validation` através do `BoletoValidationRepository`.
4.  **Resposta**: O serviço retorna um `BoletoValidationResponse` com o status (`valid`), o nome do banco e uma mensagem descritiva.

### 2.4. Inicialização de Dados (`DataInitializer`)

Esta classe é executada na inicialização da aplicação e tem duas funções principais:
1.  **Criar Usuário Padrão**: Verifica se o usuário `admin` existe. Se não, cria-o com a senha padrão definida em `application.properties` (`app.admin.initial-password=password`) e atribui as roles `ADMIN` e `USER`.
2.  **Popular Bancos Confiáveis**: Insere na tabela `trusted_banks` uma lista inicial de bancos brasileiros (Bradesco, Itaú, etc.) para que a validação de boletos possa funcionar imediatamente.

---