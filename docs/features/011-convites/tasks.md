# Tarefas — Convite de usuários por email

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** em andamento.

As tasks são sequenciais, e cada uma deixa o `./mvnw verify` verde. Cada task mexe em até 15 arquivos. Specs contam, e apagar ou mover conta como um arquivo. A lista de arquivos é uma estimativa. As tasks do frontend (`supply-chain-verde-web/docs/features/010-convites/tasks.md`) dependem destas, conforme indicado nelas.

---

## Task 1 — `feat/invite-schema`

Só banco e persistência. Nenhuma rota nova.

- [x] `V12__create_invite.sql`: tabela com `accepted` e `expires_at` gerado (`created_at + 24h`), triggers de auditoria sem `token_hash` e grants (plan §1)
- [x] `Invite` (com `accept`) e `InviteRepository` no domínio (plan §2)
- [x] `InviteJpaEntity`, `InviteJpaMapper`, `InviteJpaRepository` e `InviteRepositoryImpl`
- [x] `InviteSchemaIT`: `expires_at` gerado, unique de `token_hash` e auditoria

**Arquivos (~8):** V12, `Invite`, `InviteRepository`, `InviteJpaEntity`, `InviteJpaMapper`, `InviteJpaRepository`, `InviteRepositoryImpl`, `InviteSchemaIT`.

**Pronto:** o banco migra do zero e a partir da V11. O `ddl-auto=validate` aceita a entidade.

## Task 2 — `feat/invite-send`

Depende da Task 1.

- [x] `InviteToken` (plan §4) + `InviteTokenTest`
- [x] `EmailSender`, `SendGridEmailSender` via `RestClient`, `EmailDeliveryException` e o handler 502 (plan §5)
- [x] `InviteEmail` (assunto + HTML em text block)
- [x] `SendInviteRequestDTO`, `SendInviteUseCase` + teste unitário (plan §3)
- [x] `InviteController` com `POST /invites` e a regra no `SecurityConfig`
- [x] Propriedades `sendgrid.*` e `app.frontend-url` obrigatórias. Valores de teste na configuração de testes

**Arquivos (~14):** `InviteToken` + teste, `EmailSender`, `SendGridEmailSender`, `EmailDeliveryException`, `GlobalExceptionHandler`, `InviteEmail`, `SendInviteRequestDTO`, `SendInviteUseCase` + teste, `InviteController`, `SecurityConfig`, `application.properties`, `TestcontainersConfiguration`.

**Pronto:** um `ADMIN` convida e o `EmailSender` (mockado nos testes) recebe o link `<frontend-url>/convites/<token>`. O `POST /users` ainda existe.

## Task 3 — `feat/invite-accept`

Depende da Task 2. Libera a Task 2 do frontend.

- [x] `InviteDetailsResponseDTO` e `GetInviteUseCase` (`GET /invites/{token}`)
- [x] `AcceptInviteRequestDTO` e `AcceptInviteUseCase` (`POST /invites/accept`), devolvendo `LoginResponseDTO`
- [x] `permitAll` das duas rotas no `SecurityConfig`
- [x] `AcceptInviteUseCaseTest` e `InviteFlowIT` com todos os casos do plan §8

**Arquivos (~9):** 2 DTOs, 2 use cases, `AcceptInviteUseCaseTest`, `InviteController`, `SecurityConfig`, `InviteFlowIT`, `JdbcInserts` (se precisar de convite vencido).

**Pronto:** o fluxo completo funciona ponta a ponta nos testes.

## Task 4 — `refactor/remove-user-create`

Depende da Task 3. Libera a Task 1 do frontend.

- [x] Apagar `POST /users`, `RegisterUserUseCase`, `UserRequestDTO` e `RegisterUserUseCaseTest`
- [x] Remover a regra de `POST /api/v1/users` no `SecurityConfig`
- [x] `UserControllerTest` e `ApplicationUseCasesTest` sem os casos de criação
- [x] `UserPlanLimitIT` e `TenantIsolationIT` criando usuários por convite ou por `JdbcInserts`

**Arquivos (~9):** `UserController`, `RegisterUserUseCase`, `UserRequestDTO`, `RegisterUserUseCaseTest`, `SecurityConfig`, `UserControllerTest`, `ApplicationUseCasesTest`, `UserPlanLimitIT`, `TenantIsolationIT`.

**Pronto:** `grep -r RegisterUserUseCase src` não acha nada, e o `./mvnw verify` está verde.

## Task 5 — `docs/invite-reference`

- [ ] `docs/features/000-base/reference.md`: rotas de convite e remoção de `POST /users`
- [ ] README: variáveis `SENDGRID_API_KEY`, `SENDGRID_FROM` e `APP_FRONTEND_URL`, e como verificar o remetente no SendGrid
- [ ] ADR 0004 curto: token opaco com hash SHA-256 em vez de id ou JWT no link

**Arquivos (~3):** `reference.md`, `README.md`, ADR 0004.

**Pronto:** a documentação bate com a API.
