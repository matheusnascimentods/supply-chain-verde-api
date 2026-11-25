# Débitos do controle de carbono

Problemas encontrados no cálculo e no uso das emissões de CO₂ (API e web). Ordem: impacto no número, depois correções pequenas.

> Tratados na feature [`019-calculo-de-carbono`](spec.md). A spec cita cada item como D-n; o que ficou fora do escopo está no §7 de lá.

## Resumo

Na prática, o CO₂ exibido no ranking, nos relatórios, no dashboard e na página pública é **distância do frete × 0,15**, e só existe quando alguém marcou a caixa de cálculo na etapa `TRANSPORT`. As outras etapas valem 0 ou não têm emissão (itens 1, 2, 4 e 5). Antes de mexer em fator ou metodologia, é preciso decidir o que gera emissão em cada etapa e se o cálculo é obrigatório.

## Como funciona hoje

1. O web registra a etapa, depois o transporte (só na etapa `TRANSPORT`) e, se a caixa "Calcular emissão desta etapa" estiver marcada, chama `POST /stages/{chainId}/emission` com a metodologia.
2. `CalculateCarbonEmissionUseCase` faz `co2Kg = distância do transporte × 0,15` e grava fator, CO₂ e metodologia em `carbon_emission`.
3. Pegada do lote, dashboard, relatório (`sp_generate_sustainability_report`), ranking (`vw_supplier_sustainability`) e catálogo (`vw_supplier_product_emission`) somam `carbon_emission` via `vw_stage_emission`.

## Problemas

### 1. Só a etapa de transporte pode ter CO₂ diferente de zero

O web só envia transporte quando a etapa é `TRANSPORT` (`stage-modal/index.component.ts:104` e `:144`). Para as outras cinco etapas (`PRODUCTION`, `STORAGE`, `PROCESSING`, `DISTRIBUTION`, `RETAIL`), o cálculo sempre grava 0 kg. Produção e processamento, que costumam ser as maiores fontes, nunca entram na pegada.

### 2. Modo de transporte e combustível são ignorados

`transport_mode` e `fuel_type` estão no banco, mas o fator é sempre 0,15 kg CO₂/km (`CalculateCarbonEmissionUseCase`). Um trecho aéreo com querosene e um ferroviário elétrico de mesma distância dão o mesmo CO₂. A capacidade também não entra, então o cálculo não é por tonelada-km.

### 3. A metodologia escolhida não muda o resultado

`DEFRA`, `GHG_PROTOCOL`, `IPCC` e `EMEP_EEA` são só gravados como rótulo. O modal pede a escolha como se ela mudasse o cálculo. Resolver junto com o item 2 (fator por metodologia, modo e combustível).

### 4. Etapa sem transporte grava emissão 0 em vez de recusar

Sem `transport`, a distância vira `BigDecimal.ZERO` e a emissão é salva com 0 kg. Isso reduz o CO₂ por unidade (`vw_supplier_product_emission`) e aumenta o score do fornecedor. Deveria retornar 422, ou a etapa deveria ficar explicitamente "sem emissão".

### 5. O cálculo é opcional sem regra que o justifique

A spec base (`docs/features/000-base/spec.md`, §6.3) desenha o fluxo como obrigatório: toda etapa passa por "Calcular emissão". O web tornou o cálculo opcional (caixa desmarcada por padrão) e nenhuma spec, ADR ou commit explica por quê. O motivo provável é histórico: até a RN-10 (`017-ciclo-de-vida-lote`), o `SUPPLIER` não podia calcular, e a caixa permitia registrar a etapa sem cálculo. Etapas sem emissão também existem por correção de dados (`007-ordem-etapas`, fora de escopo). Consequência: lotes com etapas sem `carbon_emission` somam menos CO₂ sem que ninguém perceba.

### 6. "Calcular em outro momento" não existe

O hint do modal diz "Você pode registrar a etapa agora e calcular a emissão em outro momento", mas o único chamador de `calculateEmission` no web é `AdvanceStageUseCase`, dentro do próprio registro da etapa. Depois de salva, a etapa sem emissão não tem como ser calculada pela interface.

### 7. Recalcular a mesma etapa estoura a constraint

`carbon_emission` tem `UNIQUE (chain_id)`. Uma segunda chamada a `POST /stages/{id}/emission` cai na violação de constraint em vez de devolver 409 com mensagem clara (ou de recalcular, se for desejado).

### 8. CO₂ ou CO₂e: a unidade é ambígua

A coluna se chama `co2_kg` e o fator é tratado como CO₂, mas a página pública mostra "kg CO₂e" (CO₂ equivalente, que inclui CH₄ e N₂O convertidos pelo GWP, o potencial de aquecimento global). As metodologias oferecidas publicam fatores em CO₂e, e os valores mudam conforme a versão do GWP (AR4, AR5 ou AR6 do IPCC). Falta definir a unidade e gravar a fonte do fator (metodologia, ano ou versão da tabela).

### 9. Somas misturam metodologias diferentes

Pegada do lote, relatório, dashboard e ranking somam emissões sem olhar `calculation_method`. Quando a metodologia passar a mudar o fator (item 3), dois fornecedores calculados por métodos diferentes deixam de ser comparáveis no ranking. Opções: fixar uma metodologia por plataforma ou mercado, ou só comparar emissões do mesmo método.

### 10. Score do ranking penaliza volume, não eficiência

`vw_supplier_sustainability` desconta `CO₂ total / 100`, então um fornecedor grande perde para um pequeno mesmo emitindo menos por unidade. `vw_supplier_product_emission` já tem CO₂ por unidade e seria uma base mais justa.

### 11. Código morto e documentação desatualizada

- `domain/service/CarbonFootprintCalculator` não tem uso; `GetBatchCarbonFootprintUseCase` repete a mesma soma. Usar o calculador ou apagá-lo.
- `supply-chain-verde.md` cita `SustainabilityScoreCalculator`, que não existe; o score só é calculado na view SQL.

### 12. Texto de permissão errado no web

`stage-modal/index.component.html` diz "O cálculo de emissão está disponível para perfis de gestor e administrador", mas `SUPPLIER` também pode calcular (RN-10). O texto só aparece para `AUDITOR`, então deveria dizer que auditores não calculam.
