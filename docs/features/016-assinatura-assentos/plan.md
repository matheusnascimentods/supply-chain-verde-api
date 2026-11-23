# Plano técnico — Assinatura única e ativação de usuários

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Banco: o que sai, o que muda, o que entra

| Objeto | Hoje | Depois |
|---|---|---|
| `plan` | catálogo `BASIC`/`PRO`/`PRO_MAX` (V7) | **sai** (Task 4) |
| `company_plan` | assinatura com `started_at`/`ended_at`, unique da ativa (V7) | **sai** (Task 4) |
| triggers `audit_company_plan_*` | V9 | **saem** com a tabela (Task 4) |
| grants de `plan`/`company_plan` | V9 (`supply_audit` e app) | `REVOKE` antes do `DROP`: o MySQL não apaga o privilégio de tabela junto com ela (Task 4) |
| `company` | sem teto | **ganha** `max_users INT NOT NULL DEFAULT 10` + `CHECK (max_users > 0)` (Task 1) |
| trigger `audit_company_update`/`_insert`/`_delete` | V9 | recriados com `max_users` (Task 1) |
| `users` | sem situação | **ganha** `is_active BOOLEAN NOT NULL DEFAULT TRUE` e `INDEX idx_users_company_active (company_id, is_active)` (Task 2) |
| triggers `audit_users_*` | V9, colunas explícitas | recriados com `is_active` (Task 2) |
| `invite` | sem mudança | sem mudança: continua contando assento enquanto ativo |
| `audit_log` com `table_name = 'company_plan'` | histórico | **fica**. É histórico, e o rótulo do front continua |

Migrations:

- `V16__company_max_users.sql`: `ALTER TABLE company ADD max_users ...`, mais o `DROP`/`CREATE` dos 3 triggers de `company`, no padrão da V9. Todas as companhias ficam com 10. As que já passam de 10 ativos ficam acima, sem corte (RN-03). No compose local nenhuma passa: os 3 mercados `PRO` estão em 10/10.
- `V17__users_is_active.sql`: coluna, índice e os 3 triggers de `users`.
- `V18__drop_plan.sql`: `REVOKE`s, `DROP TRIGGER audit_company_plan_*`, `DROP TABLE company_plan`, `DROP TABLE plan`.

## 2. Teto (Task 1)

- `Company` e `CompanyJpaEntity` ganham `maxUsers`.
- `UserRepository.countActiveByCompanyId(companyId)`, que é o `countByCompanyIdAndIsActiveTrue` do Spring Data. Na Task 1 ele ainda é `countByCompanyId`, e a Task 2 troca para os ativos.
- Uma checagem só, usada por convite, aceite e reativação:

  ```java
  // SeatPolicy (application/usecase/user), chamada sempre depois de companyRepository.lockById(companyId)
  public void requireFreeSeat(Company company) {
      long used = userRepository.countActiveByCompanyId(company.getCompanyId())
              + inviteRepository.countActiveByCompanyId(company.getCompanyId());
      if (used >= company.getMaxUsers()) {
          throw new ConflictException("Sem assentos livres: " + used + " de " + company.getMaxUsers() + " em uso.");
      }
  }
  public long seatsUsed(Long companyId) { ... mesma soma ... }
  ```

  - `SendInviteUseCase`: troca o bloco do `planRepository` por `seatPolicy.requireFreeSeat(company)`. O lock já está lá.
  - `AcceptInviteUseCase`: hoje conta só os usuários, porque o próprio convite já está entre os ativos. Como o convite aceito já ocupa o assento dele, o aceite chama `seatsUsed` e só barra quando `seatsUsed > max`. Isso cobre o caso em que o teto baixou depois do convite. É um `if` no próprio use case, sem parâmetro booleano no `SeatPolicy`.
- `RegisterCompanyUseCase`: saem `planRepository`, `findByCode` e `subscribe`. `RegisterCompanyRequestDTO` perde `planCode`, e o Jackson ignora campo desconhecido (padrão do Spring Boot), então o front antigo não quebra.
- `GetOwnCompanyUseCase`/`CompanyResponseDTO`: sai `plan` e entram `maxUsers` e `seatsUsed`. Como o web ainda lê `plan` até a web Task 2 (perfil), a Task 1 **mantém `plan`** e só acrescenta os dois campos. O `plan` sai na Task 4.

## 3. Ativação (Task 2)

### Domínio e persistência

- `User.active` (boolean) e `UserJpaEntity.active` → `@Column(name = "is_active")`. `UserJpaMapper` copia o campo.
- `User.deactivate()` / `User.activate()` só mudam o campo. As regras ficam no use case, como no `updateRole`.
- `UserRepository`: `countActiveByCompanyIdAndRole(companyId, role)` substitui `countByCompanyIdAndRole` no RN-07, e `isActive(userId)` serve para o filtro (§3.3).

### `SetUserStatusUseCase.execute(userId, active, actorUserId, actorCompanyId)`

Na mesma ordem do `UpdateUserRoleUseCase`:

1. Usuário inexistente devolve 404. Ator de companhia diferente devolve 403 (RN-05). Alvo sem companhia (`PLATFORM_ADMIN`) devolve 400 (RN-08).
2. Se `user.active == active`, devolve como está (RN-10).
3. Se for desativar e `userId == actorUserId`, devolve 409 (RN-06).
4. `companyRepository.lockById(companyId)`.
5. Desativar um `ADMIN` com `countActiveByCompanyIdAndRole(ADMIN) <= 1` devolve 409 (RN-07). Para ativar, `seatPolicy.requireFreeSeat(company)` (RN-09).
6. `save`.

