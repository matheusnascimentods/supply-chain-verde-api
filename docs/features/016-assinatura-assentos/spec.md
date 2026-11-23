# Especificação — Assinatura única e ativação de usuários

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Contraparte no frontend: `supply-chain-verde-web/docs/features/015-assinatura-assentos/`. Substitui o catálogo de planos da `010-empresas-planos` (RN-03, RN-04, RF-01).

## 1. Problema

- Existem três planos (`BASIC` 3, `PRO` 10, `PRO_MAX` 15) que só mudam o número de contas. O cadastro obriga a escolher um, e trocar de plano não existe.
- Um usuário não pode ser desativado. Quem sai da empresa continua com acesso e ocupando vaga, e a única saída é apagar a linha no banco.
- A tela de usuários não filtra por perfil nem por situação.

## 2. Objetivo

- Uma assinatura única por companhia, com teto de **10 usuários ativos** (assentos).
- O `ADMIN` da companhia desativa e reativa usuários. Ativar ocupa um assento, desativar libera um.
- A listagem de usuários filtra por perfis e por ativo/inativo.

## 3. Glossário

| Termo | Significado |
|---|---|
| Assento | Vaga da companhia. Ocupam assento os usuários ativos e os convites ativos (pendentes e não expirados). |
| Teto | `company.max_users`, 10 por padrão. Muda só por SQL, sem endpoint. |
| Usuário inativo | `users.is_active = FALSE`. Não loga, não usa a API, não ocupa assento e continua no histórico (lotes, auditoria, convites). |

## 4. Regras de negócio

### Assinatura

| ID | Regra |
|---|---|
| RN-01 | Toda companhia tem `max_users`, 10 por padrão. Os planos, as assinaturas (`plan`, `company_plan`) e `GET /plans` deixam de existir. |
| RN-02 | Ativos + convites ativos de uma companhia nunca passam de `max_users`. Isso vale para convidar, aceitar convite e reativar, e o excesso devolve 409. |
| RN-03 | Uma companhia que já tenha mais ativos que o teto na migração não perde ninguém. Ela só não ativa nem convida até ficar abaixo. |
| RN-04 | O teto vale para todos, inclusive para o `PLATFORM_ADMIN`. Exceção por cliente se faz alterando `company.max_users`. |

### Ativação

| ID | Regra |
|---|---|
| RN-05 | Só o `ADMIN` ativa e desativa, e só na própria companhia (outra companhia devolve 403). O `PLATFORM_ADMIN` ativa e desativa em qualquer companhia. São as mesmas regras da troca de perfil (RN-09 da 010). |
| RN-06 | Ninguém desativa a si mesmo (409). |
| RN-07 | A companhia sempre tem pelo menos um `ADMIN` **ativo**. Desativar o último devolve 409. A RN-08 da 010 passa a contar só os `ADMIN` ativos. |
| RN-08 | Um `PLATFORM_ADMIN` não pode ser desativado por endpoint (400), do mesmo jeito que o perfil dele não muda. |
| RN-09 | Reativar exige um assento livre (RN-02), checado sob lock da companhia. |
| RN-10 | Ativar quem já está ativo, ou desativar quem já está inativo, não muda nada e devolve 200. |
| RN-11 | O acesso de quem é desativado é cortado na próxima requisição: o token emitido antes deixa de valer. O login de um inativo devolve 401. |
| RN-12 | O perfil de um usuário inativo não muda (409). Reative antes. |
| RN-13 | Desativar não tem outro efeito. Convites enviados pelo usuário, lotes, relatórios e auditoria ficam como estão. |
| RN-14 | Convidar um email que pertence a um usuário inativo devolve 409, com a orientação de reativá-lo. |

### Listagem

| ID | Regra |
|---|---|
| RN-15 | `GET /users` aceita `role` repetido (`role=MANAGER&role=AUDITOR`, OU entre eles) e `active` (`true`/`false`). Sem o parâmetro, o filtro não se aplica. Os filtros se combinam com `email` por E. |
| RN-16 | Cada usuário traz `active`. |

## 5. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | `PATCH /users/{id}/status` com `{ "active": boolean }`, só para `ADMIN` e `PLATFORM_ADMIN` (RN-05 a RN-10). A resposta é o `UserResponseDTO`. |
| RF-02 | `GET /users` com `role` (lista) e `active` (RN-15). `UserResponseDTO` ganha `active`. |
| RF-03 | `GET /companies/me`: sai `plan` e entram `maxUsers` e `seatsUsed` (ativos + convites ativos). |
| RF-04 | `POST /companies` deixa de ler `planCode`. Se ele vier, é ignorado. |
| RF-05 | Saem `GET /plans`, `plan`, `company_plan` e o código Java deles. |
| RNF-01 | A situação do usuário entra na auditoria pelos triggers de `users` (`is_active`), e o teto pelo trigger de `company` (`max_users`). |
| RNF-02 | A checagem de token ativo custa uma consulta por PK a cada requisição autenticada. |
| RNF-03 | Erros no formato atual: 400 (dado inválido, alvo `PLATFORM_ADMIN`), 403 (outra companhia), 404 (inexistente), 409 (sem assento, último `ADMIN` ativo, a si mesmo, perfil de inativo, email de inativo). |

## 6. Contratos

### `PATCH /api/v1/users/{id}/status`

```json
{ "active": false }
```

`200` com o usuário:

```json
{ "userId": 7, "name": "Jonathan Tah", "email": "j.tah@bayern.com", "role": "MANAGER", "active": false,
  "companyId": 2, "companyType": "MARKET", "companyName": "Mercado Bom Preço", "createdAt": "..." }
```

`409` com sem assento: `{ "message": "Sem assentos livres: 10 de 10 em uso." }`.

### `GET /api/v1/users?role=MANAGER&role=AUDITOR&active=false&email=...&limit=20&offset=0`

Mesmo envelope paginado de hoje. Cada item ganha `active`.

### `GET /api/v1/companies/me`

```json
{ "...": "campos atuais, sem plan", "maxUsers": 10, "seatsUsed": 8 }
```

## 7. Fora do escopo

- Cobrança, preço, período de assinatura e troca de teto por endpoint.
- Apagar usuários.
- Revogar convites ou transferir dados de quem é desativado.
- Convite que reativa uma conta existente.

## 8. Critérios de aceite

- Com 10 ativos, `PATCH /users/{inativo}/status {active:true}` devolve 409. Depois de desativar um, a mesma chamada devolve 200.
- Com 9 ativos e 1 convite ativo, reativar e convidar devolvem 409.
- O token de um usuário desativado recebe 401 na chamada seguinte, e o login dele devolve 401.
- Desativar o único `ADMIN` ativo, ou a si mesmo, devolve 409.
- `PATCH /users/{inativo}/role` devolve 409.
- `GET /users?role=MANAGER&role=AUDITOR&active=true` só traz gestores e auditores ativos.
- O `POST /companies` sem `planCode` cria a companhia com `maxUsers = 10`.
- `GET /plans` devolve 404 depois da Task 4.
- O `./mvnw verify` passa.
