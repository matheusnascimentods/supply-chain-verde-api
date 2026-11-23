# Tarefas — Busca full text

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** em andamento (Tasks 1 e 2 concluídas).

Esta feature vem **antes** da `015-gestao-produtos`, que usa o `FullTextSearch` e a collation daqui. As tasks são sequenciais, cada uma mexe em **no máximo 10 arquivos** (testes contam) e deixa o `./mvnw verify` verde. A task do frontend (`supply-chain-verde-web/docs/features/013-busca-full-text/tasks.md`) é a verificação manual das searchbars depois da Task 3.

---

## Task 1 — `fix/full-text-acento-termo-curto`

- [x] `V13__full_text_sem_acento.sql`: collation `ai_ci` em `product.name/description` e `company.name`, e o `ft_company_cnpj` (plan §2)
- [x] `FullTextSearch.booleanQuery` sem corte de tamanho/stopword e com `_` como parte da palavra, mais o `cnpjQuery` (plan §3)
- [x] `ListProductsUseCase`: termo que vira busca vazia cai no `findAll` (plan §4)
- [x] `FullTextSearchTest` atualizado (plan §7)
- [x] `FullTextSearchIT` com os casos de produto e de fornecedor por nome (plan §7)

**Arquivos (~5):** `V13__full_text_sem_acento.sql`, `FullTextSearch`, `ListProductsUseCase`, `FullTextSearchTest`, `FullTextSearchIT`.

**Pronto:** `./mvnw verify` verde. No compose local, `GET /products?search=graos` e `GET /suppliers?ranked=true&search=c` voltam resultados.

## Task 2 — `fix/full-text-cnpj`

Depende da Task 1 (`ft_company_cnpj` e `cnpjQuery`).

- [x] `CompanyJpaRepository`: nas 4 queries, `cnpj LIKE` vira `MATCH(s.cnpj)` e o `:search = ''` vira a guarda do plan §4
- [x] `CompanyRepositoryImpl`: passa `cnpjQuery(search)` no lugar dos dígitos crus e para de passar o `search` cru
- [x] `FullTextSearchIT`: CNPJ com e sem máscara, e "--" listando tudo
- [x] Ajustar `SupplierRecommendationIT`, se ele contar com o `LIKE` no meio do CNPJ

**Arquivos (~4):** `CompanyJpaRepository`, `CompanyRepositoryImpl`, `FullTextSearchIT`, `SupplierRecommendationIT`.

**Pronto:** `./mvnw verify` verde, e não sobra `LIKE` em `CompanyJpaRepository`.

## Task 3 — `fix/full-text-email`

Depende da Task 1.

- [ ] `V14__full_text_email.sql`: `users.email` em `ai_ci`, mais o `ft_users_email` (plan §5)
- [ ] `UserJpaRepository.findIdsByEmailSearch` (native)
- [ ] `UserRepositoryImpl`: filtro por `user.userId IN :emailUserIds`, e página vazia sem ids
- [ ] `AuditLogRepositoryImpl`: idem na listagem e na contagem
- [ ] `FullTextSearchIT`: usuários e auditoria por `KANE` e por `h.kane@bayern.com`
- [ ] Ajustar os ITs que contam com o email por trecho (`UpdateOwnUserIT`, `UserPlanLimitIT`, `AuditLogTriggerIT`), se algum quebrar

**Arquivos (~8):** `V14__full_text_email.sql`, `UserJpaRepository`, `UserRepositoryImpl`, `AuditLogRepositoryImpl`, `FullTextSearchIT`, até 3 ITs.

**Pronto:** `./mvnw verify` verde, e `grep -rn "LIKE" src/main/java` não acha busca textual.

## Task 4 — `docs/busca-full-text`

Depende das Tasks 1–3.

- [ ] `docs/features/000-base/reference.md`: semântica de busca (RN-01 a RN-06) e o email sem diferença de maiúscula no login
- [ ] README: uma linha sobre a busca por prefixo sem acento

**Arquivos (~2):** `reference.md`, `README.md`.

**Pronto:** a documentação bate com a API.
