# Plano técnico — Deploy em VM com Docker

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Visão geral

```
                  VM Linux (qualquer provedor) — só 80/443 abertas
 internet ──► web (Caddy) ──┬── /api/*, /swagger-ui*, /v3/api-docs* ──► api:8080 ──► mysql:3306
              HTTPS auto    └── qualquer outra rota ──► Angular (/srv)
```

São três containers num compose só, na mesma rede interna. O Caddy fica na imagem do web (`supply-chain-verde-web/docs/features/011-deploy/plan.md`) e emite o certificado Let's Encrypt sozinho, desde que o DNS do domínio aponte para a VM.

A máquina de quem desenvolve roda o mesmo `docker-compose.yml`, com `SITE_ADDRESS=localhost` no `.env`. Nesse caso, o Caddy usa uma CA local em vez do Let's Encrypt (RN-11).

## 2. Imagem da API (`Dockerfile`)

Build em dois estágios:

```dockerfile
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system app
USER app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- **Testes fora da imagem.** O `-DskipTests` só vale aqui. O CI já roda o `verify` com Testcontainers antes de publicar (§6), e rodar Testcontainers dentro de um `docker build` exigiria Docker dentro do build.
- **Cache:** o `dependency:go-offline` fica numa camada separada. Uma mudança só em `src` não baixa as dependências de novo.
- **Usuário sem privilégio** (`app`), não root.
- **Memória:** a JVM usa por padrão 25% da RAM que enxerga. Numa VM de 4 GB, isso dá cerca de 1 GB de heap, o suficiente. Só vale ajustar `-XX:MaxRAMPercentage` se aparecer falta de memória.
- `.dockerignore`: `target/`, `.git/`, `.idea/`, `docs/`, `*.iml`. Assim o contexto de build fica pequeno e não leva lixo local.

## 3. Senha do `supply_app` no init do MySQL

`docker/mysql/init/01-users.sql` vira `01-users.sh` (`git mv`, para manter o histórico):

```bash
#!/bin/bash
# A senha entra numa string SQL: use só letras e números (ex.: openssl rand -hex 24).
mysql --protocol=socket -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE USER 'supply_audit'@'%' IDENTIFIED BY RANDOM PASSWORD ACCOUNT LOCK;
CREATE USER 'supply_app'@'%' IDENTIFIED BY '${MYSQL_APP_PASSWORD:?defina MYSQL_APP_PASSWORD}';
SQL
```

- A imagem `mysql` roda arquivos `.sh` de `/docker-entrypoint-initdb.d` na mesma ordem dos `.sql`, depois de definir a senha de root. Se o arquivo não for executável, ela faz `source` dele. Por isso o script não usa `set -u` (afetaria o entrypoint) e funciona dos dois jeitos.
- Sem `MYSQL_APP_PASSWORD`, o `:?` interrompe o init e o container não sobe. É melhor isso do que criar um usuário com senha vazia.
- **Desenvolvimento:** na Task 1, `docker-compose.yml` ganha `MYSQL_APP_PASSWORD: supply_app`, igual ao padrão do `application.properties`. Na Task 3, o valor passa a vir do `.env` (§5).
- **Testes:** `TestcontainersConfiguration` ganha `.withEnv("MYSQL_APP_PASSWORD", "supply_app")`.
- **Volumes que já existem** não mudam, porque o init só roda na criação do volume. Ninguém precisa recriar o banco local, desde que o `.env` local use as senhas com que o volume foi criado (§5).

## 4. Configuração (`application.properties`)

| Propriedade | Hoje | Depois |
|---|---|---|
| `jwt.secret` | `${JWT_SECRET:404E63...}` | `${JWT_SECRET}`, sem padrão (RN-02) |
| `server.forward-headers-strategy` | — | `framework` (RN-08) |
| `spring.config.import` | — | `optional:file:.env[.properties]` (Task 3, RN-11) |
| `spring.datasource.password` | `${SPRING_DATASOURCE_PASSWORD:supply_app}` | `${MYSQL_APP_PASSWORD:supply_app}` (Task 3) |
| `spring.flyway.password` | `${FLYWAY_PASSWORD:root}` | `${MYSQL_ROOT_PASSWORD:root}` (Task 3) |
| `app.frontend-url` | `${APP_FRONTEND_URL}` | `${APP_FRONTEND_URL:https://${SITE_ADDRESS}}` (Task 3) |

- **Testes:** `src/test/resources/config/application.properties` ganha um `jwt.secret` de 256 bits só para os testes, ao lado dos valores do SendGrid que já estão lá.
- **Desenvolvimento local:** na Task 1, o passo 4 do Quick Start no README passa a exportar `JWT_SECRET` junto com as variáveis do SendGrid, e o README sugere `openssl rand -hex 32`. Na Task 3, o `export` sai: o Spring lê o `.env` da raiz pelo `spring.config.import`.
- **Os mesmos nomes no `.env`, no compose e no Spring.** A senha do datasource e a do Flyway passam a ler `MYSQL_APP_PASSWORD` e `MYSQL_ROOT_PASSWORD`, as mesmas variáveis que o init do MySQL usa. Assim, o `.env` não repete a mesma senha com dois nomes. `SPRING_DATASOURCE_PASSWORD` e `FLYWAY_PASSWORD` deixam de existir.
- **`app.frontend-url` sai de `SITE_ADDRESS`:** o link do convite aponta para `https://<SITE_ADDRESS>`. Quem roda o front com `ng serve` define `APP_FRONTEND_URL=http://localhost:4200` no `.env`, e esse valor tem prioridade. O CORS mantém o padrão `http://localhost:4200`, porque em produção front e API ficam na mesma origem (RN-06).
- **O import é `optional`:** dentro do container não há `.env` em `/app`, e lá o compose entrega as variáveis como ambiente. No CI também não há `.env`.
- **O `.env` local não pode vazar nos testes.** Rodando da raiz, o `./mvnw verify` também importaria o `.env`, e um `MYSQL_APP_PASSWORD` diferente de `supply_app` quebraria a conexão com o Testcontainers. Por isso, o `TestcontainersConfiguration` registra `spring.datasource.password=supply_app`, junto com a URL.
- `framework` usa o `ForwardedHeaderFilter` do Spring. Só o Caddy alcança a API, porque ela não publica porta (RN-05), então confiar nos `X-Forwarded-*` é seguro.
- O Flyway (`root`) e o datasource (`supply_app`) mantêm os padrões de desenvolvimento para quando não houver `.env` (RN-04).

## 5. Compose único (`docker-compose.yml`)

O `docker-compose.yml` de desenvolvimento passa a ser o compose de todos os ambientes (RN-11):

```yaml
services:
  mysql:
    image: mysql:8.4
    restart: unless-stopped
    command: --default-time-zone=America/Sao_Paulo --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_as_cs
    environment:
      MYSQL_DATABASE: supply_chain_verde
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:?}
      MYSQL_APP_PASSWORD: ${MYSQL_APP_PASSWORD:?}
    ports:
      - "127.0.0.1:3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docker/mysql/init:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h localhost -p$$MYSQL_ROOT_PASSWORD"]
      interval: 5s
      retries: 20

  api:
    image: ghcr.io/matheusnascimentods/supply-chain-verde-api:latest
    build: .
    restart: unless-stopped
    depends_on:
      mysql: { condition: service_healthy }
    env_file: .env
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/supply_chain_verde?connectionTimeZone=SERVER

  web:
    image: ghcr.io/matheusnascimentods/supply-chain-verde-web:latest
    restart: unless-stopped
    depends_on: [api]
    ports: ["80:80", "443:443", "443:443/udp"]
    environment:
      SITE_ADDRESS: ${SITE_ADDRESS:?}
    volumes:
      - caddy_data:/data
      - caddy_config:/config

volumes:
  mysql_data:
  caddy_data:
  caddy_config:
```

- **Um compose só, sem `.prod`.** Desenvolvimento e produção sobem os mesmos serviços, da mesma imagem, e só o `.env` muda. Um arquivo separado para a VM deixaria o que roda localmente divergir do que vai para produção.
- **MySQL só no loopback.** `127.0.0.1:3306` atende a IDE e o `spring-boot:run` na máquina de quem desenvolve. Na VM, a porta só responde de dentro dela (RN-05). Ligar no `127.0.0.1`, e não em `0.0.0.0`, também evita que as regras de iptables do Docker passem por cima do firewall (UFW) da VM.
- **`image` e `build` juntos na API.**
  - Localmente, `docker compose up -d --build` monta a imagem com o código da máquina e o mesmo `Dockerfile` do CI.
  - Na VM, `docker compose pull && docker compose up -d` roda exatamente a imagem publicada.
  - Sem `--build`, o compose usa a imagem que já estiver na máquina e só puxa do GHCR se não houver nenhuma. Por isso, o README manda usar `--build` no desenvolvimento.
- **`env_file: .env` na API.** O `.env` usa os mesmos nomes que o `application.properties` lê (§4), então a API recebe o arquivo inteiro e o compose só sobrescreve a URL do banco, que dentro da rede aponta para `mysql`. Quem garante as variáveis obrigatórias é o `:?` do MySQL e do web e a falta de padrão no Spring (RN-02): sem uma delas, o container para com a mensagem que cita a variável.
- **Seis variáveis no `.env`:** `MYSQL_ROOT_PASSWORD`, `MYSQL_APP_PASSWORD`, `JWT_SECRET`, `SENDGRID_API_KEY`, `SENDGRID_FROM` e `SITE_ADDRESS`. `APP_FRONTEND_URL` é opcional e sai de `SITE_ADDRESS` quando não está definida (§4).
- **`caddy_data` é volume** porque guarda os certificados. Sem ele, cada restart pediria um certificado novo e esbarraria no limite do Let's Encrypt.
- **`.env.example`** na raiz lista as seis variáveis, com comentários. Para desenvolvimento, sugere `root`, `supply_app` e `SITE_ADDRESS=localhost`. Para produção, mostra como gerar cada segredo (`openssl rand -hex 32` para o JWT e `openssl rand -hex 24` para as senhas). O `.gitignore` passa a ignorar `.env`. O arquivo é lido pelo compose e pelo Spring (`.env[.properties]`), então ele não pode ter `export`, aspas nem espaços em volta do `=`.
- **O volume local já existe.** O projeto do compose continua o mesmo, então o `mysql_data` de hoje é reaproveitado com as senhas com que foi criado (`root` e `supply_app`). O `.env` local precisa usar essas senhas. Para trocar, é preciso recriar o volume com `docker compose down -v`.
- **Enquanto a imagem do web não existir** (Web Task 2), `docker compose up -d mysql` sobe só o banco, e a API roda pelo `spring-boot:run`, lendo o mesmo `.env`.

## 6. Publicação da imagem (CI)

Um job `publish` novo no `ci.yml`:

```yaml
  publish:
    needs: build-and-test
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      - uses: docker/setup-buildx-action@v3
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: |
            ghcr.io/${{ github.repository }}:latest
            ghcr.io/${{ github.repository }}:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

- O `build-and-test` atual não muda. Em PR, o `publish` não roda.
- **Tag `sha`** para poder voltar a uma versão anterior (`image: ...:<sha>` no compose) sem rebuild.
- **Só `linux/amd64`**, que é a arquitetura das VMs em vista. Se um dia for para ARM (Oracle Ampere, AWS Graviton), basta acrescentar `platforms: linux/amd64,linux/arm64`.
- **Visibilidade:** o pacote do GHCR nasce privado. Depois da primeira publicação, ele precisa ser marcado como público em *Package settings*. Se preferirem mantê-lo privado, a VM faz `docker login ghcr.io` com um token `read:packages`.

## 7. Operação na VM (qualquer provedor)

Isso fica documentado numa seção **Deploy** no README:

1. VM Ubuntu com Docker. Firewall liberando só 22, 80 e 443.
2. DNS: registro `A` do domínio apontando para o IP da VM, **antes** de subir o web, para o Caddy conseguir emitir o certificado.
3. `git clone` da API na VM. Do repositório, o compose usa só o `docker-compose.yml` e o `docker/mysql/init`.
4. `cp .env.example .env` e preencher com os segredos de produção e o domínio em `SITE_ADDRESS`.
5. `docker compose pull && docker compose up -d`. Na VM, não use `--build`: ela roda a imagem publicada.
6. Para atualizar: `git pull` (caso o compose ou o init tenham mudado) e repetir o passo 5.

## 8. Riscos

| Risco | Mitigação |
|---|---|
| Senha com aspas ou caracteres especiais quebra o SQL do init | Comentário no script e no `.env.example`: só letras e números (`openssl rand -hex`). |
| Primeiro boot lento: o Flyway roda V1 a V12 do zero | É uma vez só. O `restart: unless-stopped` cobre uma falha de conexão enquanto o MySQL termina o init. |
| Contas de demonstração públicas alteram dados | Aceito por enquanto (RN-09). A sandbox fica para uma feature própria. |
| Perder o volume `mysql_data` apaga os dados | Backup fica fora do escopo (spec §6). Até lá, um `mysqldump` manual antes de mexer na VM. |
| `JWT_SECRET` sem padrão quebra o `spring-boot:run` de quem não tiver `.env` | A mensagem de erro do Spring cita a propriedade, e o README manda copiar o `.env.example`. |
| `.env` local com senhas diferentes das do volume que já existe | O `.env.example` sugere `root` e `supply_app`, as senhas com que o volume foi criado. Para trocar, é preciso recriar o volume com `docker compose down -v`. |
| `.env` local com outra senha muda a conexão dos testes | O `TestcontainersConfiguration` fixa `spring.datasource.password` (§4). |
| Rodar localmente uma imagem velha, sem `--build` | O README manda usar `up -d --build` no desenvolvimento. |

## 9. Testes

- `./mvnw verify` verde. O Testcontainers passa a exercitar o `01-users.sh` com `MYSQL_APP_PASSWORD`.
- **Teste de fumaça local**, antes do primeiro deploy:
  - Num volume novo, com um `.env` de senhas geradas e `SITE_ADDRESS=localhost`, rodar `docker compose up -d --build`.
  - O login funciona em `https://localhost` com um usuário seed (o Caddy usa uma CA local para `localhost`), e o Swagger abre em `https://localhost/swagger-ui.html`.
- `docker compose exec mysql mysql -usupply_app -p<senha>` entra com a senha do `.env`, e não com `supply_app`.
- `docker compose ps` mostra a 3306 só em `127.0.0.1` e não mostra a 8080.
- `./mvnw spring-boot:run` sobe sem nenhum `export`, só com o `.env`.
- `./mvnw verify` passa com um `.env` local de senhas diferentes de `supply_app`.
