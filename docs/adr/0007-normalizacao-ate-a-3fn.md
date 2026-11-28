# Normalização até a 3FN, com redundâncias controladas pelo banco

*Template: decision record de Jeff Tyree e Art Akerman, “Architecture Decisions: Demystifying Architecture”.*

## Issue

O edital da A3 pede o modelo lógico com normalização aplicada até a 3ª Forma Normal, e a banca avalia "normalização correta, integridade referencial, uso adequado de chaves e constraints". O modelo nasceu quase normalizado e foi corrigido ao longo das migrations, mas nenhum documento mostrava isso. Também não estavam escritas as redundâncias que o schema mantém de propósito.

Esta ADR serve aos dois públicos. A banca encontra a verificação de cada tabela e a evolução do modelo. Quem desenvolve encontra a regra para decidir se um dado novo pode ser gravado ou se tem que ser derivado.

As formas normais usadas aqui:

- **1FN:** todo atributo é atômico e não há grupos repetidos.
- **2FN:** 1FN, e nenhum atributo não chave depende de só uma parte de uma chave candidata composta.
- **3FN:** 2FN, e nenhum atributo não chave depende de outro atributo não chave (sem dependência transitiva).

## Decision

1. **O modelo de negócio fica em 3FN.** Cada fato é gravado num lugar só, e o que pode ser calculado a partir de outros dados é derivado na leitura, por view ou pela aplicação.
2. **Uma redundância só é aceita se o banco garantir que ela não diverge:** coluna gerada (`GENERATED ALWAYS AS`), FK composta ou `CHECK`. Redundância mantida só pela aplicação não entra.
3. **Um valor copiado de propósito é registro histórico, não redundância:** o fator de emissão usado num cálculo e os totais de um relatório. Ele grava o que valia no momento e não deve acompanhar mudanças posteriores.
4. **Domínios fechados são `ENUM`**, e não tabelas de domínio. Um domínio vira tabela quando ganha atributos próprios, como aconteceu com `emission_factor` na V22.

## Status

Decidida. Descreve o schema depois da V23.

## Group

Modelagem de dados.

## Assumptions

- Todas as tabelas têm chave primária substituta (`id BIGINT AUTO_INCREMENT`). As chaves naturais viram `UNIQUE`: `company.cnpj`, `users.email`, `invite.token_hash`, `(chain.batch_id, stage_type)` e as colunas de busca de `emission_factor`.
- `created_at` e `updated_at` existem em todas as tabelas e dependem só do `id`. Ficam de fora da análise.

## Constraints

- **MySQL 8.4:** não tem tipo enumerado criado pelo usuário (`CREATE TYPE`), só a coluna `ENUM`. Não tem `CHECK` com subconsulta, então regras entre linhas ficam em FK composta.
- **Histórico das migrations:** uma migration aplicada não é editada. As correções do modelo entram como migrations novas, e por isso a evolução abaixo é a história real do schema.

## Positions

**Domínios fechados (status, perfil, etapa, modal, combustível…)**

1. **Coluna `ENUM`.** Escolhida. Os valores são atômicos e não têm atributos próprios, então nenhuma forma normal é violada. O `ENUM` espelha o enum do domínio Java, e um valor fora da lista é recusado pelo próprio banco.
2. **Tabela de domínio com FK.** Rejeitada enquanto o domínio for só uma lista de nomes: acrescentaria um JOIN em toda leitura sem guardar nenhum fato a mais.

**Snapshots `before_data` e `after_data` em `audit_log` (JSON)**

1. **Valor atômico.** Escolhida. O snapshot é um documento opaco: é gravado e lido inteiro, e nenhuma consulta filtra ou junta por um campo dele. Para o modelo, é um valor só.
2. **Uma tabela de histórico por entidade auditada.** Rejeitada: duplicaria o schema de negócio, e cada mudança de coluna exigiria mudar também a tabela de auditoria. Ver as [ADRs 0001](0001-auditoria-no-postgresql.md) e [0002](0002-migracao-para-mysql.md).

**Totais do relatório (`report.total_co2_kg`, `batch_count`, `tracked_product_count`)**

1. **Gravar o retrato no momento da geração.** Escolhida. O relatório é um documento emitido para a certificadora. Se uma emissão do período for corrigida depois, o relatório já emitido continua igual, e quem precisar do número corrigido gera um relatório novo.
2. **Calcular na leitura, por view.** Rejeitada: o mesmo relatório mostraria números diferentes a cada consulta, e o que a certificadora recebeu deixaria de ser reproduzível.

