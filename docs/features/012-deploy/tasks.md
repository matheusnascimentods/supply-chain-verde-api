# Tarefas — Deploy em VM com Docker

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** Task 1 concluída; Tasks 2 e 3 pendentes.

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

- [ ] `Dockerfile` em dois estágios e `.dockerignore` (plan §2)
- [ ] Job `publish` no `ci.yml`, publicando no GHCR com as tags `latest` e `sha` (plan §6)
- [ ] Depois do primeiro merge: marcar o pacote como público no GHCR

**Arquivos (~3):** `Dockerfile`, `.dockerignore`, `.github/workflows/ci.yml`.

**Pronto:** `docker build -t api .` funciona localmente, e o container sobe contra o MySQL do `docker-compose.yml` com as variáveis exportadas. O push na `main` publica `ghcr.io/matheusnascimentods/supply-chain-verde-api:latest`.

## Task 3 — `build/prod-compose`

Depende da Task 2 e da Web Task 2 (imagem do web com o Caddy).

- [ ] `docker-compose.prod.yml` com mysql, api e web (plan §5)
- [ ] `.env.example` com as seis variáveis e como gerar cada segredo, e `.env` no `.gitignore`
- [ ] README: seção **Deploy** com o passo a passo da VM (plan §7) e as contas de demonstração
- [ ] Teste de fumaça local com `SITE_ADDRESS=localhost` (plan §9)

**Arquivos (~4):** `docker-compose.prod.yml`, `.env.example`, `.gitignore`, `README.md`.

**Pronto:** numa máquina limpa, o README sozinho basta para subir tudo. Login e Swagger funcionam em `https://localhost`, e as portas 3306 e 8080 não aparecem em `docker compose ps`.