`UserController`:

```java
@PatchMapping("/{userId}/status")
@PreAuthorize("hasRole('ADMIN')")   // igual ao /role, que já deixa o PLATFORM_ADMIN passar
public UserResponseDTO updateStatus(@PathVariable Long userId, @RequestBody UpdateUserStatusRequestDTO request, ...)
```

`UpdateUserStatusRequestDTO(Boolean active)`: `null` devolve 400.

### Corte imediato (RN-11)

- `JwtAuthenticationFilter`: depois de validar o token, só autentica se `userRepository.isActive(userId)`. A query é `SELECT is_active FROM users WHERE id = ?`, pela PK. Um usuário inexistente conta como inativo. Sem autenticação, o Spring devolve 401 como hoje para token inválido.
  - `// ponytail: 1 SELECT por PK a cada request; cache curto (Caffeine, ~30s) se o filtro aparecer no profiling.`
- `AuthenticateUserUseCase.execute`: depois do `passwordHasher.matches`, `if (!user.isActive()) throw new UnauthorizedActionException("Usuário desativado.")`. A senha é conferida antes, para não revelar a situação a quem não sabe a senha.
- `CustomUserDetailsService`: passa `.disabled(!user.isActive())`, para manter coerente quem usa o `UserDetails`.

### Ajustes nas regras existentes

- `UpdateUserRoleUseCase`: alvo inativo devolve 409 (RN-12), checado antes do lock. A contagem do RN-08 passa a ser `countActiveByCompanyIdAndRole`.
- `SendInviteUseCase`: o `existsByEmail` vira `findByEmail`. Se o usuário existir e estiver inativo, a mensagem é "Email pertence a um usuário desativado. Reative-o na lista de usuários." (RN-14). Ativo continua "Email já cadastrado".
- `UserResponseDTO` e `UserDtoMapper` ganham `active`.

## 4. Filtros da listagem (Task 3)

- `UserController.list`: `@RequestParam(required = false) List<UserRole> role` e `Boolean active`.
- `ListUsersUseCase.execute(email, roles, active, companyId, limit, offset)`.
- `UserRepositoryImpl.where`/`bindFilters` ganham as duas condições, no mesmo padrão das que existem:

  ```java
  if (roles != null && !roles.isEmpty()) conditions.add("user.role IN :roles");
  if (active != null) conditions.add("user.active = :active");
  ```

- O índice `idx_users_company_active` da V17 cobre `company_id` + `is_active`. O `role` já tem `idx_users_company_role` (V8).

## 5. Remoção dos planos (Task 4)

Só depois que o web parar de chamar `GET /plans` e de ler `plan` (web Tasks 1 e 2 em produção).

- Apagar: `Plan`, `PlanRepository`, `PlanRepositoryImpl` (e o JPA, se houver), `PlanController`, `ListPlansUseCase`, `PlanResponseDTO`, `CompanyRepository.subscribe` (+ impl), o `permitAll` de `/api/v1/plans` no `SecurityConfig`, e `plan` do `CompanyResponseDTO`.
- `V18__drop_plan.sql` (§1).
- Testes: `UserPlanLimitIT` vira `SeatLimitIT`. `MigrationToCompanyIT` e `CompanyConstraintsIT` perdem as asserções de `company_plan`: a da unique de assinatura ativa sai, porque a regra deixou de existir. `RegisterCompanyUseCaseTest`, `CompanySignupIT`, `SendInviteUseCaseTest` e `AcceptInviteUseCaseTest` perdem o mock/uso de `PlanRepository` (a maior parte já na Task 1).
- Conferência: `grep -rniE "plan(Code|Repository|_id)|company_plan|PlanResponse" src` só pode achar migrations antigas e este doc.

## 6. Testes

`SeatLimitIT` / `UserStatusIT` (Testcontainers):

- 10 ativos: reativar devolve 409. Desativar um e reativar outro devolve 200 (RN-02, RN-09).
- 9 ativos + 1 convite: reativar e convidar devolvem 409. Aceitar esse convite devolve 200 (o próprio convite não conta duas vezes).
- Desativar a si mesmo devolve 409. Desativar o último `ADMIN` ativo devolve 409. Com dois `ADMIN`, desativar um devolve 200 (RN-06, RN-07).
- Outra companhia devolve 403. Alvo `PLATFORM_ADMIN` devolve 400. O `PLATFORM_ADMIN` desativando um usuário de mercado devolve 200, e reativando numa companhia cheia devolve 409 (RN-04, RN-05, RN-08).
- O token emitido antes da desativação recebe 401 em `GET /users/me`, e o login devolve 401 (RN-11).
- `PATCH /role` em inativo devolve 409 (RN-12). Convite para email de inativo devolve 409 com a mensagem da RN-14.
- `GET /users?role=MANAGER&role=AUDITOR&active=false` (RN-15).
- `AuditLogTriggerIT`: a desativação grava `UPDATE` em `users` com `is_active` em `before`/`after`.

Teste de concorrência: dois `PATCH /status {active:true}` em paralelo com 1 assento livre, para conferir que só um passa. Ele reaproveita o padrão de lock já usado em `InviteFlowIT`, se existir lá. Se não, fica um teste com `CountDownLatch` e dois threads.
