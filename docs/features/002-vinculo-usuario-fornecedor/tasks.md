# Tarefas — Vínculo entre usuário e fornecedor

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md). Todas as tasks usam a mesma branch, `feat/vinculo-usuario-fornecedor`, com um PR para `main` por task, em sequência: o PR da task seguinte só abre depois do merge do anterior, a partir da mesma branch atualizada com `main`.

**Status:** não iniciado. Esta é a primeira feature a implementar: o gráfico de emissões depende do `supplierId` no token. Ordem sugerida das demais: [`views-procedures`](../003-views-procedures/tasks.md) → [`recomendacao-fornecedores`](../001-recomendacao-fornecedores/tasks.md), [`grafico-emissoes`](../004-grafico-emissoes/tasks.md) e [`relatorio-pdf`](../005-relatorio-pdf/tasks.md).

---

## Task 1 — PR `feat: vincula fornecedor a usuário`

- [x] Migration `V32__link_supplier_to_user.sql` (coluna, `UNIQUE`, FK, migração da convenção)
- [x] Rodar a query de verificação do plan e ajustar seeds se necessário
- [x] `userId` em `Supplier`, `SupplierJpaEntity`, mapper e DTOs de resposta
- [x] `findByUserId`/`existsByUserId` no repositório

## Task 2 — PR `feat: expõe supplierId no token`

- [x] Claim `supplierId` no JWT e em `CustomUserPrincipal`
- [x] `supplierId` em `LoginResponseDTO` e `GET /users/me`
- [x] Trocar `principal.userId()` por `principal.supplierId()` em `BatchController` e `ReportController`
- [ ] Forçar `supplierId` do token em `POST /batches` e checar dono em `POST /suppliers/{id}/certifications` para `SUPPLIER`

## Task 3 — PR `feat: cria fornecedor com usuário`

- [ ] Bloco `user` opcional em `POST /suppliers`, transacional
- [ ] `supplierId` em `POST /users` com as validações de role
- [ ] Liberar `POST /users` e `GET /users` para `MANAGER` (só `SUPPLIER`)
- [ ] `withoutUser=true` em `GET /suppliers`
- [ ] Bloquear mudança de/para `SUPPLIER` em `PATCH /users/{id}/role`

## Task 4 — PR `fix: permite ao fornecedor editar o próprio perfil`

- [ ] `UpdateOwnSupplierRequestDTO` e `UpdateOwnSupplierUseCase`
- [ ] `PATCH /suppliers/me` no `SupplierController` + regra no `SecurityConfig`
- [ ] Validação de senha atual
- [ ] Swagger dos endpoints alterados

**Pronto:** `grep -rn "principal.userId()" src/main` só aparece em `UserController` (`/users/me`).
