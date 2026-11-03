# Especificação — Empresas, planos e isolamento por companhia

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Contraparte no frontend: `supply-chain-verde-web/docs/features/009-empresas-planos/`.

## 1. Problema

- A plataforma é de uma empresa só. Fornecedores são cadastrados por `ADMIN`/`MANAGER`, e todo usuário que não é `SUPPLIER` vê todos os dados: lotes, produtos, ranking, auditoria e usuários.
- Não há como um varejista (mercado) ou um fornecedor se cadastrar sozinho.
- O `ADMIN` é ao mesmo tempo "dono da plataforma" e "administrador do cliente".
- `supplier` tem relação 1:1 com `users`, então um fornecedor não pode ter mais de um usuário.
- Algumas escritas não verificam de quem é o dado: etapas, transporte, emissão, status de certificação e geração de relatório.

## 2. Objetivo

Transformar a plataforma em multiempresa:

- Varejistas e fornecedores se cadastram sozinhos escolhendo um plano. Cada empresa só vê o que é dela.
- O mercado pede lotes aos fornecedores, e o fornecedor executa ou recusa.
- Um novo perfil `PLATFORM_ADMIN` mantém a visão global que hoje é do `ADMIN`.

## 3. Glossário

| Termo | Significado |
|---|---|
| Companhia | Linha de `company`. `type` é `MARKET` (varejista/mercado) ou `SUPPLIER` (fornecedor). |
| Plano | Linha do catálogo `plan`: `BASIC` (3 contas), `PRO` (10), `PRO_MAX` (15). |
| Assinatura | Linha de `company_plan` que liga a companhia a um plano, com início e fim. Só uma fica ativa por vez (`ended_at` nulo). |
| Fornecedores do mercado | Fornecedores que têm ou tiveram ao menos um lote com aquele mercado, em qualquer status. |

## 4. Regras de negócio

### Companhia e plano

| ID | Regra |
|---|---|
| RN-01 | `company` substitui `supplier` e tem os mesmos dados (razão social, CNPJ, endereço, telefone), mais `email` e `type`. O CNPJ é único na plataforma. |
| RN-02 | Toda referência que hoje aponta para fornecedor (`batch`, `certification`, `report`, e agora `product`) só aceita uma companhia com `type = SUPPLIER`. O banco garante isso. |
| RN-03 | Toda companhia tem exatamente uma assinatura ativa. O cadastro cria a primeira. Troca de plano não faz parte desta feature. |
| RN-04 | O número de usuários de uma companhia nunca passa do `max_users` do plano ativo. Criar um usuário além do limite devolve 409. |
| RN-05 | `type` e CNPJ não mudam depois do cadastro. |

### Usuários e perfis

| ID | Regra |
|---|---|
| RN-06 | Todo usuário pertence a uma companhia, exceto o `PLATFORM_ADMIN`, que não pertence a nenhuma. |
| RN-07 | Perfis permitidos por tipo: `MARKET` tem `ADMIN`, `MANAGER` e `AUDITOR`. `SUPPLIER` tem `ADMIN` e `SUPPLIER`. |
| RN-08 | Toda companhia tem pelo menos um `ADMIN`. Qualquer troca de perfil que deixe a companhia sem `ADMIN` devolve 409, inclusive o único admin trocando o próprio perfil. |
| RN-09 | Só o `ADMIN` da companhia cria usuários e troca perfis, e só na própria companhia. O `MANAGER` deixa de criar usuários. |
| RN-10 | O `PLATFORM_ADMIN` vê tudo, sem filtro por companhia, como o `ADMIN` de hoje. Ele só nasce por migration ou seed: nenhum endpoint cria esse perfil ou promove alguém a ele. |
| RN-11 | O `ADMIN` de uma companhia `SUPPLIER` pode tudo o que o `SUPPLIER` pode, e também gerencia usuários e os dados da companhia. |

### Isolamento

