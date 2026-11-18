# Tarefas — Deploy em VM com Docker

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** Tasks 1 e 2 concluídas (falta marcar o pacote do GHCR como público). Task 3 implementada, com o compose único (RN-11). Falta o teste de fumaça do front door, que depende da Web Task 2.

As tasks são sequenciais, e cada uma deixa o `./mvnw verify` verde. Cada task mexe em até 15 arquivos. Specs contam, e apagar ou mover conta como um arquivo. A lista de arquivos é uma estimativa. As dependências "Web Task N" apontam para `supply-chain-verde-web/docs/features/011-deploy/tasks.md`.

---

## Task 1 — `chore/deploy-config`

Configuração que vale para qualquer ambiente. Nada de Docker novo ainda.

- [x] `git mv docker/mysql/init/01-users.sql 01-users.sh` lendo `MYSQL_APP_PASSWORD` (plan §3)
- [x] `MYSQL_APP_PASSWORD: supply_app` no `docker-compose.yml` e no `TestcontainersConfiguration`
- [x] `jwt.secret=${JWT_SECRET}` sem padrão e valor de teste em `src/test/resources/config/application.properties` (plan §4)
- [x] `server.forward-headers-strategy=framework`
- [x] README: `JWT_SECRET` no passo 4 do Quick Start, tabela de variáveis sem o padrão do JWT e o init em `.sh`
- [x] `docs/features/000-base/reference.md`: `JWT_SECRET` obrigatório

**Arquivos (~7):** `01-users.sh`, `docker-compose.yml`, `TestcontainersConfiguration`, `application.properties`, `application.properties` de teste, `README.md`, `reference.md`.

**Pronto:** `./mvnw verify` verde. `docker compose down -v && docker compose up -d` recria o banco local com `supply_app`/`supply_app`. A API não sobe sem `JWT_SECRET`.

## Task 2 — `build/docker-image`

Depende da Task 1.

- [x] `Dockerfile` em dois estágios e `.dockerignore` (plan §2)
- [x] Job `publish` no `ci.yml`, publicando no GHCR com as tags `latest` e `sha` (plan §6)
- [ ] Depois do primeiro merge: marcar o pacote como público no GHCR

**Arquivos (~3):** `Dockerfile`, `.dockerignore`, `.github/workflows/ci.yml`.

**Pronto:** `docker build -t api .` funciona localmente, e o container sobe contra o MySQL do `docker-compose.yml` com as variáveis exportadas. O push na `main` publica `ghcr.io/matheusnascimentods/supply-chain-verde-api:latest`.

## Task 3 — `build/compose`

Depende da Task 2 e da Web Task 2 (imagem do web com o Caddy). Sem a imagem do web, dá para validar tudo menos o front door, com `docker compose up -d mysql` e o `spring-boot:run`.

- [x] `docker-compose.yml` vira o compose único, com mysql, api e web. O MySQL publica a porta só em `127.0.0.1:3306`, e as senhas vêm do `.env` (plan §5)
- [x] `.env.example` com as seis variáveis, os valores sugeridos para desenvolvimento e como gerar os segredos de produção. `.env` entra no `.gitignore`
- [x] `application.properties`: importa o `.env`. O datasource e o Flyway leem `MYSQL_APP_PASSWORD` e `MYSQL_ROOT_PASSWORD`, e `app.frontend-url` cai em `https://${SITE_ADDRESS}` (plan §4)
- [x] `TestcontainersConfiguration` registra `spring.datasource.password`, para o `.env` local não vazar nos testes
- [x] README:
  - Quick Start com `.env` e `docker compose up -d --build`, sem `export`, e o `spring-boot:run` como opção para debug
  - seção **Deploy** com o passo a passo da VM (plan §7) e as contas de demonstração
  - tabela de variáveis com os nomes novos
- [x] `docs/features/000-base/reference.md`: tabela de variáveis com os nomes novos
- [ ] Teste de fumaça local (plan §9). Já validado sem o web: stack com senhas geradas, login, MySQL só no loopback, `spring-boot:run` sem `export` e `verify` com `.env` divergente. Falta o front door em `https://localhost`, que depende da Web Task 2

**Arquivos (~8):** `docker-compose.yml`, `.env.example`, `.gitignore`, `application.properties`, `TestcontainersConfiguration`, `README.md`, `reference.md`, `tasks.md`.

**Pronto:**
- Numa máquina limpa, o README sozinho basta: `cp .env.example .env` e `docker compose up -d --build` sobem tudo, e o login e o Swagger funcionam em `https://localhost`.
- O `spring-boot:run` sobe sem `export`.
- `docker compose ps` mostra a 3306 só em `127.0.0.1` e não mostra a 8080.
- O `./mvnw verify` passa mesmo com um `.env` local de senhas diferentes.
