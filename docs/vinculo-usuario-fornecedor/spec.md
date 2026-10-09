# Especificação — Vínculo entre usuário e fornecedor

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Contraparte no frontend: `supply-chain-verde-web/docs/vinculo-usuario-fornecedor/`.

## 1. Problema

- Não existe relação entre `users` e `supplier` no banco. A API usa a convenção `userId == supplierId` (ver `docs/spec.md`, seção de lotes), que quebra assim que os IDs divergirem.
- O usuário `SUPPLIER` não consegue salvar o próprio perfil: o frontend chama `PUT /suppliers/{id}`, liberado só para `ADMIN` e `MANAGER` (403).
- Só o `ADMIN` cria usuários, então o `MANAGER` cadastra fornecedores mas não consegue dar acesso a eles.
- Não há como dar login a um fornecedor que já existe.

## 2. Objetivo

Relacionar usuário e fornecedor de forma explícita (1:1), permitir criar o acesso junto com o fornecedor ou depois, e deixar o próprio fornecedor manter seus dados.

## 3. Regras de negócio

| ID | Regra |
|---|---|
| RN-01 | A relação é 1:1 e opcional do lado do fornecedor: `supplier.user_id` pode ser nulo, e um usuário está ligado a no máximo um fornecedor. |
| RN-02 | Só usuário com role `SUPPLIER` pode ser vinculado a um fornecedor. |
| RN-03 | Todo usuário com role `SUPPLIER` está vinculado a exatamente um fornecedor. Não existe `SUPPLIER` sem fornecedor. |
| RN-04 | Ao criar um fornecedor, o acesso (usuário `SUPPLIER`) pode ser criado junto. Fornecedor e usuário são gravados na mesma transação: falha em um desfaz os dois. |
| RN-05 | Ao criar um usuário `SUPPLIER`, ele é vinculado a um fornecedor existente que ainda não tem usuário. É o caminho para um fornecedor já cadastrado "virar usuário". |
| RN-06 | O `MANAGER` cria fornecedores (com ou sem acesso) e usuários, mas só usuários `SUPPLIER`. Na listagem de usuários ele vê apenas usuários `SUPPLIER`. |
| RN-07 | O `SUPPLIER` edita o próprio cadastro: razão social, telefone, endereço, seu nome e sua senha. CNPJ e email não são editáveis por ele. |
| RN-08 | A troca de senha exige a senha atual. |
| RN-09 | Toda verificação de "dono" (lotes, relatórios, certificações) passa a usar o `supplierId` do vínculo, não o `userId`. |

## 4. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | Coluna `supplier.user_id` (FK para `users`, `UNIQUE`, nula), com migração dos vínculos existentes pela convenção atual. |
| RF-02 | `POST /suppliers` aceita um bloco opcional `user { name, email, password }`, que cria o usuário `SUPPLIER` já vinculado. Acesso: `ADMIN`, `MANAGER`. |
| RF-03 | `POST /users` aceita `supplierId`: obrigatório quando `role = SUPPLIER`, proibido nos demais roles. Acesso: `ADMIN` (qualquer role) e `MANAGER` (só `SUPPLIER`). |
| RF-04 | `GET /users` liberado para `MANAGER`, que recebe só usuários `SUPPLIER`. Os itens passam a trazer `supplierId` e `supplierName` (nulos fora de `SUPPLIER`). |
| RF-05 | `GET /suppliers` aceita `withoutUser=true` para listar só fornecedores sem acesso (usado na criação de usuário). |
| RF-06 | `PATCH /suppliers/me`, só `SUPPLIER`: atualização parcial de `supplier`, `address` e `users` numa transação. |
| RF-07 | Login, token e `GET /users/me` passam a expor `supplierId` (nulo fora de `SUPPLIER`). |
| RF-08 | `PATCH /users/{id}/role` recusa (409) mudar o role **de** ou **para** `SUPPLIER`, porque quebraria RN-02/RN-03. |
| RNF-01 | Erros de vínculo retornam 409 (fornecedor já tem usuário, email duplicado) ou 400 (combinação inválida de `role`/`supplierId`), no formato de erro atual. |
| RNF-02 | Toda alteração continua auditada pelos triggers de `audit_log`. |

## 5. Contratos

### `POST /suppliers`

```json
{
  "name": "Fazenda Verde LTDA",
  "cnpj": "12345678000190",
  "phone": "11999990000",
  "address": { "street": "...", "number": "10", "neighborhood": "...", "complement": null, "zipCode": "01001000", "city": "São Paulo", "state": "SP" },
  "user": { "name": "Ana Souza", "email": "ana@fazendaverde.com", "password": "********" }
}
```

`user` é opcional. A resposta inclui `userId` (nulo quando não houver acesso).

### `POST /users`

```json
{ "name": "Ana Souza", "email": "ana@fazendaverde.com", "password": "********", "role": "SUPPLIER", "supplierId": 7 }
```

### `PATCH /suppliers/me`

Todos os campos são opcionais; só os enviados são alterados.

```json
{
  "name": "Fazenda Verde Orgânicos LTDA",
  "phone": "11988887777",
  "address": { "street": "...", "number": "12", "neighborhood": "...", "complement": "Galpão 2", "zipCode": "01001000", "city": "São Paulo", "state": "SP" },
  "userName": "Ana S. Souza",
  "currentPassword": "senha-atual",
  "newPassword": "senha-nova"
}
```

- `name` é a razão social (`supplier.name`); `userName` é o nome do usuário (`users.name`).
- `newPassword` exige `currentPassword`. Senha atual errada retorna 400 sem alterar nada.
- Resposta: o fornecedor atualizado, no mesmo formato de `GET /suppliers?supplierId=`.

## 6. Fora de escopo

- Vários usuários por fornecedor (ex.: funcionários de uma cooperativa).
- Troca de email pelo próprio usuário.
- Troca de senha para os outros roles.

## 7. Questões em aberto

- RF-08 foi uma decisão assumida para proteger RN-02/RN-03. Se for preciso transformar um `SUPPLIER` em outro role, o fluxo vira "desvincular, depois trocar o role".
