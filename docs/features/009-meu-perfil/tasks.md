# Tarefas — Atualizar o próprio usuário (`PATCH /users/me`)

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** concluído.

Desvios do plano:

- `UpdateOwnUserUseCase.execute` não devolve DTO: o controller grava e responde chamando o próprio `me()`, então os dois endpoints não divergem sem precisar de um método a mais.
- `OwnAccountChanges.apply` devolve se algo mudou, no lugar de um `hasChanges` separado.
- `SecurityConfig` liberava `PATCH /users/**` só para `ADMIN`; `PATCH /users/me` ganhou regra própria para qualquer autenticado. Libera a Task 2 do frontend (`supply-chain-verde-web/docs/features/008-meu-perfil/tasks.md`).

---

## Task 1 — `feat/update-own-user`

- [x] `OwnAccountChanges` extraído de `UpdateOwnSupplierUseCase` (plan §1)
- [x] `UpdateOwnSupplierUseCase` usando `OwnAccountChanges`, sem mudar comportamento
- [x] `UpdateOwnUserRequestDTO`, `UpdateOwnUserUseCase` e `PATCH /users/me` (plan §2)
- [x] Testes do plan §3
- [x] Rota nova em `docs/features/000-base/reference.md`

**Pronto:** `./mvnw verify` verde; o admin troca a própria senha e consegue logar com ela.
