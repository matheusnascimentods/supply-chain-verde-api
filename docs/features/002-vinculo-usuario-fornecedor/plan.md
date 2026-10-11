# Plano técnico — Vínculo entre usuário e fornecedor

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Banco de dados

Nova migration `V31__link_supplier_to_user.sql`:

```sql
ALTER TABLE supplier ADD COLUMN user_id BIGINT NULL;

ALTER TABLE supplier
    ADD CONSTRAINT uq_supplier_user UNIQUE (user_id),
    ADD CONSTRAINT fk_supplier_user FOREIGN KEY (user_id) REFERENCES users (user_id);

-- Migra a convenção atual (userId == supplierId).
UPDATE supplier s
SET user_id = u.user_id
FROM users u
WHERE u.role = 'SUPPLIER' AND u.user_id = s.supplier_id;
```

- A FK fica em `supplier` porque o vínculo é opcional do lado do fornecedor (RN-01). O `UNIQUE` garante o 1:1.
- Antes de aplicar, a query abaixo precisa retornar zero linhas. Se retornar, os seeds (`V14`, `V26`) precisam ser ajustados na mesma migration:

```sql
SELECT u.user_id FROM users u
LEFT JOIN supplier s ON s.supplier_id = u.user_id
WHERE u.role = 'SUPPLIER' AND s.supplier_id IS NULL;
```

- RN-02 e RN-03 (role do usuário vinculado) são garantidas na aplicação, não por constraint: uma `CHECK` não enxerga outra tabela. Um trigger seria possível, mas duplicaria a regra.

## 2. Domínio e persistência

| Item | Mudança |
|---|---|
| `Supplier` / `SupplierJpaEntity` | Campo `userId` (nulo). |
| `SupplierRepository` | `findByUserId(Long)`, `existsByUserId(Long)`, filtro `withoutUser` na listagem/ranking. |
| `UserRepository` | Listagem filtrável por role (para o `MANAGER`). |

## 3. Segurança

- `CustomUserPrincipal` ganha `supplierId` (nulo fora de `SUPPLIER`).
- `JwtTokenProvider` grava o claim `supplierId` no login. `AuthenticateUserUseCase` busca o fornecedor por `findByUserId` quando o role é `SUPPLIER`.
- `LoginResponseDTO` e `UserResponseDTO` (`/users/me`) passam a ter `supplierId`.
- Substituir todo uso de `principal.userId()` como fornecedor por `principal.supplierId()`:

| Local | Hoje |
|---|---|
| `BatchController.list` | `effectiveSupplierId = principal.userId()` |
| `ReportController.listAll` e `ensureSupplierOwns` | compara com `principal.userId()` |
| `POST /batches` (`SUPPLIER`) | não força o `supplierId` do token |
| `POST /suppliers/{id}/certifications` (`SUPPLIER`) | não verifica se o `id` é o dele |

- `SecurityConfig` (a ordem importa: regras específicas antes de `/suppliers/**`):

```java
.requestMatchers(HttpMethod.PATCH, "/api/v1/suppliers/me").hasRole("SUPPLIER")
.requestMatchers(HttpMethod.POST, "/api/v1/users").hasAnyRole("ADMIN", "MANAGER")
.requestMatchers(HttpMethod.GET, "/api/v1/users").hasAnyRole("ADMIN", "MANAGER")
```

E o `@PreAuthorize` de `UserController.create`/`list` acompanha a mudança.

## 4. Casos de uso

| Caso de uso | Mudança |
|---|---|
| `RegisterSupplierUseCase` | Se vier `user`, cria o usuário `SUPPLIER` (hash BCrypt) e grava `supplier.user_id`, tudo no mesmo `@Transactional`. |
| `RegisterUserUseCase` | Valida a combinação `role`/`supplierId`; `MANAGER` só cria `SUPPLIER`; fornecedor já vinculado retorna 409; grava o vínculo. |
| `ListUsersUseCase` | Recebe o role do solicitante; `MANAGER` filtra `role = SUPPLIER`. Inclui `supplierId`/`supplierName`. |
| `UpdateUserRoleUseCase` | Recusa (409) mudança de/para `SUPPLIER` (RF-08). |
| `UpdateOwnSupplierUseCase` (novo) | `PATCH /suppliers/me`: resolve o fornecedor por `principal.supplierId()`; aplica só os campos enviados em `supplier`, `address` e `users`; com `newPassword`, confere `currentPassword` via `PasswordHasher` antes de gravar. Um único `@Transactional`. |
| `RankSuppliersBySustainabilityUseCase` | Filtro `withoutUser`. |

## 5. DTOs

| DTO | Mudança |
|---|---|
| `SupplierRequestDTO` | `NewSupplierUserDTO user` opcional (`name`, `email`, `password`). |
| `SupplierResponseDTO` / `SupplierRankingDTO` | `userId`. |
| `UserRequestDTO` | `Long supplierId`. |
| `UserResponseDTO` | `supplierId`, `supplierName`. |
| `UpdateOwnSupplierRequestDTO` (novo) | `name`, `phone`, `address`, `userName`, `currentPassword`, `newPassword`, todos opcionais. |

## 6. Testes

- Repositório (Testcontainers): `UNIQUE` em `user_id`, migração da convenção.
- Casos de uso: criação conjunta com rollback quando o email já existe; `MANAGER` tentando criar `ADMIN` (403); `SUPPLIER` sem `supplierId` (400); fornecedor já vinculado (409); `PATCH /me` com senha atual errada não altera nada.
- Controllers: `SUPPLIER` não lista lotes/relatórios de outro fornecedor; `PATCH /suppliers/me` com `MANAGER` retorna 403.
