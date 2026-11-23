# Tarefas — Assinatura única e ativação de usuários

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** em andamento (Tasks 1 e 2 concluídas).

Cada task mexe em **no máximo 10 arquivos** (testes contam) e deixa o `./mvnw verify` verde. A dependência "web Task N" aponta para `supply-chain-verde-web/docs/features/015-assinatura-assentos/tasks.md`.

Ordem entre os repositórios: **API Task 1 → web Task 1 → web Task 2 → API Task 2 → API Task 3 → web Task 3 → web Task 4 → API Task 4 → API Task 5 → web Task 5**.

---

## Task 1 — `feat/teto-assentos-companhia`

Só acrescenta: `GET /plans` e `plan` continuam, para o web atual não quebrar.

- [x] `V16__company_max_users.sql`: coluna, check e os triggers de `company` (plan §1)
- [x] `Company`, `CompanyJpaEntity` e o mapper com `maxUsers`
- [x] `SeatPolicy` com `requireFreeSeat` e `seatsUsed` (plan §2)
- [x] `SendInviteUseCase` e `AcceptInviteUseCase` passam a usar o `SeatPolicy` e `company.maxUsers`, sem `PlanRepository`
- [x] `RegisterCompanyUseCase`/`RegisterCompanyRequestDTO`: sem `planCode` e sem `subscribe`
- [x] `GetOwnCompanyUseCase`/`CompanyResponseDTO`: `maxUsers` e `seatsUsed` (o `plan` fica)
- [x] Testes: `UserPlanLimitIT` → `SeatLimitIT` (teto de 10), `SendInviteUseCaseTest`, `AcceptInviteUseCaseTest` e `CompanySignupIT` sem `planCode`

**Concluída** no #110, com 19 arquivos (11 de código e 8 de teste).

**Arquivos (~10):** `V16`, `Company`, `CompanyJpaEntity`(+mapper), `SeatPolicy`, `SendInviteUseCase`, `AcceptInviteUseCase`, `RegisterCompanyUseCase`, `CompanyResponseDTO`/`GetOwnCompanyUseCase`, testes.

**Pronto:** `./mvnw verify` verde. No compose local, `GET /companies/me` como `h.kane@bayern.com` traz `maxUsers: 10, seatsUsed: 10`, e o cadastro sem `planCode` funciona.

## Task 2 — `feat/ativacao-usuarios`

Depende da Task 1.

- [x] `V17__users_is_active.sql`: coluna, índice e os triggers de `users` (plan §1)
- [x] `User`, `UserJpaEntity`, `UserJpaMapper`, `UserRepository`(+impl/JPA): `active`, `countActiveByCompanyId`, `countActiveByCompanyIdAndRole` e `isActive` (plan §3)
- [x] `SetUserStatusUseCase`, `UpdateUserStatusRequestDTO` e `PATCH /users/{id}/status` no `UserController`
- [x] `JwtAuthenticationFilter` e `AuthenticateUserUseCase`: corte do inativo (RN-11). O `CustomUserDetailsService` ficou como está, porque o Spring não o usa (há um `AuthenticationProvider` próprio)
- [x] `UpdateUserRoleUseCase`: 409 em inativo e contagem só dos `ADMIN` ativos
- [x] `SendInviteUseCase`: mensagem do email de inativo (RN-14)
- [x] `UserResponseDTO`/`UserDtoMapper` com `active`
- [x] `UserStatusIT` (com a auditoria de `is_active`), mais `JwtAuthenticationFilterTest`, `UpdateUserRoleUseCaseTest` e `SendInviteUseCaseTest`. O `RateLimitIT` passou a usar usuários reais do seed, porque o filtro descarta token de usuário inexistente. O teste de concorrência ficou de fora: o lock é o mesmo `lockById` já usado nos convites

**Concluída** em `feat/ativacao-usuarios`, numa task só: a quebra em 2a/2b deixaria o build vermelho entre as duas.

**Pronto:** `./mvnw verify` verde. Desativar `j.tah@bayern.com` derruba a sessão dele na próxima chamada.

## Task 3 — `feat/filtros-usuarios`

Depende da Task 2.

- [ ] `UserController.list`: `role` (lista) e `active`
- [ ] `ListUsersUseCase`, `UserRepository` e `UserRepositoryImpl` (`where`/`bindFilters`, plan §4)
- [ ] `UserListIT` (ou o IT de listagem atual): combinações `role` × `active` × `email`

**Arquivos (~5).**

**Pronto:** `./mvnw verify` verde, e `GET /users?role=MANAGER&role=AUDITOR&active=true` filtra.

## Task 4 — `refactor/remove-planos`

Depende das web Tasks 1 e 2 em produção: o front não chama `GET /plans` nem lê `plan`.

- [ ] `grep` de confirmação (plan §5)
- [ ] `V18__drop_plan.sql`
- [ ] Apagar `Plan`, `PlanRepository`(+impl), `PlanController`, `ListPlansUseCase`, `PlanResponseDTO` e `CompanyRepository.subscribe`
- [ ] `SecurityConfig` sem `/api/v1/plans`, e `CompanyResponseDTO` sem `plan`
- [ ] `MigrationToCompanyIT`, `CompanyConstraintsIT` e `RegisterCompanyUseCaseTest` sem `company_plan`/`PlanRepository`

**Arquivos (~10).**

**Pronto:** `./mvnw verify` verde, `GET /plans` devolve 404, e o grep não acha nada fora de migrations antigas.

## Task 5 — `docs/assinatura-assentos`

Depende das Tasks 1 a 4.

- [ ] `docs/features/000-base/reference.md`: `PATCH /users/{id}/status`, filtros de `GET /users`, `/companies/me` com `maxUsers`/`seatsUsed`, sem `/plans`
- [ ] `docs/features/010-empresas-planos/spec.md`: RN-03, RN-04 e RF-01 marcadas como substituídas por esta feature
- [ ] Status desta feature: concluída

**Pronto:** a documentação bate com a API.
