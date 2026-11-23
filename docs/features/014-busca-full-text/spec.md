# Especificação — Busca full text

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Contraparte no frontend: `supply-chain-verde-web/docs/features/013-busca-full-text/`.

## 1. Problema

As buscas demoram a "achar" um termo, mesmo com índices FULLTEXT. Medido no MySQL local:

- **Acento.** `product.name`, `product.description` e `company.name` usam `utf8mb4_0900_as_ci`, que é *accent-sensitive*. `graos` volta 0 e `grãos` volta 2; `organico` volta 0 e `orgânico` volta 10. Quem digita sem acento só acha quando completa a palavra com o acento.
- **Termo curto e stopword.** O `FullTextSearch.booleanQuery` descarta palavras com menos de 3 letras e as stopwords em inglês do InnoDB (`com`, `for`...). Com "ca" a query fica vazia, mas o `search` não está vazio, então a lista volta **zero** até a 3ª letra. "Com" (Comercial, Composto) também zera. O MySQL não precisa desse corte: com o operador `*`, ele não descarta termo curto nem stopword na query (`+c*` acha 23 empresas, `+com*` acha "Composto Vivo...").
- **CNPJ e email com `LIKE '%...%'`.** A busca de fornecedores casa CNPJ por `LIKE`, e as buscas de usuários e de auditoria casam email por `LOWER(email) LIKE`. Nenhuma das três usa índice.

## 2. Objetivo

Toda searchbar do front acha o termo desde a 1ª letra, sem depender de acento nem de maiúscula, usando só os índices FULLTEXT, sem `LIKE`.

## 3. Searchbars atendidas

| Tela (web) | Rota | Hoje | Depois |
|---|---|---|---|
| Produtos | `GET /products?search=` | `MATCH(name, description)` | igual, sem acento e sem corte de termo |
| Fornecedores | `GET /suppliers?ranked=true&search=` | `MATCH(name)` + `cnpj LIKE` | `MATCH(name)` + `MATCH(cnpj)` |
| Pedido de lote (fornecedor; produto na feature 015) | `GET /suppliers?ranked=true&search=` | idem | idem |
| Usuários | `GET /users?email=` | `LOWER(email) LIKE` | `MATCH(email)` |
| Auditoria e exportação | `GET /audit-logs?userEmail=` | `LOWER(email) LIKE` | `MATCH(email)` |

## 4. Regras de negócio

| ID | Regra |
|---|---|
| RN-01 | Cada palavra digitada é obrigatória e casa pelo **início** de uma palavra indexada (`+termo*`). "ara" acha "Arábica"; "rábica" não. |
| RN-02 | Nenhuma palavra é descartada pelo tamanho ou por ser stopword. "c" já filtra. Ao lado de outra palavra, uma palavra que o índice não guarda inteira (menos de 3 letras ou stopword) vira opcional: "café de montanha" e "h.kane@bayern.com" acham, em vez de zerar. |
| RN-03 | Acento e maiúscula não importam: as colunas buscadas passam a `utf8mb4_0900_ai_ci`. |
| RN-04 | CNPJ casa pelo início dos dígitos, com ou sem máscara: "12.345" e "12345" acham `12345678000199`. |
| RN-05 | Email é quebrado em palavras por `.`, `@` e `-`. `_` faz parte da palavra. "kane", "bayern" e "h.kane@bayern.com" acham `h.kane@bayern.com`; "silva" **não** acha `joao_silva@x.com`, e "joao_s" acha. |
| RN-06 | Um termo sem letras nem dígitos (ex.: "--") vale como busca vazia: lista tudo, em vez de zero. |
| RN-07 | Nenhuma busca textual usa `LIKE`. |

## 5. Contrato

Nenhuma rota nova e nenhum parâmetro novo. Muda o resultado dos `search`/`email`/`userEmail` existentes conforme as RNs.

Efeito colateral da RN-03 em `users.email`: o `UNIQUE` e a comparação no login passam a ignorar maiúscula e acento. `H.Kane@Bayern.com` entra com a conta de `h.kane@bayern.com`, e não dá mais para cadastrar os dois.

## 6. Fora do escopo

- Busca no meio da palavra (parser `ngram`). Ela traz ruído (por exemplo, "ca" casaria "Distribui**ção**"), e o `*` não funciona direito com ngram.
- Indexar palavras de 1–2 letras e stopwords **dos dados** ("SP", "de"). O índice continua ignorando essas palavras: "sp" acha "Spirulina", mas não acha a palavra "SP" sozinha. A melhoria possível está descrita no plan §6.
- Ordenar por relevância (`MATCH` no `ORDER BY`). As ordens de hoje continuam.
- Busca por trecho do meio do CNPJ.

## 7. Critérios de aceite

- `GET /products?search=graos` e `?search=organico` acham os mesmos produtos que com acento.
- `GET /suppliers?ranked=true&search=c` volta fornecedores. `?search=com` acha "Composto Vivo Reciclagem Orgânica".
- `GET /suppliers?ranked=true&search=12.345` acha o fornecedor de CNPJ `12345...`.
- `GET /users?email=KANE` acha `h.kane@bayern.com`, e a auditoria filtra igual.
- O `./mvnw verify` passa, e não sobra `LIKE` em busca textual (`grep -rn "LIKE" src/main/java`).
