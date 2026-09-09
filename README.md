# Twitter Simplificado — Spring Security + JWT

![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-7-6DB33F?logo=springsecurity&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT%20(RS256)-000000?logo=jsonwebtokens&logoColor=white)

API REST de um Twitter simplificado, construída como estudo prático e aprofundado de
**autenticação e autorização** com Spring Security: login stateless via JWT assinado com
par de chaves RSA, controle de permissão por role, e validação de posse de recurso
(um usuário não mexe no que não é dele).

Não é um CRUD de fachada — cada rota tem uma regra de autorização real por trás, e o projeto
inclui um exercício de auditoria (issues de segurança e arquitetura documentadas abaixo, de
propósito, para deixar claro o raciocínio de trade-off de uma POC).

---

## ✨ Funcionalidades

- **Cadastro de usuário** com senha criptografada (BCrypt)
- **Login** com emissão de JWT assinado via RSA (RS256) — token carrega `sub`, `iss`, `exp`, `iat` e `scope` (roles)
- **Autorização por role** (`ADMIN` / `BASIC`) usando o claim `scope` do próprio token, sem round-trip ao banco a cada requisição
- **Rota exclusiva de admin** para listar usuários (sem expor senha no payload)
- **Tweets**: criação e deleção com validação de posse — só o dono deleta, exceto admin (super-poder)
- **Feed paginado**, ordenado do tweet mais recente para o mais antigo

## 🧱 Stack

| Camada | Tecnologia |
|---|---|
| Linguagem / Runtime | Java 25 |
| Framework | Spring Boot 4.1.1 |
| Segurança | Spring Security 7 + OAuth2 Resource Server (JWT, RS256) |
| Persistência | Spring Data JPA / Hibernate |
| Banco de dados | MySQL 8 (via Docker Compose) |
| Build | Maven (com wrapper, `./mvnw`) |
| Testes manuais | Coleção Postman incluída (`postman/`) |

## 🔐 Como a autenticação funciona

1. Servidor gera um par de chaves RSA (privada/pública) — a privada **nunca** sai do backend.
2. No login, o servidor valida usuário/senha, monta os claims do JWT e **assina com a chave privada**.
3. Em cada requisição autenticada, o servidor valida a assinatura do token usando a **chave pública** — não precisa consultar sessão nenhuma (100% stateless).
4. A autorização por role usa o claim `scope` embutido no próprio token (`SCOPE_ADMIN`, por exemplo), e a validação de posse de recurso (ex.: deletar um tweet) compara o dono do dado com o `sub` do token — com um double-check no banco para o super-poder do admin.

---

## 🚀 Como rodar localmente

### Pré-requisitos

- JDK 25
- Docker e Docker Compose
- OpenSSL (já vem por padrão no Linux/macOS e no Git Bash do Windows)

### 1. Clonar e gerar as chaves JWT

As chaves RSA **não são versionadas** (por segurança — nunca comite uma chave privada). Gere o seu próprio par:

```bash
cd src/main/resources
openssl genrsa -out app.key 2048
openssl rsa -in app.key -pubout -out app.pub
cd ../../..
```

Isso cria `app.key` (privada) e `app.pub` (pública) dentro de `src/main/resources` — exatamente onde a aplicação espera encontrá-las.

### 2. Subir o banco de dados

```bash
cd docker
docker compose up -d
cd ..
```

Sobe um MySQL 8 na porta `33069` do host (porta não-padrão, de propósito, para não colidir com outros bancos locais).

### 3. Rodar a aplicação

```bash
./mvnw spring-boot:run
```

Na primeira subida, um usuário administrador é criado automaticamente:

| username | password | role |
|---|---|---|
| `admin` | `123` | ADMIN |

A aplicação sobe em `http://localhost:8080`.

### 4. Testar

Importe a coleção `postman/spring-security-jwt.postman_collection.json` no Postman — o token de acesso é capturado automaticamente após qualquer login e injetado nas próximas requisições.

> Variáveis de ambiente opcionais: `DB_USERNAME` e `DB_PASSWORD` (têm default e funcionam sem configurar nada extra em ambiente local).

---

## 📡 Endpoints

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/login` | Pública | Autentica e retorna um JWT |
| `POST` | `/users` | Pública | Cria uma nova conta (role BASIC) |
| `GET` | `/users` | `SCOPE_ADMIN` | Lista todos os usuários |
| `POST` | `/tweets` | Autenticado | Cria um tweet do usuário logado |
| `DELETE` | `/tweets/{id}` | Autenticado | Deleta um tweet (dono ou admin) |
| `GET` | `/feed?page=&pageSize=` | Autenticado | Feed paginado, mais recente primeiro |

## 📂 Estrutura do projeto

```
src/main/java/com/buildrun/springsecurityjwt/
├── config/          # SecurityConfig (JWT, filtros) + seed do usuário admin
├── controller/       # Endpoints REST
│   └── dto/          # Records de request/response
├── entities/          # User, Role, Tweet (JPA)
└── repository/        # Spring Data JPA repositories

docker/                # docker-compose.yml (MySQL)
postman/                # Coleção de testes prontos
```

---

## 🧭 Do estado de POC para produção

Este projeto nasceu como exercício de aprendizado, não como serviço em produção. Documentar
essa distância de propósito é parte do exercício — é o tipo de raciocínio que se espera de
quem vai operar isso de verdade um dia:

- **Sem revogação de token** — JWT stateless não permite invalidar antes da expiração; produção pediria refresh token de vida curta + estratégia de blacklist.
- **Sem rate limiting no `/login`** — hoje não há proteção contra força bruta.
- **Chave RSA única, sem rotação nem `kid`** — trocar a chave um dia invalida todo token emitido de uma vez.
- **Lógica de negócio dentro dos Controllers** — falta uma camada de Service, o que dificulta teste unitário isolado do contexto web.
- **Sem testes automatizados** — especialmente crítico nas regras de autorização (validação de posse), que é onde bug de segurança mais se esconde.
- **`ddl-auto=update`** — ótimo para desenvolvimento, arriscado em produção; o caminho é Flyway/Liquibase com migrations versionadas.
- **`@ManyToMany(fetch = EAGER)`** entre User e Role — funciona bem com 2 roles fixas, mas não escalaria sem revisão de estratégia de fetch.

## 🎓 Origem

Construído acompanhando o vídeo [**Guia Definitivo: Spring Security 6 + JWT + OAuth2**](https://www.youtube.com/watch?v=nDst-CRKt_k), do canal **Build & Run**, com adaptações, correções e extensões ao longo do caminho (incluindo um bug de autorização em `/error` que o vídeo original deixou sem solução).

## 🤝 Contribuindo

Pull requests são bem-vindos. Ideias de continuação natural: cobertura de testes de autorização, camada de Service, refresh token, rate limiting no login — veja a seção acima para o roteiro completo de melhorias mapeadas.