## Argument

### Evolução do modelo

O schema não partiu de uma tabela única. Ele já começou separado por entidade, e as migrations removeram as dependências que sobravam.

| Migration | O que mudou | Forma normal |
|---|---|---|
| V1 | Uma tabela por entidade: `address`, `users`, `supplier`, `product`, `batch`, `chain`, `transport`, `carbon_emission`, `certification`, `report`. Endereço em tabela própria, referenciado por FK | 1FN a 3FN no ponto de partida |
| V7 a V9 | `supplier` vira `company`, com `type` `MARKET` ou `SUPPLIER`. O vínculo 1:1 `supplier.user_id` some, e o usuário passa a apontar para a companhia (`users.company_id`). O dado de quem é fornecedor fica num lugar só | Remove a duplicação de dados de pessoa jurídica entre fornecedor e mercado |
| V18 | Saem `plan` e `company_plan`. O teto de usuários fica em `company.max_users` (V16), porque toda assinatura é igual | Remove uma tabela que só guardava uma constante repetida |
| V21 | Sai `chain.origin_address_id`. A origem de uma etapa é o destino da etapa anterior (ou o endereço do fornecedor, na `PRODUCTION`), então era uma dependência transitiva gravada. Agora ela é derivada na leitura ([feature 018](../features/018-enderecos-etapas/spec.md)) | 3FN: elimina um dado derivável que podia divergir |
| V22 | Os fatores de emissão saem do código e viram a tabela `emission_factor`, com chave `(calculation_method, transport_mode, fuel_type, energy_source)` ([feature 019](../features/019-calculo-de-carbono/spec.md)) | O fator passa a ser um fato com atributos próprios (`unit`, `source`, `reference_year`) |

### Verificação por tabela

Como toda chave primária é um `id` simples, a 2FN em relação à PK vale automaticamente: não existe parte de chave da qual depender. A tabela abaixo verifica as chaves candidatas compostas e as dependências entre atributos não chave.

| Tabela | Chaves candidatas | Dependências funcionais | Situação |
|---|---|---|---|
| `address` | `id` | `id → street, number, neighborhood, complement, zip_code, city, state` | 3FN, com a ressalva do CEP (abaixo) |
| `company` | `id`, `cnpj` | `id → name, cnpj, address_id, phone, email, type, max_users` | 3FN. `(id, type)` é `UNIQUE` só para servir de alvo de FK composta ([ADR 0003](0003-tipo-de-companhia-por-fk-composta.md)) |
| `users` | `id`, `email` | `id → name, email, password, role, company_id, is_active` | 3FN |
| `invite` | `id`, `token_hash` | `id → invited_by, invited_for, role, token_hash, accepted, created_at`; `created_at → expires_at` | 3FN no que é gravado. `expires_at` é coluna gerada |
| `product` | `id` | `id → supplier_id, name, category, unit, description` | 3FN |
| `certification` | `id` | `id → supplier_id, certification, issuing_body, issued_at, expires_at, status` | 3FN. Ver o status abaixo |
| `batch` | `id` | `id → product_id, supplier_id, market_id, status, rejection_reason, quantity, produced_at`; `product_id → supplier_id` | Redundância controlada (abaixo) |
| `chain` | `id`, `(batch_id, stage_type)` | `id → batch_id, stage_type, destination_address_id, responsible_user_id, started_at, ended_at`; `stage_type → previous_stage_type` | 3FN no que é gravado. `previous_stage_type` é coluna gerada |
| `transport` | `id`, `chain_id` | `chain_id → transport_mode, distance, fuel_type, capacity` | 3FN. Relação 1:1 com a etapa |
| `carbon_emission` | `id`, `chain_id` | `chain_id → calculation_method, emission_factor, cargo_weight_kg, energy_source, consumption, co2_kg` | Registro histórico (abaixo) |
| `emission_factor` | `id`, `(calculation_method, transport_mode, fuel_type, energy_source)` | A chave composta inteira `→ value, unit, source, reference_year` | 3FN. Nenhum atributo depende de só parte da chave |
| `report` | `id` | `id → supplier_id, market_id, period_start_at, period_end_at, total_co2_kg, batch_count, tracked_product_count` | Registro histórico (Positions) |
| `audit_log` | `id` | `id → user_id, action, affected_table, affected_entity_id, before_data, after_data` | 3FN, com JSON como valor atômico (Positions) |

