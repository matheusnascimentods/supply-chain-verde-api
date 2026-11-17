# Plano técnico — Deploy em VM com Docker

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Visão geral

```
                  VM Linux (qualquer provedor) — só 80/443 abertas
 internet ──► web (Caddy) ──┬── /api/*, /swagger-ui*, /v3/api-docs* ──► api:8080 ──► mysql:3306
              HTTPS auto    └── qualquer outra rota ──► Angular (/srv)
```

São três containers num compose só, na mesma rede interna. O Caddy fica na imagem do web (`supply-chain-verde-web/docs/features/011-deploy/plan.md`) e emite o certificado Let's Encrypt sozinho, desde que o DNS do domínio aponte para a VM.

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
- **Desenvolvimento:** `docker-compose.yml` ganha `MYSQL_APP_PASSWORD: supply_app`, igual ao padrão do `application.properties`.
- **Testes:** `TestcontainersConfiguration` ganha `.withEnv("MYSQL_APP_PASSWORD", "supply_app")`.
- **Volumes que já existem** não mudam, porque o init só roda na criação do volume. Ninguém precisa recriar o banco local.

## 4. Configuração (`application.properties`)

| Propriedade | Hoje | Depois |
|---|---|---|
| `jwt.secret` | `${JWT_SECRET:404E63...}` | `${JWT_SECRET}`, sem padrão (RN-02) |
| `server.forward-headers-strategy` | — | `framework` (RN-08) |

- **Testes:** `src/test/resources/config/application.properties` ganha um `jwt.secret` de 256 bits só para os testes, ao lado dos valores do SendGrid que já estão lá.
- **Desenvolvimento local:** o passo 4 do Quick Start no README passa a exportar `JWT_SECRET` junto com as variáveis do SendGrid. O README sugere `openssl rand -hex 32`.
- `framework` usa o `ForwardedHeaderFilter` do Spring. Só o Caddy alcança a API, porque ela não publica porta (RN-05), então confiar nos `X-Forwarded-*` é seguro.
- O Flyway (`root`/`root`) e o datasource (`supply_app`) mantêm os padrões de desenvolvimento (RN-04).

## 5. Compose de produção (`docker-compose.prod.yml`)

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
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docker/mysql/init:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h localhost -p$$MYSQL_ROOT_PASSWORD"]
      interval: 5s
      retries: 20

  api:
    image: ghcr.io/matheusnascimentods/supply-chain-verde-api:latest
    restart: unless-stopped
    depends_on:
      mysql: { condition: service_healthy }
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/supply_chain_verde?connectionTimeZone=SERVER
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_APP_PASSWORD:?}
      FLYWAY_PASSWORD: ${MYSQL_ROOT_PASSWORD:?}
      JWT_SECRET: ${JWT_SECRET:?}
      SENDGRID_API_KEY: ${SENDGRID_API_KEY:?}
      SENDGRID_FROM: ${SENDGRID_FROM:?}
      APP_FRONTEND_URL: https://${SITE_ADDRESS:?}
      CORS_ALLOWED_ORIGINS: https://${SITE_ADDRESS:?}

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

- **Um compose separado em vez de um override do de desenvolvimento.** O de dev publica a porta 3306 e usa `root`/`root`, e tirar isso por override (`!reset`) é menos claro que repetir cerca de 10 linhas do MySQL.
- **Seis variáveis no `.env`:** `MYSQL_ROOT_PASSWORD`, `MYSQL_APP_PASSWORD`, `JWT_SECRET`, `SENDGRID_API_KEY`, `SENDGRID_FROM` e `SITE_ADDRESS`. `APP_FRONTEND_URL` e `CORS_ALLOWED_ORIGINS` saem de `SITE_ADDRESS`, então não há como ficarem diferentes. O CORS nem é usado (RN-06), mas fica correto se alguém abrir a API por outra origem.
- **`caddy_data` é volume** porque guarda os certificados. Sem ele, cada restart pediria um certificado novo e esbarraria no limite do Let's Encrypt.
- **`.env.example`** na raiz lista as seis variáveis, com comentários e um exemplo de como gerar cada segredo (`openssl rand -hex 32` para o JWT e `openssl rand -hex 24` para as senhas). Ele só documenta: o `.env` de verdade fica só na VM, e o `.gitignore` passa a ignorar `.env`.

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
3. `git clone` da API na VM. Do repositório, o compose usa só `docker-compose.prod.yml` e `docker/mysql/init`.
4. `cp .env.example .env` e preencher.
5. `docker compose -f docker-compose.prod.yml up -d`.
6. Para atualizar: `docker compose -f docker-compose.prod.yml pull && docker compose -f docker-compose.prod.yml up -d`.

## 8. Riscos

| Risco | Mitigação |
|---|---|
| Senha com aspas ou caracteres especiais quebra o SQL do init | Comentário no script e no `.env.example`: só letras e números (`openssl rand -hex`). |
| Primeiro boot lento: o Flyway roda V1 a V12 do zero | É uma vez só. O `restart: unless-stopped` cobre uma falha de conexão enquanto o MySQL termina o init. |
| Contas de demonstração públicas alteram dados | Aceito por enquanto (RN-09). A sandbox fica para uma feature própria. |
| Perder o volume `mysql_data` apaga os dados | Backup fica fora do escopo (spec §6). Até lá, um `mysqldump` manual antes de mexer na VM. |
| `JWT_SECRET` sem padrão quebra o `spring-boot:run` de quem não exportar a variável | A mensagem de erro do Spring cita a propriedade, e o README mostra o comando. |

## 9. Testes

- `./mvnw verify` verde. O Testcontainers passa a exercitar o `01-users.sh` com `MYSQL_APP_PASSWORD`.
- **Teste de fumaça local do compose de produção**, antes do primeiro deploy: `.env` com `SITE_ADDRESS=localhost`, `docker compose -f docker-compose.prod.yml up -d`, login em `https://localhost` com um usuário seed (o Caddy usa uma CA local para `localhost`) e o Swagger em `https://localhost/swagger-ui.html`.
- `docker compose ... exec mysql mysql -usupply_app -p<senha>` entra com a senha do `.env`, e não com `supply_app`.
