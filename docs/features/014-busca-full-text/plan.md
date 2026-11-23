# Plano técnico — Busca full text

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Diagnóstico (MySQL 8.4 local)

| Query | Resultado | Leitura |
|---|---|---|
| `MATCH(name) AGAINST('+graos*')` em `as_ci` | 0 | acento quebra a busca |
| idem em tabela `ai_ci` | 1 | a collation resolve |
| `AGAINST('+c*')` com `innodb_ft_min_token_size=3` | 23 | termo com `*` não é descartado |
| `AGAINST('+com*')` com stopwords ligadas | "Composto..." | stopword com `*` não é descartada |
| `MATCH(cnpj) AGAINST('+1234*')` | 1 | CNPJ por prefixo via FULLTEXT |
| `MATCH(email) AGAINST('+kane* +bayern*')` | 1 | email quebra em `.`/`@` |
| `MATCH(email) AGAINST('+joao_s*')` / `'+silva*'` | 1 / 0 | `_` é parte da palavra |

Conclusão: o problema está no `FullTextSearch` e na collation. A configuração do servidor fica como está, e o MySQL não precisa reiniciar.

## 2. Migration `V13__full_text_sem_acento.sql`

```sql
-- RN-03: ai_ci (accent-insensitive). Alterar a collation reconstrói os índices FULLTEXT das colunas.
ALTER TABLE product
    MODIFY name        VARCHAR(180) COLLATE utf8mb4_0900_ai_ci NOT NULL,
    MODIFY description TEXT         COLLATE utf8mb4_0900_ai_ci NULL;

ALTER TABLE company
    MODIFY name VARCHAR(180) COLLATE utf8mb4_0900_ai_ci NOT NULL,
    ADD FULLTEXT INDEX ft_company_cnpj (cnpj);   -- RN-04
```

`cnpj` ganha um índice separado, e não entra num `(name, cnpj)` combinado. O `MATCH` precisa da lista exata de colunas do índice. Com índices separados, cada termo vai para a coluna certa: o nome recebe as palavras, e o CNPJ recebe só os dígitos juntos (§3).

Os triggers de auditoria e as views (`vw_*`) leem as colunas, mas não dependem da collation, então não precisam ser recriados. A Task 1 confirma isso com o `./mvnw verify`, que roda as migrations do zero (`ViewsAndProceduresIT`, `AuditLogTriggerIT`).

## 3. `FullTextSearch`

```java
/** RN-01/RN-02: cada palavra é obrigatória e casa por prefixo; nada é descartado (o MySQL não corta termo com '*'). */
public static String booleanQuery(String search) {
    return Arrays.stream(search.replaceAll("[^\\p{L}\\p{N}_]+", " ").trim().split(" "))
            .filter(term -> !term.isEmpty())
            .map(term -> "+" + term + "*")
            .collect(Collectors.joining(" "));
}

/** RN-04: só os dígitos, juntos, como um prefixo; vazio quando não há dígito. */
public static String cnpjQuery(String search) {
    String digits = search.replaceAll("\\D", "");
    return digits.isEmpty() ? "" : "+" + digits + "*";
}
```

- `MIN_TOKEN_SIZE` e `STOPWORDS` deixam de descartar: a palavra com menos de 3 letras ou stopword vira **opcional** (sem `+`) quando há outra palavra obrigatória, e continua obrigatória sozinha. Obrigatória, ela zerava a busca quando aparece só inteira no dado (o `h` e o `com` de `h.kane@bayern.com`, o `de` de "café de montanha"). Achado na Task 3.
- `_` sai da lista de separadores (RN-05). Sem isso, "joao_silva" virava `+joao* +silva*`, e `silva` não existe no índice.
- Os operadores do modo booleano (`+ - < > ( ) ~ * " @`) continuam sendo removidos pelo regex, então o usuário não consegue injetar operador.

## 4. Busca vazia (RN-06)

Hoje as queries de company decidem pelo `search` cru (`:search = ''`). Elas passam a decidir pelo termo montado:

