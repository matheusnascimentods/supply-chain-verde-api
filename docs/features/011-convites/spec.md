# Especificação — Convite de usuários por email

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Contraparte no frontend: `supply-chain-verde-web/docs/features/010-convites/`.

## 1. Problema

- O `ADMIN` da companhia cria o usuário (`POST /users`) e escolhe a senha dele. A senha tem de ser repassada por fora da plataforma, e o usuário nunca confirma que o email é dele.
- Não existe envio de email na API.

## 2. Objetivo

O `ADMIN` informa só o email e o perfil. A API manda um convite por email (Twilio SendGrid) com um link para o front. O convidado abre o link, informa nome e senha e já entra logado. A criação direta de usuário pelo admin deixa de existir.

## 3. Glossário

| Termo | Significado |
|---|---|
| Convite | Linha de `invite`. Liga o admin que convidou (e, por ele, a companhia), o email convidado e o perfil. |
| Token | Segredo aleatório que vai no link do email. O banco guarda só o hash SHA-256 dele, nunca o token. |
| Convite ativo | `accepted = FALSE` e `expires_at > agora`. Não há coluna de status: se a linha existe, o email foi enviado. |

## 4. Regras de negócio

| ID | Regra |
|---|---|
| RN-01 | Só o `ADMIN` de uma companhia convida, e sempre para a própria companhia. O `PLATFORM_ADMIN` não tem companhia e recebe 403. |
| RN-02 | O admin escolhe o perfil no convite. Valem os perfis por tipo de companhia (RN-07 da feature 010): perfil não permitido retorna 400. |
| RN-03 | Email que já tem usuário na plataforma retorna 409. O email é único globalmente, como hoje. |
| RN-04 | Só pode existir um convite ativo por email. Convidar um email com convite ativo retorna 409. Não há reenvio nem cancelamento: o admin espera expirar. |
| RN-05 | Limite do plano: usuários da companhia + convites ativos dela ≤ `max_users`. Passar disso no envio retorna 409. O aceite confere de novo, só com usuários, sob o mesmo lock de companhia da RN-04 da feature 010. |
| RN-06 | O convite vale 24h a partir do envio. `expires_at` é uma coluna gerada (`created_at + 24h`) e ninguém a grava. Expirado é `expires_at <= agora`, comparado sempre pelo relógio do banco: não há rotina no banco nem status gravado. |
| RN-07 | O token é de uso único. O aceite marca `accepted = TRUE` na mesma transação que cria o usuário, e um token aceito ou expirado não serve mais. |
| RN-08 | Token desconhecido, expirado ou já aceito tem a mesma resposta (404, "Convite inválido ou expirado."). Não se revela qual dos três casos é. |
| RN-09 | Consultar o convite (`GET`) não muda nada. Só o `POST` de aceite consome o token. Assim, scanners de email que abrem o link não queimam o convite. |
| RN-10 | O aceite cria o usuário com o email e o perfil do convite, na companhia de quem convidou. O convidado informa só o nome e a senha, e a resposta é um token JWT, igual à do login. |
| RN-11 | Se o email não sai (erro do SendGrid), nada é gravado e a API retorna 502. O admin tenta de novo. |
| RN-12 | O link é montado com a URL do front configurada (`app.frontend-url`), nunca com o `Host` da requisição. |
| RN-13 | `POST /users` deixa de existir. O primeiro `ADMIN` continua nascendo no `POST /companies`, e todos os outros usuários entram por convite. |

## 5. Contrato

| Método e rota | Acesso | Corpo | Resposta |
|---|---|---|---|
| `POST /api/v1/invites` | `ADMIN` | `{ email, role }` | 201 sem corpo. Também 400 (perfil), 403 (`PLATFORM_ADMIN`), 409 (RN-03, RN-04, RN-05) e 502 (RN-11). |
| `GET /api/v1/invites/{token}` | público | — | 200 `{ email, role, companyName, expiresAt }` ou 404 (RN-08) |
| `POST /api/v1/invites/accept` | público | `{ token, name, password }` | 200 `LoginResponseDTO` (o mesmo de `/auth/login`), 400 (nome ou senha vazios), 404 (RN-08) ou 409 (limite do plano no aceite) |
| `POST /api/v1/users` | — | — | **removido** |

O token vai no path do `GET` porque é ele que está no link. No aceite, ele vai no corpo para não ficar em logs de acesso de um `POST`.

## 6. Fora do escopo

- Listar, reenviar ou cancelar convites.
- Rastrear abertura de email e webhooks do SendGrid.
- Fila ou retentativa de envio.
- Convites para o `PLATFORM_ADMIN`.

## 7. Critérios de aceite

- O admin convida, o email sai com o link `<frontend-url>/convites/<token>`, o convidado aceita e entra logado com o perfil do convite.
- O mesmo token falha na segunda vez, e também depois de 24h.
- Com o plano cheio contando os convites ativos, o envio retorna 409.
- O banco nunca guarda o token em texto puro.
- O `./mvnw verify` passa sem chave real do SendGrid.
