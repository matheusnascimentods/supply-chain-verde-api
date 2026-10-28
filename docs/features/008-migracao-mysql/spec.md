# Especificação — Migração de PostgreSQL para MySQL

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md).

## 1. Problema

- O projeto é acadêmico e a professora exige MySQL. Hoje o banco é PostgreSQL 17 + `pg_cron`.
- Boa parte da regra de dados vive no banco e usa recursos que só o PostgreSQL tem: auditoria por trigger genérica (`to_jsonb`, `TG_ARGV`, `SECURITY DEFINER`, `set_config(..., true)`), busca textual em português (`tsvector`, `websearch_to_tsquery`), `pg_trgm`, tipos `ENUM` nomeados, `DISTINCT ON`, procedures com `INOUT` e agendamento com `pg_cron`.
- As native queries usam sintaxe que, no MySQL, **compila e muda de sentido em silêncio**: `||` vira OR lógico, `AS "alias"` vira string literal, `CAST(... AS BIGINT/TEXT)` não existe.

## 2. Objetivo

Trocar o banco por MySQL 8.4 LTS **sem perder nenhuma feature**. O contrato HTTP fica idêntico, o frontend não muda e as regras que hoje o banco garante continuam garantidas pelo banco.

## 3. Decisões tomadas

| ID | Decisão |
|---|---|
| D-01 | Alvo: **MySQL 8.4 LTS em Docker** (`mysql:8.4`), igual no compose, no CI e no Testcontainers. |
| D-02 | Todo ambiente nasce do zero. **Não há cópia de dados** de PostgreSQL para MySQL. |
| D-03 | As 39 migrations PostgreSQL são **substituídas por um baseline MySQL consolidado**, com o schema final, os seeds, as views, as procedures, as triggers e o event. O histórico PG continua no git. |
| D-04 | A auditoria **continua no banco, por trigger** (ADR 0001), com delta por coluna e `STATUS_CHANGE`. |
| D-05 | **A separação de privilégios é mantida**: o usuário da app só pode dar `SELECT` em `audit_log`, e quem escreve é o DEFINER das triggers. |
| D-06 | Busca textual: `FULLTEXT` + `MATCH ... AGAINST` em `BOOLEAN MODE` com prefixo (`+termo*`). Os operadores web (`"frase"`, `-termo`, `or`) não são portados. |
| D-07 | A collation **reproduz o comportamento do PG**: o padrão diferencia maiúsculas e acentos, o `email` único diferencia maiúsculas e as colunas de busca textual ignoram só maiúsculas. |
| D-08 | Pronto = **todos os testes atuais rodando contra MySQL real** (Testcontainers) e passando. |
| D-09 | A documentação é atualizada junto: nova ADR, README, `docs/sql`, instruções de migration. |

## 4. Regras que precisam sobreviver

| ID | Regra | Hoje (PG) |
|---|---|---|
| RN-01 | Toda escrita nas 10 tabelas de negócio gera evento em `audit_log` na mesma transação; se a gravação do log falhar, a escrita é revertida. | V29/V35 |
| RN-02 | `INSERT` grava `after_data` completo; `DELETE` grava `before_data` completo; `UPDATE` grava só as colunas alteradas; `UPDATE` sem mudança não gera evento. | V35 |
| RN-03 | Mudança em `certification.status` ou `chain.stage_type` é `STATUS_CHANGE`; o resto é `UPDATE`. | V35 |
| RN-04 | Os snapshots nunca contêm `password` nem outras colunas sensíveis. | V30/V35 |
| RN-05 | O ator vem do principal autenticado. Valor não numérico faz a escrita falhar. Ator de uma requisição nunca vaza para outra. | `AuditActorContextAspect` |
| RN-06 | A app não insere, altera nem apaga `audit_log` diretamente. | V29 (grants) |
| RN-07 | Seeds e DDL não geram eventos de auditoria; `audit_log` começa vazia. | ADR 0001 |
| RN-08 | `updated_at` só muda quando a linha muda de fato. | V35 (`set_updated_at`) |
| RN-09 | Ordem estrita das etapas, sem repetir etapa e sem apagar a etapa anterior. | V39 |
| RN-10 | Certificações `ACTIVE` vencidas viram `EXPIRED` todo dia à meia-noite de São Paulo, sem depender da API. | V37 |
| RN-11 | Views e procedures dão os mesmos números de hoje (emissão por etapa, por fornecedor/produto, mensal, score de sustentabilidade, etapa atual, relatório). | V35/V36/V38 |
| RN-12 | Todos os `CHECK`s e FKs atuais continuam ativos. | V1–V39 |

## 5. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | `docker compose up` sobe MySQL 8.4 já com os usuários e as timezones necessários; a API sobe e o Flyway aplica o baseline sem erro. |
| RF-02 | `./mvnw verify` roda todos os testes atuais contra MySQL via Testcontainers e fica verde. |
| RF-03 | Busca de fornecedor e produto: termos múltiplos funcionam como AND e cada termo casa por prefixo. Busca vazia devolve tudo. Busca por CNPJ parcial continua funcionando. |
| RF-04 | O formato de `beforeData`/`afterData` no `GET /api/v1/audit-logs` continua um objeto JSON com os mesmos nomes de chave. Datas seguem em ISO 8601. |
| RF-05 | `POST` de relatório continua usando a procedure e devolvendo o id gerado. |
| RF-06 | Nenhuma dependência de PostgreSQL sobra no `pom.xml`, no compose, nos testes nem em `application.properties`. |

## 6. Mudanças de comportamento aceitas

| O que muda | Por quê |
|---|---|
| A busca não faz stemming em português. "certificações" ainda acha "certificação" por prefixo (`certifica*`), mas "pães" não acha "pão". | O MySQL não tem dicionário de português (D-06). |
| Os operadores `"frase exata"`, `-excluir` e `or` da busca deixam de funcionar. | D-06. |
| Termos com menos de 3 letras são ignorados pela busca textual. | `innodb_ft_min_token_size=3`, padrão do InnoDB. |
| `LIKE '%x%'` em email e CNPJ deixa de ter índice (full scan). | Não existe `pg_trgm` no MySQL. Com o volume acadêmico (~100 linhas), o impacto é desprezível. |
| O histórico de migrations recomeça em `V1`. | D-02/D-03. |

## 7. Fora de escopo

- Ferramenta de cópia de dados PG → MySQL.
- Suporte a MariaDB/XAMPP ou MySQL < 8.4.
- Dicionário/stemming de português no MySQL (plugin de parser `FULLTEXT`).
- Paridade com a sintaxe `websearch_to_tsquery`.
- Mudanças no frontend.
