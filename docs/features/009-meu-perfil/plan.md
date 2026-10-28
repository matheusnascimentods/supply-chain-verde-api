# Plano técnico — Atualizar o próprio usuário (`PATCH /users/me`)

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Regras de nome e senha num lugar só

Hoje as regras de nome e senha estão dentro de `UpdateOwnSupplierUseCase` (validação nas linhas 40–45 e gravação nas linhas 57–61). Elas saem para um componente de aplicação reutilizado pelos dois endpoints:

| Peça | Responsabilidade |
|---|---|
| `application/usecase/user/OwnAccountChanges` | `validate(User, userName, currentPassword, newPassword)`: as mesmas 3 validações de hoje. `apply(User, userName, newPassword)`: aplica nome e hash da senha. `hasChanges(userName, newPassword)`. Não grava nada. |
| `UpdateOwnSupplierUseCase` | Chama `validate` junto com as validações do fornecedor, **antes** de gravar qualquer coisa, como hoje. Depois chama `apply` + `userRepository.save` no lugar do bloco atual. O comportamento não muda. |
| `UpdateOwnUserUseCase` (novo) | `@Transactional execute(userId, UpdateOwnUserRequestDTO)`: busca o usuário, chama `validate`, `apply` e `save`, e devolve o mesmo `UserResponseDTO` do `GET /users/me`. |

`validate` e `apply` ficam separados porque o fornecedor precisa validar tudo (empresa, endereço e usuário) antes da primeira gravação.

## 2. Contrato

```java
public record UpdateOwnUserRequestDTO(String userName, String currentPassword, String newPassword) {}
```

```java
@PatchMapping("/me")
@PreAuthorize("isAuthenticated()")
public UserResponseDTO updateMe(@RequestBody UpdateOwnUserRequestDTO request,
                                @AuthenticationPrincipal CustomUserPrincipal principal)
```

- A montagem do `UserResponseDTO`, que hoje fica inline no `me()` do `UserController`, vira um método privado usado pelos dois endpoints, para os dois não divergirem.
- Auditoria: o `save` passa pelo `UserRepositoryImpl.save`, que já está no pointcut do `AuditActorContextAspect`. A senha já é excluída do snapshot. Nada muda no banco.
- Swagger: `@Operation` no mesmo tom do `PATCH /suppliers/me`.

## 3. Testes

- `UpdateOwnUserUseCaseTest`: só nome; troca de senha com a senha atual certa; senha atual errada ou ausente → `ValidationException` sem `save`; nome vazio → `ValidationException`.
- `UpdateOwnSupplierUseCaseTest` continua verde sem mudar nenhum caso, porque o comportamento é o mesmo.
- IT: admin faz `PATCH /users/me`, o `GET /users/me` reflete o novo nome e o login funciona com a nova senha; `audit_log` tem o `UPDATE` em `users` com o ator igual ao próprio usuário e sem `password`.