### Redundâncias e como o banco as controla

**`batch.supplier_id`, que repete `product.supplier_id`.** É uma dependência transitiva (`batch.id → product_id → supplier_id`). Fica gravada porque é ela que permite à FK composta `fk_batch_supplier (supplier_id, supplier_type) → company (id, type)` garantir que o lote é de uma companhia `SUPPLIER`. A FK `fk_batch_product_supplier (product_id, supplier_id) → product (id, supplier_id)` impede que o valor diverja do produto. Ver a [ADR 0003](0003-tipo-de-companhia-por-fk-composta.md).

**Colunas `*_type` constantes** (`batch`, `certification`, `product`, `report`). São colunas geradas com valor fixo (`'SUPPLIER'` ou `'MARKET'`) e só existem para completar as FKs compostas. Não guardam nenhum fato.

**`chain.previous_stage_type`.** Depende só de `stage_type`: é transitiva em relação ao `id` e parcial em relação à chave candidata `(batch_id, stage_type)`. É gerada pelo MySQL, então não pode ser escrita nem divergir. Ela existe para que a FK `fk_chain_previous_stage (batch_id, previous_stage_type) → chain (batch_id, stage_type)` imponha a ordem das etapas sem trigger.

**`invite.expires_at`.** Gerada como `created_at + 24h`.

**`carbon_emission.emission_factor` e `co2_kg`.** O fator vem de `emission_factor`, e `co2_kg` é o fator multiplicado pela atividade. Os dois ficam gravados porque representam o cálculo como foi feito. Se a tabela de fatores for atualizada (uma nova edição do DEFRA, ou uma nova mistura de biodiesel), as emissões já registradas não mudam. É o mesmo princípio do relatório.

**`certification.status` e `batch.status`.** Parte dos valores poderia ser derivada: `EXPIRED` vem de `expires_at`, e `COMPLETED` vem da existência da etapa `RETAIL`. Mas o status também guarda estados que nenhum outro dado deduz: `SUSPENDED` e `UNDER_REVIEW` na certificação, e `PENDING`, `REJECTED` e `CANCELED` no lote. É uma máquina de estados, e não uma cópia de outro atributo. O `EXPIRED` é aplicado todo dia pela procedure `sp_expire_certifications`, disparada pelo event `ev_expire_certifications`.

**`address.zip_code → city, state`.** Em tese, o CEP determina cidade e UF, o que seria uma dependência transitiva. Ela é aceita porque essa relação não é confiável como regra de dados: existem CEPs gerais de cidade, CEPs que mudam e localidades com o mesmo CEP. Uma tabela de CEPs exigiria manter a base dos Correios. O endereço é guardado como foi informado.

## Implications

- **Regra para colunas novas:** se o valor pode ser calculado a partir de outras colunas, ele é derivado na leitura (view ou aplicação). Só é gravado se for (a) coluna gerada, (b) protegido por FK composta ou `CHECK`, ou (c) registro histórico que não deve acompanhar mudanças. Quem gravar uma redundância registra aqui a justificativa.
- **Leituras com JOIN:** a origem da etapa, o score do fornecedor e as emissões agregadas são calculados a cada consulta pelas views `vw_stage_emission`, `vw_supplier_sustainability`, `vw_monthly_emission` e `vw_supplier_product_emission`. Isso custa JOINs, mas o volume atual não pede views materializadas ([feature 006](../features/006-views-procedures/spec.md)).
- **Novo valor num `ENUM`:** exige `ALTER TABLE` numa migration e a mudança no enum Java correspondente.
- **Relatórios e emissões não se recalculam sozinhos.** Corrigir um fator ou uma etapa não altera o que já foi emitido.

## Related artifacts

- [`../features/005-padronizacao-schema/schema.puml`](../features/005-padronizacao-schema/schema.puml) e o diagrama ER no [`README.md`](../../README.md).
- [ADR 0003](0003-tipo-de-companhia-por-fk-composta.md): FKs compostas por tipo de companhia.
- [`../features/018-enderecos-etapas/spec.md`](../features/018-enderecos-etapas/spec.md): origem derivada da etapa.
- [`../features/019-calculo-de-carbono/spec.md`](../features/019-calculo-de-carbono/spec.md): tabela `emission_factor`.
- `src/main/resources/db/migration/`: V1, V7 a V9, V18, V21 e V22.