| ID | Regra |
|---|---|
| RN-12 | Toda leitura é filtrada pela companhia do usuário logado. Mercado: lotes e relatórios com `market_id` dele. Produtos e certificações de fornecedores fazem parte do catálogo (RN-13), então o mercado os lê de qualquer fornecedor, porque precisa deles para pedir lote a um fornecedor novo. Fornecedor: os próprios lotes, produtos, certificações e relatórios. |
| RN-13 | Exceção ao RN-12: a lista e o ranking de fornecedores (`GET /suppliers`, inclusive a recomendação por categoria usada para criar lote) são um catálogo global para o mercado, para ele descobrir fornecedores novos. Cada item traz `workedWith`, que indica se o fornecedor é do mercado (glossário), e o filtro `workedWith=true` restringe a lista a eles. Score, CO₂ total e certificações ativas são métricas públicas do fornecedor. A contagem de relatórios conta só os relatórios do mercado logado. |
| RN-13a | Companhias `SUPPLIER` não acessam a lista nem o ranking de fornecedores (403). Elas só leem o próprio detalhe. |
| RN-13b | Todo usuário vê a lista de usuários da própria companhia. Só o `ADMIN` cria usuários e troca perfis (RN-09). |
| RN-13c | Dashboard: o card de fornecedores conta, para o mercado, os fornecedores com `workedWith`. Para companhia `SUPPLIER`, o card é trocado por lotes `PENDING` aguardando ação dela. |
| RN-14 | Auditoria: cada companhia vê só as ações feitas pelos próprios usuários (filtro pela companhia do autor). O `PLATFORM_ADMIN` vê tudo. |
| RN-15 | Escrever em um dado fora do escopo devolve 403, inclusive nas escritas que hoje não verificam o dono (etapa, transporte, emissão, certificação, relatório). |

### Produto e lote

| ID | Regra |
|---|---|
| RN-16 | O produto pertence a um fornecedor, que é o único que cadastra e edita os próprios produtos. |
| RN-17 | Só o mercado cria lotes (`MANAGER` ou `ADMIN` de companhia `MARKET`). O lote liga o mercado a um fornecedor e a um produto **desse** fornecedor. O banco garante isso. |
| RN-18 | O lote nasce `PENDING`. Só o fornecedor do lote registra a primeira etapa (`PRODUCTION`), e isso muda o lote para `ACTIVE`. |
| RN-19 | Enquanto o lote estiver `PENDING`, o fornecedor pode recusá-lo informando um motivo obrigatório. O lote vira `REJECTED`, que é final: não recebe etapas e não volta a outro status. |
| RN-20 | `produced_at` deixa de ser informado na criação. Ele é preenchido com a data de início da etapa `PRODUCTION`. |
| RN-21 | A recomendação de fornecedores compara apenas por `category` + `unit` (RN-03 da feature 001). A busca por `productId` sai, porque o produto agora é de um fornecedor só. |

### Certificação e relatório

| ID | Regra |
|---|---|
| RN-22 | O fornecedor gerencia as próprias certificações: cria e muda o status. O `AUDITOR` do mercado só consulta. |
| RN-23 | O relatório de sustentabilidade é gerado pelo mercado para um dos fornecedores dele e considera só os lotes daquele mercado. O fornecedor vê os relatórios sobre ele mesmo. |

### Migração dos dados

| ID | Regra |
|---|---|
| RN-24 | Cada `supplier` existente vira uma `company` `SUPPLIER` com o **mesmo id** e plano `BASIC`. O usuário `SUPPLIER` vinculado vira o `ADMIN` dessa companhia. |
| RN-25 | É criada uma companhia `MARKET` de demonstração com plano `PRO_MAX`. Ela recebe todos os lotes e relatórios, e os usuários `ADMIN`, `MANAGER` e `AUDITOR` existentes, menos um. |
| RN-26 | Esse `ADMIN` restante, o de menor id do seed, vira `PLATFORM_ADMIN`. |
| RN-27 | Lotes existentes viram `ACTIVE`. |
| RN-28 | Produtos existentes passam a pertencer ao fornecedor dos lotes deles. Um produto usado por mais de um fornecedor é duplicado, uma cópia por fornecedor, e os lotes passam a apontar para a cópia do próprio fornecedor. Um produto sem lote vai para o fornecedor de menor id. |

