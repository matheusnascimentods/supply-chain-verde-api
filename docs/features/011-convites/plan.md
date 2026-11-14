# Plano técnico — Convite de usuários por email

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Banco (V12)

`V12__create_invite.sql`. A tabela fica no singular, como `company`, `plan` e `batch`. Os campos `invitedBy` e `invitedFor` do pedido viram `invited_by` e `invited_for` (padrão da feature 005).

```sql
CREATE TABLE invite (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    invited_by  BIGINT NOT NULL,
    invited_for VARCHAR(180) NOT NULL,
    role        ENUM('ADMIN', 'MANAGER', 'AUDITOR', 'SUPPLIER') COLLATE utf8mb4_0900_as_ci NOT NULL,
    token_hash  CHAR(64) NOT NULL,
    accepted    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    -- RN-06: 24h por construção; ninguém grava expires_at.
    expires_at  DATETIME(6) GENERATED ALWAYS AS (created_at + INTERVAL 24 HOUR) STORED,
    CONSTRAINT uq_invite_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_invite_invited_by FOREIGN KEY (invited_by) REFERENCES users (id),
    INDEX idx_invite_invited_for (invited_for, accepted, expires_at),
    INDEX idx_invite_invited_by (invited_by, accepted, expires_at)
);
```

- **Sem status.** Se a linha existe, o convite foi enviado: quando o envio falha, a transação desfaz e nada é gravado (RN-11). Expirado é `expires_at <= agora`. Aceito é `accepted = TRUE`, e o aceite marca isso na mesma transação que cria o usuário. Por isso não há procedure nem event.
- **24h garantidas pelo banco:** `expires_at` é uma coluna gerada a partir de `created_at`, que tem o default `CURRENT_TIMESTAMP(6)` como as outras tabelas. A API não calcula nem grava o prazo, então não há como os dois valores divergirem. Na `InviteJpaEntity`, `expires_at` (e também `created_at`) fica `insertable = false, updatable = false`. O valor só é lido nas consultas por token, que voltam do banco já com ele.
- **"Ativo" é sempre calculado pelo MySQL** (`accepted = FALSE AND expires_at > NOW(6)`), dentro das consultas do repositório, e nunca com o relógio da JVM. O prazo e a comparação usam o mesmo relógio.
- **Companhia do convite:** é a de quem convidou, via `invite.invited_by → users.company_id`. `users.company_id` não muda depois do cadastro, então `invite` não precisa de coluna própria. A contagem da RN-05 faz join com `users`.
- **Um convite ativo por email (RN-04):** o banco não garante, porque "ativo" depende do horário, e o MySQL não aceita `NOW()` em coluna gerada nem em `CHECK`. A coluna gerada não muda isso. O use case confere sob o lock da companhia. Resta uma corrida possível: duas companhias convidando o mesmo email no mesmo instante. Ela é inofensiva, porque só um dos aceites passa pelo unique de `users.email`.
- **Auditoria e grants:** triggers `audit_invite_{insert,update,delete}` e grants (`SELECT, INSERT, UPDATE` para `${app_user}`, `SELECT, TRIGGER` para `supply_audit`), seguindo `.github/instructions/migrations.instructions.md`. O `token_hash` fica **fora** do JSON auditado.

## 2. Domínio e persistência

Segue o padrão de `User`/`Company`: domínio puro, entidade JPA, mapper MapStruct, repositório Spring Data e `*RepositoryImpl`.

- `domain/entity/Invite` com `Invite.accept()`.
- `domain/repository/InviteRepository`. "Ativo" é sempre `accepted = FALSE AND expires_at > NOW(6)`, escrito na query.
  - `save`
  - `findActiveByTokenHash(hash)`
  - `countActiveByCompanyId(companyId)`: `JOIN users u ON u.id = i.invited_by WHERE u.company_id = ?`
  - `existsActiveByEmail(email)`
- `UserRepository.countByCompanyId` já existe e é reaproveitado.

## 3. Casos de uso

### `SendInviteUseCase.execute(SendInviteRequestDTO, adminUserId, companyId)`

1. Sem `companyId` (`PLATFORM_ADMIN`): `AccessDeniedException`. Email vazio ou perfil nulo: `ValidationException`.
2. Perfil não permitido para o tipo da companhia: `ValidationException`. Reusa `UserRole.allowedFor`.
3. `userRepository.existsByEmail`: 409.
4. `companyRepository.lockById(companyId)`, depois `inviteRepository.existsActiveByEmail`: 409 se houver.
5. `countByCompanyId + countActiveByCompanyId >= maxUsers`: 409. É a mesma mensagem do limite de hoje.
6. Gera o token, grava (o banco preenche `created_at`, e com ele `expires_at`) e chama `EmailSender.send`. Tudo isso fica na mesma `@Transactional`: se o envio lança `EmailDeliveryException`, a transação desfaz e a resposta é 502.

O envio acontece dentro da transação, com a linha da companhia travada. Se o SendGrid ficar lento, os outros convites e aceites da mesma companhia esperam. Isso é aceitável porque o volume é baixo. Se virar problema, o caminho é mover o envio para um `@TransactionalEventListener(AFTER_COMMIT)`. Mas aí a linha passa a existir antes do email sair, e seria preciso voltar a ter um status de envio.

### `GetInviteUseCase.execute(token)`

