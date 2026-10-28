# Especificação — Atualizar o próprio usuário (`PATCH /users/me`)

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Consumo no frontend: `supply-chain-verde-web/docs/features/008-meu-perfil/spec.md`.

## 1. Problema

- O frontend vai ter "Meu perfil" para todos os perfis. A API só permite editar o próprio nome e senha via `PATCH /suppliers/me`, que exige um fornecedor vinculado ao token.
- Admin, gestor e auditor não conseguem trocar a própria senha. Só o admin consegue alterar usuários, e só o perfil de acesso (`PATCH /users/{id}/role`).

## 2. Objetivo

Um endpoint para qualquer usuário autenticado atualizar o próprio nome e a própria senha, com as mesmas regras que o fornecedor já tem hoje.

## 3. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | `PATCH /users/me`, para qualquer perfil autenticado. Corpo: `userName`, `currentPassword` e `newPassword`, todos opcionais. Só os campos enviados são alterados. |
| RF-02 | `newPassword` exige `currentPassword` correta. Se estiver ausente ou incorreta, a resposta é 400 e nada é alterado. `userName` ou `newPassword` vazios também dão 400. |
| RF-03 | A resposta tem o mesmo formato de `GET /users/me`. |
| RF-04 | Email e perfil não são editáveis por aqui. Se esses campos vierem no corpo, são ignorados. |
| RF-05 | O usuário vem do token, nunca de um parâmetro. |
| RF-06 | A alteração entra no `audit_log` com o próprio usuário como ator. A senha nunca entra no snapshot, como já acontece hoje. |
| RF-07 | `PATCH /suppliers/me` continua igual, e as regras de nome e senha passam a ser as mesmas nos dois endpoints, sem lógica duplicada. |

## 4. Fora de escopo

- Trocar email, recuperar senha ou invalidar tokens antigos depois da troca de senha.
- Política de força de senha (o tamanho mínimo segue o que existe hoje).
