# Token de convite opaco, guardado só como hash SHA-256

*Template: decision record de Jeff Tyree e Art Akerman, “Architecture Decisions: Demystifying Architecture”.*

## Issue

O convite da feature 011 vai por email como um link para o front. Quem abre o link consulta o convite e cria a própria conta sem estar logado. O link precisa ser impossível de adivinhar, valer por 24h e uma vez só (RN-06, RN-07), e não pode virar uma credencial válida se o banco vazar. Requisitos em [`../features/011-convites/spec.md`](../features/011-convites/spec.md).

## Decision

O link leva um token opaco: 32 bytes de `SecureRandom` em base64url sem padding, com 43 caracteres. O banco guarda só o SHA-256 dele, em hex, em `invite.token_hash`, que tem `UNIQUE`. A consulta e o aceite fazem o hash do token recebido e buscam por ele. A validade e o uso único ficam na linha do convite (`expires_at` gerado e `accepted`), e não no token.

## Status

Aceita e implementada nas Tasks 1 a 3 da feature 011 (`InviteToken`, V12). Coberta por `InviteTokenTest`, `InviteSchemaIT` e `InviteFlowIT`.

## Group

Segurança.

## Assumptions

- O email é o canal de confiança: quem lê a caixa de entrada do convidado pode aceitar o convite.
- O volume de convites é baixo, e uma busca por `token_hash` com índice único basta.

## Constraints

- As rotas de consulta e aceite são públicas, porque o convidado ainda não tem conta.
- O mesmo token serve para consultar (`GET`, que não consome) e para aceitar (`POST`). Ele precisa continuar válido entre as duas chamadas, então não pode ser de leitura única.

## Positions

1. **Token opaco aleatório com hash SHA-256 no banco.** Escolhida. Segue a recomendação do OWASP Forgot Password Cheat Sheet para tokens enviados por email.
2. **Id do convite no link.** Rejeitada: é sequencial, então dá para enumerar, e qualquer um aceitaria convites alheios.
3. **JWT assinado no link.** Rejeitada:
   - O uso único e a revogação continuariam exigindo estado no banco.
   - O link ficaria longo e carregaria dados legíveis (email, perfil).
   - Um vazamento do `JWT_SECRET` permitiria forjar convites para qualquer companhia.
4. **Token aleatório guardado em texto puro.** Rejeitada: um dump do banco, um backup ou um log de query exporia links válidos.

## Argument

256 bits aleatórios tornam a força bruta inviável, então um hash rápido como o SHA-256 basta. Diferente de senhas, não há entropia baixa a proteger, e bcrypt ou argon2 só deixariam cada consulta mais lenta. Com o hash determinístico, a busca usa o índice único, sem comparar linha a linha. Só a biblioteca padrão é usada (`SecureRandom`, `MessageDigest`, `HexFormat`), sem dependência nova.

## Implications

- O token só existe no email e na URL. Um convite perdido não pode ser reenviado com o mesmo link: o caminho é esperar expirar e convidar de novo.
- As triggers de auditoria deixam `token_hash` de fora do JSON.
- O aceite vai por `POST` com o token no corpo, para não ficar em logs de acesso. O `GET` leva o token no path porque é ele que está no link.
- Token desconhecido, expirado ou já aceito tem a mesma resposta (404), para não revelar qual dos três é (RN-08).

## Related artifacts

- [`../features/011-convites/plan.md`](../features/011-convites/plan.md) §4: geração e hash do token.
- [OWASP Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).