Faz o hash do token e chama `findActiveByTokenHash`. Se vier vazio (não existe, expirou ou já foi aceito), devolve `ResourceNotFoundException("Convite inválido ou expirado.")`. Se não, devolve `InviteDetailsResponseDTO(email, role, companyName, expiresAt)`.

### `AcceptInviteUseCase.execute(AcceptInviteRequestDTO)`

1. Nome ou senha vazios: 400, com a mesma validação de não-vazio do `RegisterCompanyUseCase`.
2. Busca pelo hash do token. Inativo ou inexistente: 404 (RN-08).
3. `companyRepository.lockById`. Se `countByCompanyId >= maxUsers`, 409.
4. Cria o `User` com o `invited_for` e o `role` do convite, na `company_id` de quem convidou, e com `passwordHasher.hash`. Marca `accepted = TRUE` com `invite.accept()`.
5. Devolve `authenticate.issueToken(user)`, como o `RegisterCompanyUseCase` já faz.

Dois aceites simultâneos do mesmo token: o primeiro grava `accepted = TRUE`, e o segundo cai no `existsByEmail`/unique de `users.email` e falha. Para garantir isso, o use case chama `findActiveByTokenHash` de novo depois do lock.

## 4. Token

Fica em `application/usecase/invite/InviteToken` e só usa a biblioteca padrão:

```java
static String generate() // 32 bytes de SecureRandom em Base64.getUrlEncoder().withoutPadding(): 43 caracteres
static String hash(String token) // MessageDigest SHA-256 em HexFormat.of().formatHex: 64 caracteres
```

São 256 bits aleatórios, o que torna força bruta inviável, e o banco só guarda o hash. Assim, um vazamento do banco não expõe links válidos. É a recomendação do OWASP Forgot Password Cheat Sheet para tokens enviados por email. O id do convite nunca aparece no link.

## 5. Email (Twilio SendGrid)

- `domain/service/EmailSender` é uma interface (`send(String to, String subject, String html)`), no mesmo padrão de `PasswordHasher`/`BcryptPasswordHasher`.
- `infrastructure/email/SendGridEmailSender` faz `POST https://api.sendgrid.com/v3/mail/send` com o `RestClient` do Spring, que já está no classpath via `spring-boot-starter-webmvc`. Não entra o SDK `sendgrid-java`: o Spring Boot 4 não tem autoconfiguração do SendGrid, e uma chamada só não justifica uma dependência nova. Qualquer resposta diferente de 2xx, ou erro de I/O, vira `EmailDeliveryException`.
- O conteúdo do email fica no código: assunto e HTML curto num text block, em `InviteEmail.of(companyName, role, link)`. Assim é revisável em PR e não depende de template no painel.
- Propriedades novas, todas **obrigatórias e sem default** (a API não sobe sem elas):

  ```properties
  sendgrid.api-key=${SENDGRID_API_KEY}
  sendgrid.from=${SENDGRID_FROM}          # remetente verificado no SendGrid
  app.frontend-url=${APP_FRONTEND_URL}    # ex.: http://localhost:4200
  ```

- O `GlobalExceptionHandler` ganha um handler: `EmailDeliveryException` → 502 "Não foi possível enviar o convite. Tente novamente."

## 6. Web

- `InviteController` (`/api/v1/invites`):
  - `POST /` com `@PreAuthorize("hasRole('ADMIN')")`
  - `GET /{token}`
  - `POST /accept`
- `SecurityConfig`:
  - `permitAll` para `GET /api/v1/invites/*` e `POST /api/v1/invites/accept`
  - `hasRole("ADMIN")` para `POST /api/v1/invites`
  - remove a linha de `POST /api/v1/users`

## 7. Remoção (dead code)

Saem:

- `UserController.create`
- `RegisterUserUseCase`
- `UserRequestDTO`
- `RegisterUserUseCaseTest`
- os casos de criação em `UserControllerTest` e `ApplicationUseCasesTest`

As regras de perfil por tipo e de limite do plano continuam testadas pelos testes do convite. `UserPlanLimitIT` e `TenantIsolationIT` passam a criar usuários pelo fluxo de convite (ou por `JdbcInserts`, quando o convite não é o assunto do teste).

## 8. Testes

- **Unitários:**
  - `InviteTokenTest`: tamanho, alfabeto URL-safe e hash determinístico.
  - `SendInviteUseCaseTest` e `AcceptInviteUseCaseTest`, com repositórios e `EmailSender` mockados.
- **ITs (Testcontainers):** `@MockitoBean EmailSender`. O link é capturado do HTML com `ArgumentCaptor`.
  - `InviteSchemaIT`: `expires_at` sai igual a `created_at + 24h` e um `INSERT` que informa `expires_at` é recusado, unique de `token_hash` e trigger de auditoria sem `token_hash`.
  - `InviteFlowIT`:
    - enviar, consultar, aceitar e fazer login
    - reuso do token: 404
    - `expires_at` no passado: 404
    - convite ativo duplicado: 409
    - plano cheio com convites: 409
    - falha do sender: 502 sem linha gravada
- **Chave nos testes:** `TestcontainersConfiguration` (ou `@TestPropertySource` comum) define `sendgrid.api-key=test`, `sendgrid.from=test@local` e `app.frontend-url=http://localhost:4200`. Nenhum teste chama o SendGrid real.
