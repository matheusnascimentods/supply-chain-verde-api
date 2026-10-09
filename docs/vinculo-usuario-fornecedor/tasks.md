# Tarefas — Vínculo entre usuário e fornecedor

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md). Cada task é um branch/PR para `main`, em sequência.

**Status:** não iniciado. Esta é a primeira feature a implementar: o gráfico de emissões depende do `supplierId` no token. Ordem sugerida das demais: [`views-procedures`](../views-procedures/tasks.md) → [`recomendacao-fornecedores`](../recomendacao-fornecedores/tasks.md), [`grafico-emissoes`](../grafico-emissoes/tasks.md) e [`relatorio-pdf`](../relatorio-pdf/tasks.md).

---

## Task 1 — `feat/supplier-user-link`

- [ ] Migration `V31__link_supplier_to_user.sql` (coluna, `UNIQUE`, FK, migração da convenção)
- [ ] Rodar a query de verificação do plan e ajustar seeds se necessário
- [ ] `userId` em `Supplier`, `SupplierJpaEntity`, mapper e DTOs de resposta
- [ ] `findByUserId`/`existsByUserId` no repositório

## Task 2 — `feat/supplier-id-in-token`

- [ ] Claim `supplierId` no JWT e em `CustomUserPrincipal`
- [ ] `supplierId` em `LoginResponseDTO` e `GET /users/me`
- [ ] Trocar `principal.userId()` por `principal.supplierId()` em `BatchController` e `ReportController`
- [ ] Forçar `supplierId` do token em `POST /batches` e checar dono em `POST /suppliers/{id}/certifications` para `SUPPLIER`
- [ ] Atualizar `docs/spec.md` removendo a convenção `userId == supplierId`

## Task 3 — `feat/create-supplier-with-user`

- [ ] Bloco `user` opcional em `POST /suppliers`, transacional
- [ ] `supplierId` em `POST /users` com as validações de role
- [ ] Liberar `POST /users` e `GET /users` para `MANAGER` (só `SUPPLIER`)
- [ ] `withoutUser=true` em `GET /suppliers`
- [ ] Bloquear mudança de/para `SUPPLIER` em `PATCH /users/{id}/role`

## Task 4 — `fix/supplier-update-own-profile`

- [ ] `UpdateOwnSupplierRequestDTO` e `UpdateOwnSupplierUseCase`
- [ ] `PATCH /suppliers/me` no `SupplierController` + regra no `SecurityConfig`
- [ ] Validação de senha atual
- [ ] Swagger dos endpoints alterados

**Pronto:** `grep -rn "principal.userId()" src/main` só aparece em `UserController` (`/users/me`).
