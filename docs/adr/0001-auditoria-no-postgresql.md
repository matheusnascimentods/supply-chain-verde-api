# Auditoria de alterações mantida pelo PostgreSQL

*Template: decision record de Jeff Tyree e Art Akerman, “Architecture Decisions: Demystifying Architecture”.*

## Issue

A auditoria atual é criada pelo `AuditLogInterceptor` da API, que observa operações de repositório. Esse mecanismo só enxerga gravações que passam pelos pontos instrumentados da aplicação e acopla a captura ao backend. A Task 23 também exige registrar entidade e snapshots antes/depois, o que torna a cobertura e a consistência do código de aplicação mais difíceis de manter. A decisão é necessária antes da implementação dessa task para fixar quem captura alterações, como identificar o ator autenticado e o que fazer com o histórico existente.

## Decision

O PostgreSQL será a fonte de captura e persistência dos eventos de auditoria. Triggers nas tabelas de negócio gravarão INSERT, UPDATE, STATUS_CHANGE e DELETE na tabela `audit_log`; a API autentica e propaga o ID do ator no contexto local à transação, e continua responsável por consultar e autorizar `GET /api/v1/audit-logs`. A API não terá interceptor AOP nem fluxo de escrita em `audit_log`.

A tabela substituta começará vazia. Os registros legados não serão copiados nem reconstruídos.

## Status

**Decidida; implementação concluída, implantação pendente.** A migration V29, as triggers, a propagação transacional do ator e o adaptador somente leitura foram implementados e validados em PostgreSQL via Testcontainers. A aplicação em ambientes implantados depende da execução da migration; até lá, esta ADR não afirma que triggers ou `app.user_id` estejam ativos nesses ambientes.

## Group

Dados, integração e segurança.

## Assumptions

- O banco do projeto é PostgreSQL e suas alterações de schema, funções e triggers são versionadas pelo Flyway.
- A API é a fronteira que valida JWTs e conhece o `userId` autenticado.
- A aplicação usa pool de conexões; portanto, identidade de ator não pode ser armazenada em estado de sessão que sobreviva à transação.
- Todas as tabelas de negócio que precisam de auditoria podem receber triggers versionadas.
- A decisão de iniciar a nova tabela vazia é intencional e aceita a perda do histórico atual; não existe backfill confiável dos snapshots antes/depois.
- A role SQL usada pela API não é entregue ao navegador nem a consumidores externos.

## Constraints

- O ID de ator deve vir do principal validado pela API, nunca do body, query string ou parâmetro livre do cliente.
- Definição do ator e mutações devem usar a mesma transação e conexão física. Uma escrita autenticada não pode prosseguir sem contexto identificável do ator.
- O contexto do ator deve expirar ao final da transação para evitar vazamento entre requisições que reutilizam uma conexão.
- A gravação de auditoria é atômica com a mutação de negócio: falha da trigger deve reverter a escrita de negócio.
- A role da API consulta logs, mas não faz INSERT/UPDATE/DELETE direto em `audit_log`.
- Snapshots não podem incluir senhas, hashes, tokens ou outros segredos; relações são representadas por IDs, nunca por grafos serializados.
- O início vazio e o descarte do legado precisam ser explícitos na migration e no release note.

## Positions

1. **Manter captura por AOP na API.** Interceptar repositórios/casos de uso e gravar logs via Java. É a abordagem existente, mas não captura gravações que contornem os pontos instrumentados e deixa a lógica de snapshots/status distribuída na aplicação.
2. **Capturar por triggers no PostgreSQL.** O banco observa mutações nas tabelas cobertas, obtém `OLD`/`NEW` e grava na mesma transação. Exige funções e privilégios SQL bem administrados, além de contexto explícito do ator vindo da API.
3. **Abordagem híbrida com gravação explícita na API.** Casos de uso enviam payloads de auditoria a um serviço/repositório. Mantém lógica e dependência em código de aplicação e aumenta o risco de esquecer operações; não resolve a cobertura de gravações fora desses fluxos.

## Argument

Selecionamos triggers porque a fonte mais próxima da alteração é o banco: `OLD` e `NEW` permitem obter os valores reais antes/depois e a transação garante que evento e mudança persistam ou sejam revertidos juntos. Isso remove o interceptor e reduz duplicação de lógica de captura na API; também cobre operações que chegam ao banco sem atravessar os casos de uso, desde que usem tabelas e triggers cobertas.

O custo total muda para o lado operacional do PostgreSQL: funções, política de exclusão de campos sensíveis, permissões, índices e cobertura das tabelas passam a exigir revisão e versionamento disciplinados. A propagação do ator é uma responsabilidade pequena e explícita da API. Essa troca é adequada porque o projeto já usa PostgreSQL e Flyway. Apagar o histórico legado simplifica o novo formato, mas é uma consequência deliberada e irreversível que deve ser comunicada.

## Implications

### Identidade do ator

Antes da primeira escrita autenticada de cada transação, a API executará `SELECT set_config('app.user_id', :userId, true)` na conexão/transação ativa. As triggers lerão `current_setting('app.user_id', true)`. O argumento `true` limita o valor à transação; não é cache compartilhado nem configuração persistente da sessão. Leituras não definem esse contexto.