## 5. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | `GET /plans`, público: catálogo com `code`, `name` e `maxUsers`. |
| RF-02 | `POST /companies`, público: cria companhia, endereço, assinatura e usuário `ADMIN` numa transação. A resposta é igual à do `POST /auth/login`, então o usuário já entra logado. |
| RF-03 | `GET /companies/me` (qualquer usuário com companhia) e `PATCH /companies/me` (só `ADMIN`): dados da companhia, endereço e plano ativo. `PATCH` altera razão social, email, telefone e endereço. |
| RF-04 | Login, token e `GET /users/me` passam a expor `companyId`, `companyType` e `companyName`. `supplierId` continua no token e no `GET /users/me`, igual a `companyId` quando a companhia é `SUPPLIER`, para não quebrar o front. |
| RF-05 | Saem `POST /suppliers`, `PUT /suppliers/{id}` e `PATCH /suppliers/me`, junto com o `supplierId` de `POST /users` e o filtro `withoutUser`. |
| RF-06 | `POST /users` e `PATCH /users/{id}/role` aplicam RN-04, RN-07, RN-08 e RN-09. `GET /users` é liberado para qualquer usuário com companhia e lista a companhia dele (RN-13b). Para o `PLATFORM_ADMIN`, lista todos e traz `companyName` e `companyType`. |
| RF-06a | `GET /suppliers` (lista e ranking): cada item ganha `workedWith` (boolean), e a rota aceita `workedWith=true` (RN-13). Para companhia `SUPPLIER`, devolve 403, exceto `GET /suppliers?supplierId=<a própria>` (RN-13a). Para o `PLATFORM_ADMIN`, `workedWith` vem sempre `false`. |
| RF-06b | `GET /dashboard/summary` ganha `pendingBatches` (RN-13c). Para o mercado, `suppliers` passa a contar só os fornecedores com `workedWith`. |
| RF-07 | `POST /products` e `PUT /products/{id}` são do fornecedor (RN-16). `GET /products` aceita `supplierId` e respeita o RN-12. Esse filtro é usado no modal de lote. |
| RF-08 | `POST /batches` recebe `{ productId, supplierId, quantity }`, e o mercado vem do token. A resposta e as listagens ganham `status`, `rejectionReason`, `marketId` e `marketName`. |
| RF-09 | `GET /batches` aceita `status`. |
| RF-10 | `PATCH /batches/{id}/reject` com `{ reason }`, só para o fornecedor do lote (RN-19). Se o lote não estiver `PENDING`, devolve 409. |
| RF-11 | Registrar a primeira etapa muda o lote para `ACTIVE` (RN-18). Registrar etapa em lote `REJECTED` devolve 409. |
| RF-12 | `PATCH /certifications/{id}` (status) é liberado para `SUPPLIER` e `ADMIN` do fornecedor dono, e deixa de ser liberado para `AUDITOR`. |
| RF-13 | Dashboard (`/summary`, `/emissions`), ranking, relatórios, certificações e auditoria respeitam RN-12 a RN-14. |
| RNF-01 | Os vínculos de tipo (RN-02, RN-17) e "uma assinatura ativa" (RN-03) são garantidos por constraint no banco, não só no código. |
| RNF-02 | `company`, `company_plan` e as colunas novas de `users`, `batch` e `product` entram na auditoria por trigger, seguindo o padrão da V5. A mudança de `batch.status` é registrada como `STATUS_CHANGE`. |
| RNF-03 | Erros seguem o formato atual: 400 para dado inválido, 403 para fora do escopo, 404 para inexistente, 409 para conflito (CNPJ ou email duplicado, limite do plano, último admin, status do lote). |

## 6. Contratos

### `POST /companies`

```json
{
  "planCode": "PRO",
  "type": "MARKET",
  "company": {
    "name": "Mercado Bom Preço LTDA",
    "cnpj": "12345678000190",
    "email": "contato@bompreco.com",
    "phone": "11999990000",
    "address": { "street": "...", "number": "10", "neighborhood": "...", "complement": null, "zipCode": "01001000", "city": "São Paulo", "state": "SP" }
  },
  "admin": { "name": "Ana Souza", "email": "ana@bompreco.com", "password": "********" }
}
```

`201` com o mesmo corpo do login: `{ token, expiresAt, role, supplierId, companyId, companyType }`.

### `GET /plans`

```json
[{ "code": "BASIC", "name": "Basic", "maxUsers": 3 }, { "code": "PRO", "name": "Pro", "maxUsers": 10 }, { "code": "PRO_MAX", "name": "Pro Max", "maxUsers": 15 }]
```

### `PATCH /batches/{id}/reject`

```json
{ "reason": "Sem capacidade de produção para o período." }
```

## 7. Fora de escopo

- Cobrança, troca de plano e CRUD do catálogo de planos. As tabelas já guardam histórico para quando isso entrar.
- Confirmação de email, recuperação de senha, captcha e rate limit no `POST /companies`, que é público. Fica registrado como risco.
- Validação externa das certificações. A certificação autodeclarada enfraquece o argumento contra greenwashing, e uma validação pelo `PLATFORM_ADMIN` fica para outra feature.
- Desativar ou excluir usuários e companhias.
- Auditoria do que **outra** companhia fez nos meus dados (por exemplo, a etapa que o fornecedor registrou no lote do mercado). Isso continua visível na timeline do lote.
