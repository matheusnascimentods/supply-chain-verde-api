# Tipo de companhia e assinatura ativa garantidos pelo banco

*Template: decision record de Jeff Tyree e Art Akerman, “Architecture Decisions: Demystifying Architecture”.*

## Issue

A feature 010 troca `supplier` por `company`, com `type` `MARKET` ou `SUPPLIER`. Toda referência a fornecedor (`batch`, `certification`, `report`, `product`) só pode apontar para uma companhia `SUPPLIER`, e `batch.market_id` e `report.market_id` só para uma `MARKET` (RN-02, RN-17). Cada companhia tem uma assinatura ativa por vez (RN-03). O RNF-01 pede que essas regras fiquem no banco, não só no código. Requisitos em [`../features/010-empresas-planos/spec.md`](../features/010-empresas-planos/spec.md).

## Decision

1. **FK composta por tipo.** `company` tem `UNIQUE (id, type)`. Quem referencia ganha uma coluna gerada constante, `supplier_type ... GENERATED ALWAYS AS ('SUPPLIER') STORED` (ou `market_type` com `'MARKET'`), e a FK vira `(supplier_id, supplier_type) → company (id, type)`. Uma companhia do tipo errado não satisfaz a FK.
2. **Produto do fornecedor do lote.** `product` tem `UNIQUE (id, supplier_id)`, e `batch` tem `FOREIGN KEY (product_id, supplier_id) → product (id, supplier_id)`.
3. **Assinatura ativa única.** `company_plan.active_company_id` é gerada como `IF(ended_at IS NULL, company_id, NULL)` e tem `UNIQUE`. Como o `UNIQUE` ignora `NULL`, as assinaturas encerradas não colidem, e só uma linha por companhia fica com `ended_at` nulo.

## Status

Aceita e implementada nas migrations V7–V9 (feature 010). Coberta por `CompanyConstraintsIT`.

## Group

Dados e integridade.

## Assumptions

- O tipo e o CNPJ de uma companhia não mudam depois do cadastro (RN-05): a FK composta nunca precisa propagar uma troca de `type`.
- O histórico de planos fica em `company_plan`, ainda sem troca de plano (fora de escopo).

## Constraints

- O MySQL 8.4 não aceita subquery em `CHECK`, então o tipo da companhia referenciada não pode ser checado por `CHECK`.
- Uma FK sobre coluna gerada `STORED` não aceita `ON UPDATE`/`ON DELETE` em `CASCADE`, `SET NULL` ou `SET DEFAULT`; as FKs ficam com a ação padrão (`RESTRICT`).

## Positions

1. **FK composta com coluna gerada constante.** Escolhida. É o mesmo truque de `chain.previous_stage_type` (V1) para a ordem das etapas.
2. **Trigger `BEFORE INSERT/UPDATE` que consulta `company.type`.** Rejeitada: regra procedural escondida, mais uma trigger por tabela e um caminho de erro diferente das FKs.
3. **Tabelas separadas `market` e `supplier`.** Rejeitada: duplica endereço, CNPJ único, assinatura e usuários, e a companhia deixaria de ser uma entidade só para planos e auditoria.
4. **Só validação no código.** Rejeitada pelo RNF-01: um `INSERT` direto ou um bug no use case gravaria um vínculo inválido.

## Argument

As colunas geradas ocupam um byte por linha (`ENUM`) e não aparecem na API nem na auditoria. O erro sai como violação de FK ou de `UNIQUE`, com o nome da constraint, e a aplicação já valida antes para devolver 400 ou 409 com mensagem legível. O banco fica como a última barreira.

## Implications

- As entidades JPA não mapeiam as colunas `*_type` e `active_company_id`: o banco as calcula.
- Os snapshots das triggers de auditoria omitem essas colunas.
- Uma nova tabela que referencie fornecedor ou mercado precisa repetir o padrão: coluna gerada constante e FK composta.
- Trocar de plano exige encerrar a assinatura atual (`ended_at`) antes de abrir a próxima, na mesma transação.

## Related artifacts

- [`../features/010-empresas-planos/plan.md`](../features/010-empresas-planos/plan.md) §1: desenho das migrations V7–V9.
- [ADR 0002](0002-migracao-para-mysql.md): contexto do MySQL 8.4.
