# Especificação — Ordem obrigatória das etapas

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Complementa as [views e procedures](../006-views-procedures/spec.md) no entregável #4 da A3 (constraints e integridade referencial).

## 1. Problema

- `RegisterChainStageUseCase` grava qualquer `stage_type` em qualquer ordem: dá para registrar `RETAIL` num lote sem `PRODUCTION`, ou repetir uma etapa.
- `InvalidStageTransitionException` existe, mas nunca é lançada.
- Nos dados atuais, os lotes pulam etapas (`PRODUCTION>PROCESSING`: 15 lotes) e alguns começam no meio (`PROCESSING`: 5, `DISTRIBUTION`: 1). A rastreabilidade não garante que o produto passou pela cadeia inteira.

## 2. Objetivo

Toda cadeia segue a mesma sequência, garantida em duas camadas: o banco recusa qualquer gravação fora de ordem (constraints) e a API valida antes e devolve uma mensagem clara (state machine).

## 3. Regras

| ID | Regra |
|---|---|
| RN-01 | Sequência estrita: `PRODUCTION → STORAGE → PROCESSING → TRANSPORT → DISTRIBUTION → RETAIL` (ordem do enum `stage_type`). |
| RN-02 | Todo lote começa em `PRODUCTION`. Cada nova etapa exige que a imediatamente anterior já exista no mesmo lote. |
| RN-03 | Um lote não repete etapa. |
| RN-04 | Uma etapa que já tem sucessora não pode ser apagada. |
| RN-05 | Os lotes existentes são corrigidos por migration: as etapas que faltam antes da etapa mais avançada do lote são inseridas, sem apagar nada. |

## 4. Requisitos

| ID | Requisito |
|---|---|
| RF-01 | Constraints em `chain`: `UNIQUE (batch_id, stage_type)` e FK composta de `(batch_id, previous_stage_type)` para `chain (batch_id, stage_type)`. |
| RF-02 | `RegisterChainStageUseCase` só aceita a próxima etapa da sequência e lança `InvalidStageTransitionException` nos outros casos. |
| RF-03 | Violação das constraints que escapar da validação (duas requisições ao mesmo tempo) responde com o mesmo status de `InvalidStageTransitionException`, não 500. |
| RF-04 | A migration corrige os dados existentes antes de criar as constraints; as etapas inseridas ficam no `audit_log`. |

## 5. Fora de escopo

- Validar que o `started_at` de uma etapa é posterior ao da anterior (regra entre linhas, exigiria trigger).
- Etapas opcionais ou transições configuráveis (tabela `stage_transition`).
- Emissão e transporte para as etapas inseridas pela correção: elas entram sem `carbon_emission` nem `transport`.
