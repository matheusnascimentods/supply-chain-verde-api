# Tarefas — Rate limit

> Requisitos em [`spec.md`](spec.md); arquitetura em [`plan.md`](plan.md).

**Status:** Task 1 concluída; Task 2 pendente.

As tasks são sequenciais, e cada uma deixa o `./mvnw verify` verde. A task do frontend (`supply-chain-verde-web/docs/features/012-rate-limit/tasks.md`) não depende destas para o merge, porque testa com `cy.intercept`. Mas só tem efeito em produção depois da Task 1.

---

## Task 1 — `feat/rate-limit`

- [x] `RateLimitFilter` com janela fixa, chave por usuário ou IP e limites estritos (plan §2)
- [x] Registro no `SecurityConfig` depois do `JwtAuthenticationFilter`, condicionado a `rate-limit.enabled` (plan §3)
- [x] `rate-limit.enabled=true` no `application.properties` e `false` na configuração de testes (plan §5)
- [x] `RateLimitFilterTest` com o relógio controlado (plan §6)
- [x] `RateLimitIT` com `rate-limit.enabled=true` (plan §6)

**Arquivos (~6):** `RateLimitFilter`, `SecurityConfig`, `application.properties`, `src/test/resources/config/application.properties`, `RateLimitFilterTest`, `RateLimitIT`.

**Pronto:** o `./mvnw verify` está verde, e o roteiro manual do plan §6 dá 401 ×5 e depois 429 no compose local.

## Task 2 — `docs/rate-limit`

Depende da Task 1.

- [ ] `docs/features/000-base/reference.md`: o `429` possível em qualquer rota e os limites da RN-04
- [ ] README: os limites e o roteiro de teste manual com `curl` (plan §6)

**Arquivos (~2):** `reference.md`, `README.md`.

**Pronto:** a documentação bate com a API.