```sql
AND ((:booleanSearch = '' AND :cnpjSearch = '')
     OR MATCH(s.name) AGAINST (:booleanSearch IN BOOLEAN MODE)
     OR MATCH(s.cnpj) AGAINST (:cnpjSearch IN BOOLEAN MODE))
```

O mesmo vale para as 4 queries de `CompanyJpaRepository` (`findBySearch`, `findRankedByEmission`, `countBySearch`, `findIdsRankedByScore`). O parâmetro `search` cru deixa de ser usado. `MATCH ... AGAINST('')` não casa nada, então o `OR` funciona sem precisar de `CASE`.

No `ListProductsUseCase`, o ramo de busca passa a valer quando `!booleanQuery(term).isEmpty() || !categoryCode.isEmpty()`. Fora isso, cai no `findAll`.

## 5. Email (usuários e auditoria)

Migration `V14__full_text_email.sql`:

```sql
-- RN-03/RN-05. O UNIQUE passa a ignorar maiúscula/acento: falha se já houver emails que só diferem nisso (desejado).
ALTER TABLE users
    MODIFY email VARCHAR(180) COLLATE utf8mb4_0900_ai_ci NOT NULL,
    ADD FULLTEXT INDEX ft_users_email (email);
```

As duas buscas são JPQL dinâmico, e o JPQL não tem `MATCH`. Registrar uma função no Hibernate (`FunctionContributor`) seria abstração para dois usos. Em vez disso, os ids são resolvidos por uma native query e passados ao JPQL:

```java
// UserJpaRepository
@Query(value = "SELECT id FROM users WHERE MATCH(email) AGAINST (:booleanSearch IN BOOLEAN MODE)", nativeQuery = true)
List<Long> findIdsByEmailSearch(@Param("booleanSearch") String booleanSearch);
```

- `UserRepositoryImpl.where/bindFilters`: `LOWER(user.email) LIKE :emailPattern` vira `user.userId IN :emailUserIds`.
- `AuditLogRepositoryImpl` (listagem e contagem): `LOWER(auditUser.email) LIKE :userEmailPattern` vira `auditUser.userId IN :emailUserIds`.
- Lista de ids vazia com termo válido: retorna página vazia / contagem 0 antes do JPQL, e o `IN ()` nunca chega ao banco.
- Termo sem letras nem dígitos (RN-06): o filtro de email é ignorado.

`users` é pequena (dezenas a milhares de linhas). O `IN` com os ids casados não justifica um join nativo.

## 6. Teto conhecido

O índice não guarda palavras de 1–2 letras nem as stopwords em inglês **dos dados** (`innodb_ft_min_token_size=3`, `innodb_ft_enable_stopword=ON`). Para indexá-las:

1. Adicionar `--innodb-ft-min-token-size=2 --innodb-ft-enable-stopword=OFF` ao `command` do `docker-compose.yml` e ao `withCommand` do `TestcontainersConfiguration`.
2. Fazer o deploy subir o `mysql` também (`docker compose up -d mysql $SERVICE` no `ci.yml`). Hoje só a api reinicia, e as flags não entrariam.
3. Criar uma migration com `OPTIMIZE TABLE` (ou recriar os índices) **depois** que o MySQL reiniciar com as flags.

Isso só vale a pena se alguém precisar achar "SP" ou "de" como palavra inteira. Os nomes atuais não justificam.

## 7. Testes

- **`FullTextSearchTest`** (unitário):
  - "ca" → `+ca*`; "c" → `+c*`; "com" → `+com*`.
  - "joao_silva" → `+joao_silva*`; "--" → `""`; "Grãos da" → `+Grãos* da*`; "h.kane@bayern.com" → `h* +kane* +bayern* com*`.
  - `cnpjQuery("12.345")` → `+12345*`; `cnpjQuery("abc")` → `""`.
- **`FullTextSearchIT`** (novo, Testcontainers, sobre o seed):
  - produtos `graos`/`organico` com o mesmo resultado que `grãos`/`orgânico`;
  - fornecedores `c`, `com` e `12.345` (CNPJ de um fornecedor do seed);
  - usuários e auditoria por `KANE`;
  - "--" lista tudo.
  - Cada task adiciona os casos dela.
