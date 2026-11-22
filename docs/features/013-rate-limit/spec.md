# Especificação — Rate limit

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Contraparte no frontend: `supply-chain-verde-web/docs/features/012-rate-limit/`.

## 1. Problema

- A API não limita requisições. Qualquer usuário logado pode copiar o `curl` de uma chamada no DevTools, com o token junto, e rodar em loop. Uma `t3.small` com a JVM e o MySQL na mesma máquina cai rápido com isso.
- As rotas públicas sensíveis não têm proteção:
  - `POST /auth/login` aceita força bruta de senha, e cada tentativa roda um BCrypt, que é caro de propósito.
  - `GET /invites/{token}` e `POST /invites/accept` aceitam tentativas em série de adivinhar o token.
  - `POST /companies` aceita cadastros em massa.

## 2. Objetivo

Toda requisição conta para um limite por cliente, como no ClickUp: 120 por minuto. As rotas públicas sensíveis têm, além disso, um limite próprio e menor por IP. Quem passa de qualquer um dos dois recebe `429 Too Many Requests` e sabe quanto tempo esperar.

## 3. Glossário

| Termo | Significado |
|---|---|
| Cliente | Quem faz a requisição. É o `userId` quando há JWT válido, e o IP quando não há. |
| Janela | Intervalo de 1 minuto. Os contadores zeram quando ele termina. |
| Limite global | Vale para todas as rotas, contado por cliente. |
| Limite estrito | Vale só para uma rota pública sensível, contado por IP mesmo quando há JWT. Conta junto com o limite global. |

## 4. Regras de negócio

| ID | Regra |
|---|---|
| RN-01 | Limite global: 120 requisições por janela por cliente, em todas as rotas que passam pela API. |
| RN-02 | Com JWT válido, o cliente é o `userId`. Assim, colegas no mesmo IP (escritório, CGNAT) não dividem o limite, e quem troca de IP com o mesmo token continua limitado. Sem JWT, ou com JWT inválido, o cliente é o IP. |
| RN-03 | O IP é o do cliente real, lido do `X-Forwarded-For` que o Caddy grava. O Caddy descarta o `X-Forwarded-For` que vem de fora, então o cliente não consegue forjar o IP. A API não publica porta, então só o Caddy chega até ela (RN-05 da feature 012). |
| RN-04 | Limites estritos por IP, além do global: `POST /api/v1/auth/login` 5, `POST /api/v1/invites/accept` 5, `GET /api/v1/invites/{token}` 10 e `POST /api/v1/companies` 3. |
| RN-05 | Janela fixa de 1 minuto, a mesma para todos os clientes. Quando ela vira, todos os contadores zeram. Na virada, um cliente pode passar até o dobro do limite. |
| RN-06 | Toda requisição conta, inclusive as que terminam em 401, 403 ou 404. A preflight `OPTIONS` do CORS é respondida antes e não conta. |
| RN-07 | Passou do limite: `429` com `Retry-After` (segundos até o fim da janela) e corpo `{ "message": "Muitas requisições. Tente novamente em instantes." }`, no mesmo formato dos outros erros. A requisição não chega ao controller. |
| RN-08 | Os contadores ficam na memória da API. Um restart zera todos. O desenho vale para uma instância só, que é o deploy atual (ADR 0005). |

## 5. Contrato

Nenhuma rota nova. Qualquer rota pode responder:

| Status | Headers | Corpo |
|---|---|---|
| `429` | `Retry-After: <segundos>` | `{ "message": "Muitas requisições. Tente novamente em instantes." }` |

## 6. Fora do escopo

- Ataque distribuído vindo de muitos IPs ao mesmo tempo (DDoS). Isso é proteção de infraestrutura (AWS WAF com regra de limite), não da API.
- Limite por email no login, contra força bruta feita de muitos IPs diferentes.
- Limites diferentes por plano da empresa.
- Headers `X-RateLimit-Limit`, `X-RateLimit-Remaining` e `X-RateLimit-Reset`. O front não os usaria.
- Contadores compartilhados entre várias réplicas (Redis). Só faz sentido quando houver mais de uma.

## 7. Critérios de aceite

- A 121ª requisição de um usuário dentro do minuto recebe `429`, e outro usuário no mesmo IP continua sendo atendido.
- A 6ª tentativa de login do mesmo IP dentro do minuto recebe `429`, mesmo com senha certa.
- Depois que a janela vira, o cliente volta a ser atendido.
- Um `X-Forwarded-For` forjado pelo cliente não muda a contagem quando a requisição passa pelo Caddy.
- O `./mvnw verify` passa, e os ITs que não testam o limite não são afetados por ele.
