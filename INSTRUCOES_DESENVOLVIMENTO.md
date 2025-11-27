# Manual Técnico e de Desenvolvimento - SafePix API

Este documento serve como um guia completo para desenvolvedores que trabalham no projeto SafePix API. Ele cobre a estrutura do projeto, o gerenciamento com Docker e Git, e a arquitetura do backend.

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
    docker-compose exec db psql -U ${POSTGRES_USER} -d db_pix_safe
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
4.  **Requisições Protegidas**: Para acessar endpoints como `POST /api/pix/valida`, o cliente deve enviar o token no cabeçalho `Authorization: Bearer <token>`.
5.  **`JwtAuthenticationFilter`**: Este filtro intercepta todas as requisições. Se um token JWT válido é encontrado, ele extrai as informações (usuário e `roles`) e cria um objeto de autenticação, inserindo-o no `SecurityContextHolder`. Isso torna o usuário "logado" para aquela requisição.
6.  **`@PreAuthorize`**: A anotação no `PixController` (`@PreAuthorize("hasAnyRole('USER', 'ADMIN')")`) verifica se o usuário autenticado pelo filtro possui a permissão necessária para executar o método.

### 2.3. Fluxo de Validação de Transações PIX

1.  **`PixController`**: Recebe a requisição em `POST /api/pix/valida` com os dados da transação PIX.
2.  **`PixService`**: Orquestra toda a lógica de validação e detecção de fraudes:
    a.  **Validação de Formato da Chave PIX**: Detecta o tipo de chave (CPF, CNPJ, Email, Telefone ou EVP) e valida o formato.
    b.  **Verificação de Lista Negra**: Verifica se a chave PIX ou documento do beneficiário estão em listas negras de fraudes conhecidas.
    c.  **Checagem de Banco Confiável**: Consulta a tabela `trusted_banks` através do `TrustedBankRepository` para verificar se o banco é confiável.
    d.  **Validação de Compatibilidade**: Verifica se a chave PIX corresponde ao documento informado (para chaves CPF/CNPJ).
    e.  **Detecção de Valores Suspeitos**: Analisa valores muito altos, muito baixos ou próximos a limites.
    f.  **Verificação de Nomes Suspeitos**: Detecta palavras-chave suspeitas ou padrões anômalos em nomes.
    g.  **Validação de CPF/CNPJ**: Calcula e valida os dígitos verificadores de CPF e CNPJ quando aplicável.
    h.  **Histórico de Fraudas**: Verifica se a chave PIX tem histórico de tentativas fraudulentas anteriores.
3.  **Cálculo de Score de Risco**: O sistema calcula um score de risco de 0-100 baseado em todos os indicadores verificados.
4.  **Persistência**: O resultado de cada tentativa de validação (seja sucesso ou falha) é salvo na tabela `pix_validations` através do `PixValidationRepository`.
5.  **Resposta**: O serviço retorna um `PixValidationResponse` com o status (`valid`), o tipo de chave, nome do banco, score de risco e uma mensagem descritiva.

### 2.4. Inicialização de Dados (`DataInitializer`)

Esta classe é executada na inicialização da aplicação e tem duas funções principais:
1.  **Criar Usuário Padrão**: Verifica se o usuário `admin` existe. Se não, cria-o com a senha padrão definida em `application.properties` (`app.admin.initial-password=password`) e atribui as roles `ADMIN` e `USER`.
2.  **Popular Bancos Confiáveis**: Insere na tabela `trusted_banks` uma lista inicial de bancos brasileiros (Bradesco, Itaú, etc.) para que a validação de transações PIX possa funcionar imediatamente.

---