Esse valor é uma declaração da API, não uma identidade que o PostgreSQL autentica independentemente. O acesso SQL deve permanecer restrito a serviços confiáveis. Jobs devem usar uma identidade de serviço explícita ou ator nulo, conforme política definida para cada job. Uma escrita autenticada sem ator deve falhar, em vez de herdar identidade anterior ou gravar usuário incorreto.

### Persistência e contrato

`audit_log` terá os campos `log_id`, `user_id`, `action`, `affected_table`, `affected_entity_id BIGINT`, `before_data JSONB`, `after_data JSONB` e `performed_at`, com índices para consulta temporal/ator/ação e para `(affected_table, affected_entity_id)`.

- INSERT: `before_data = NULL`; `after_data` contém os dados auditáveis criados.
- UPDATE e STATUS_CHANGE: os objetos contêm somente campos efetivamente alterados, com os valores de `OLD` e `NEW`.
- DELETE: `before_data` contém os dados auditáveis anteriores; `after_data = NULL`.
- `STATUS_CHANGE` só se aplica quando uma coluna de estado definida para aquela tabela muda; as demais atualizações são `UPDATE`. Updates sem mudanças efetivas não geram evento.
- A trigger remove explicitamente colunas sensíveis conhecidas (senha/hash, tokens, segredo, autorização e credenciais). Como snapshots vêm da linha e FKs são escalares, não são serializados grafos de objetos da aplicação. A lista de exclusão deve acompanhar qualquer novo campo sensível no schema.

As triggers devem cobrir a lista acordada de tabelas de negócio, não auditar a própria `audit_log` e não produzir eventos artificiais para seeds ou DDL. Funções com privilégio elevado devem fixar `search_path` e usar privilégios mínimos. A API mantém filtros, RBAC e paginação da rota de leitura; o frontend permanece consumidor do contrato HTTP.

### Histórico e rollout

A migration de substituição descartará a tabela/histórico atual e criará o novo schema vazio com suas funções e triggers. Não será feito backfill. O changelog deve informar que os registros legados foram descartados. A troca deve ser coordenada com a API para não existir janela de escrita sem contexto/triggers; remover o interceptor somente quando o novo fluxo estiver implantado.

### Validação operacional

A implementação deve validar em PostgreSQL limpo: INSERT, UPDATE sem mudança, UPDATE com mudança, STATUS_CHANGE, DELETE, sanitização, FKs representadas por IDs, falha de trigger revertendo a escrita, rollback, ator nulo/serviço, consulta paginada e ausência de vazamento de ator entre conexões reutilizadas.

## Related decisions

- A Task 23 da API implementa captura, migração e contrato de consulta.
- A Task 24 do frontend formata os snapshots para tela e CSV; a web não define o ator nem conhece triggers.
- A política de identidade para jobs e outras escritas sem usuário humano precisa ser definida antes de habilitar essas rotas/rotinas.

## Related requirements

- API `GET /api/v1/audit-logs`: manter autorização `ADMIN`/`AUDITOR`, filtros inclusivos de data, ação/email e paginação `{ items, limit, offset, hasNext, totalPages }`.
- Cada evento consultável inclui `logId`, `userId`, `userEmail`, `action`, `affectedTable`, `affectedEntityId`, `beforeData`, `afterData` e `performedAt`.
- Eventos de escrita devem ser completos e atômicos, sem registrar segredos ou inventar snapshots.
- A interface deve mostrar deltas legíveis e exportar a mesma descrição no CSV.

## Related artifacts

- [`../spec.md`](../spec.md) — requisitos e contrato da API.
- [`../reference.md`](../reference.md) — rotas, DTOs e exemplo de evento.
- [`../tasks.md`](../tasks.md), Tasks 23 e 24 — plano de implementação backend/frontend.
- [`../plan.md`](../plan.md) — arquitetura do backend.
- [`../../supply-chain-verde-web/docs/spec.md`](../../supply-chain-verde-web/docs/spec.md) e [`../../supply-chain-verde-web/docs/plan.md`](../../supply-chain-verde-web/docs/plan.md) — consumo e apresentação no frontend.

## Related principles

- A identidade é determinada na fronteira autenticada; clientes não escolhem o ator da auditoria.
- Auditoria acompanha atomicamente a mudança que registra.
- Persistir o mínimo necessário: snapshots contêm somente deltas auditáveis e não incluem segredos.
- A API é responsável por autenticação/autorização e consulta; o banco é responsável por capturar e persistir alterações.

## Notes

- O contrato de propagação assume PostgreSQL; outros SGBDs exigiriam mecanismo equivalente e uma decisão específica de escopo transacional.
- A lista implementada inclui address, users, supplier, certification, product, batch, chain, transport, carbon_emission e report; `status` de certification e `stage_type` de chain classificam `STATUS_CHANGE`.
- A migration V29 requer que o usuário executor do Flyway tenha permissão para criar/alterar a role `supply_chain_audit_owner`; a aplicação concede SELECT à role SQL corrente usada durante a migration. Confirmar que essa role corresponde ao principal runtime em cada ambiente.
- A migration destrutiva foi validada em banco limpo no teste de integração; a instalação em ambientes implantados ainda depende do rollout.
