# Tarefas — Atualizar o próprio usuário (`PATCH /users/me`)

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** planejado, nada implementado. Libera a Task 2 do frontend (`supply-chain-verde-web/docs/features/008-meu-perfil/tasks.md`).

---

## Task 1 — `feat/update-own-user`

- [ ] `OwnAccountChanges` extraído de `UpdateOwnSupplierUseCase` (plan §1)
- [ ] `UpdateOwnSupplierUseCase` usando `OwnAccountChanges`, sem mudar comportamento
- [ ] `UpdateOwnUserRequestDTO`, `UpdateOwnUserUseCase` e `PATCH /users/me` (plan §2)
- [ ] Testes do plan §3
- [ ] Rota nova em `docs/features/000-base/reference.md`

**Pronto:** `./mvnw verify` verde; o admin troca a própria senha e consegue logar com ela.
