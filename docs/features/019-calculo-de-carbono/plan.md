# Plano técnico: Cálculo de carbono

> Os requisitos estão em [`spec.md`](spec.md) e o passo a passo em [`tasks.md`](tasks.md).

## 1. Correções pequenas (Task 1)

- `CalculateCarbonEmissionUseCase`: antes de calcular, `if (carbonEmissionRepository.findByChainId(chainId).isPresent()) throw new ConflictException("A emissão da etapa " + chainId + " já foi calculada.")` (RN-12). O `ConflictException` já vira 409 no `GlobalExceptionHandler`. Hoje a constraint `carbon_emission_chain_id_key` cai no handler genérico e vira 500.
- `GetBatchCarbonFootprintUseCase`: troca o `reduce` repetido por `new CarbonFootprintCalculator().calculateTotal(emissions)`. O calculador é um domain service sem estado e sem bean, e esse passa a ser o único uso dele (D-11).
- `supply-chain-verde.md` (visão geral, fora dos repositórios): o score é calculado na `vw_supplier_sustainability`, e o `SustainabilityScoreCalculator` não existe.

## 2. Fatores (Task 2)

### Tabela

```sql
CREATE TABLE emission_factor (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    calculation_method ENUM('DEFRA', 'GHG_PROTOCOL') COLLATE utf8mb4_0900_as_ci NOT NULL,
    transport_mode     ENUM('ROAD', 'RAIL', 'MARITIME', 'AIR') COLLATE utf8mb4_0900_as_ci NULL,
    fuel_type          ENUM('DIESEL', 'BIODIESEL', 'ELECTRIC', 'GASOLINE', 'ETHANOL', 'AVIATION_KEROSENE', 'HEAVY_FUEL_OIL')
                       COLLATE utf8mb4_0900_as_ci NULL,
    energy_source      ENUM('ELECTRICITY', 'DIESEL', 'LPG', 'NATURAL_GAS') COLLATE utf8mb4_0900_as_ci NULL,
    value              DECIMAL(19,8) NOT NULL,
    unit               VARCHAR(20)   NOT NULL,
    source             VARCHAR(255)  NOT NULL,
    reference_year     SMALLINT      NOT NULL,
    -- Deslocamento (modo + combustível) ou etapa fixa (fonte), nunca os dois.
    CONSTRAINT chk_emission_factor_activity CHECK (
        (transport_mode IS NOT NULL AND fuel_type IS NOT NULL AND energy_source IS NULL)
        OR (transport_mode IS NULL AND fuel_type IS NULL AND energy_source IS NOT NULL)),
    CONSTRAINT chk_emission_factor_value_non_negative CHECK (value >= 0),
    CONSTRAINT uq_emission_factor UNIQUE (calculation_method, transport_mode, fuel_type, energy_source)
);
```

No MySQL, o `UNIQUE` com `NULL` não impede duplicatas. Ele vale como documentação. Quem garante a unicidade é a migration, que é a única que escreve nessa tabela (RN-02). O app user recebe só `SELECT`.

### Valores

Todos os valores são kg CO₂e por unidade, de queima direta (sem a parte do poço ao tanque, o "WTT"). Fontes conferidas célula a célula nos arquivos oficiais:

- **DEFRA:** *UK Government GHG Conversion Factors for Company Reporting 2026* (DESNZ), flat file revisado v1.2, aba "Factors by Category". O ID da linha está na coluna "Localização".
- **GHG_PROTOCOL:** Ferramenta do Programa Brasileiro GHG Protocol (FGV), v2026.0.1, aba "Fatores de Emissão", e MCTI/SIRENE para eletricidade. A ferramenta publica fatores **por gás**. O CO₂e aqui é `CO₂ + 28·CH₄ + 265·N₂O`, o GWP AR5 da própria ferramenta (Seção 6), com o diesel B15 (85% fóssil e 15% biodiesel). O CO₂ biogênico fica de fora, como a ferramenta faz no Escopo 1.

**Deslocamento (kg CO₂e/t·km)**

| Metodologia | Modo | Combustível | Valor | Localização |
|---|---|---|---|---|
| DEFRA | ROAD | DIESEL | 0.10356 | Freighting goods > HGV (non-refrigerated, all diesel) > Average laden (27_304_3140_14_1) |
| DEFRA | ROAD | GASOLINE | 0.83810 | Freighting goods > Vans > Average (up to 3.5 t) > Petrol (27_303_3103_14_1); só existe para vans |
| DEFRA | ROAD | ELECTRIC | 0.14699 | Freighting goods > Vans > Average (up to 3.5 t) > Battery Electric Vehicle (27_303_3108_14_1); só para vans e com a rede britânica |
| DEFRA | RAIL | DIESEL | 0.02583 | Freighting goods > Rail > Freight train (27_315_3151_14_1); média da frota britânica |
| DEFRA | MARITIME | HEAVY_FUEL_OIL | 0.01321 | Freighting goods > Cargo ship > General cargo > Average (27_320_3228_14_1) |
| DEFRA | AIR | AVIATION_KEROSENE | 0.75539 | Freighting goods > Freight flights > Short-haul, to/from UK > Without RF (27_317_3155_14_1) |
| GHG_PROTOCOL | ROAD | DIESEL | 0.08967 | Tabela 11, linha 297, "Caminhão - caminhão (média)", B15 |
| GHG_PROTOCOL | ROAD | BIODIESEL | 0.00136 | Tabela 11, linha 297, colunas do B100, sem o CO₂ biogênico |
| GHG_PROTOCOL | RAIL | DIESEL | 0.00967 | Tabela 17, linha 378, "Média nacional" (fator brasileiro) |
| GHG_PROTOCOL | MARITIME | DIESEL | 0.01321 | Tabela 19, linha 450, carga geral (média); é o fator DEFRA reproduzido pela ferramenta |
| GHG_PROTOCOL | AIR | AVIATION_KEROSENE | 0.75539 | Tabela 14, linha 339, de 500 a 3.700 km (DEFRA, sem RF) |

Combinações sem linha (RN-04): `BIODIESEL`, `ETHANOL` e as outras de `RAIL`/`MARITIME` na DEFRA; `GASOLINE`, `ETHANOL`, `ELECTRIC`, `RAIL + ELECTRIC` e `MARITIME + HEAVY_FUEL_OIL` no GHG Protocol. Nenhuma das duas fontes publica fator por tonelada-km para elas.

**Etapa fixa**

| Metodologia | Fonte | Valor | Unidade | Localização |
|---|---|---|---|---|
| DEFRA | DIESEL | 2.58354 | kg CO₂e/L | Fuels > Liquid fuels > Diesel (average biofuel blend) (1_101_1011_8_1) |
| DEFRA | LPG | 2.93936 | kg CO₂e/kg | Fuels > Gaseous fuels > LPG > tonnes (1_100_1003_15_1) ÷ 1000 |
| DEFRA | NATURAL_GAS | 2.02633 | kg CO₂e/m³ | Fuels > Gaseous fuels > Natural gas > cubic metres (1_100_1004_1_1) |
| GHG_PROTOCOL | ELECTRICITY | 0.04610 | kg CO₂/kWh | MCTI, `Inventario_2026_janout.xlsx`, aba "inventário-todos", O99, média do SIN em 2025. **Só CO₂**: o MCTI não publica CH₄ e N₂O |
| GHG_PROTOCOL | DIESEL | 2.24482 | kg CO₂e/L | Tabela 1, linha 61 (setor Manufatura), B15 |
| GHG_PROTOCOL | LPG | 2.93346 | kg CO₂e/kg | Tabela 1, linha 51 (Manufatura) ÷ 1000 |
| GHG_PROTOCOL | NATURAL_GAS | 2.06895 | kg CO₂e/m³ | Tabela 1, linha 52 (Manufatura) |

A DEFRA não publica fator de eletricidade do Brasil, então `ELECTRICITY` só aceita GHG Protocol.

Simplificações deliberadas, comentadas na migration com `-- ponytail:`:
- Aéreo usa uma faixa só, de 500 a 3.700 km, que cobre os voos domésticos do Brasil. Faixas por distância entram se aparecer frete internacional.
- Combustível estacionário no GHG Protocol usa o CH₄ do setor Manufatura. A diferença para o setor Comercial (armazém e varejo) é de 0,3%.
- A mistura B15 é a de agosto de 2025 em diante. A média anual de 2025 foi 14,42%.

Atualizar os valores (fator do SIN todo ano, edição nova da DEFRA) é uma migration nova com `UPDATE`. As emissões já calculadas guardam o fator da época (RN-09).

### Resto da `V22`

```sql
-- RN-17: legado para GHG_PROTOCOL, depois o enum sem IPCC e EMEP_EEA.
UPDATE carbon_emission SET calculation_method = 'GHG_PROTOCOL' WHERE calculation_method IN ('IPCC', 'EMEP_EEA');
ALTER TABLE carbon_emission
    MODIFY calculation_method ENUM('DEFRA', 'GHG_PROTOCOL') COLLATE utf8mb4_0900_as_ci NOT NULL,
    ADD COLUMN cargo_weight_kg DECIMAL(19,4) NULL AFTER co2_kg,
    ADD COLUMN energy_source ENUM('ELECTRICITY', 'DIESEL', 'LPG', 'NATURAL_GAS') COLLATE utf8mb4_0900_as_ci NULL AFTER cargo_weight_kg,
    ADD COLUMN consumption DECIMAL(19,4) NULL AFTER energy_source,
    ADD CONSTRAINT chk_carbon_emission_cargo_weight_positive CHECK (cargo_weight_kg IS NULL OR cargo_weight_kg > 0),
    ADD CONSTRAINT chk_carbon_emission_consumption_non_negative CHECK (consumption IS NULL OR consumption >= 0);

-- RN-18: peso dos transportes antigos de produto em KG ou TON.
UPDATE carbon_emission ce
JOIN chain c ON c.id = ce.chain_id
JOIN batch b ON b.id = c.batch_id
JOIN product p ON p.id = b.product_id
SET ce.cargo_weight_kg = CASE p.unit WHEN 'KG' THEN b.quantity WHEN 'TON' THEN b.quantity * 1000 END
WHERE c.stage_type = 'TRANSPORT' AND p.unit IN ('KG', 'TON');
```

As três triggers `audit_carbon_emission_*` são recriadas (`DROP TRIGGER` + `CREATE`, com o mesmo `DEFINER` da `V5`) com as três colunas novas no JSON. Sem isso, o dado da atividade não entra na auditoria. O `UPDATE` do legado roda **antes** do drop das triggers, para que a troca de metodologia fique auditada (RN-17).

Java:
- `CalculationMethod { DEFRA, GHG_PROTOCOL }`.
- O enum novo `EnergySource { ELECTRICITY, DIESEL, LPG, NATURAL_GAS }`.
- `CarbonEmission` e `CarbonEmissionJpaEntity` ganham `cargoWeightKg`, `energySource` e `consumption`. O `CarbonEmissionJpaMapper` é MapStruct e mapeia os campos de mesmo nome sem mudança. Conferir no build.

## 3. Consulta dos fatores (Task 3)

Segue o padrão das outras portas do domínio:

```java
public interface EmissionFactorRepository {
    List<Entry> findAll();
    Optional<Entry> findTransport(CalculationMethod method, TransportMode mode, FuelType fuel);
    Optional<Entry> findEnergy(CalculationMethod method, EnergySource source);

    record Entry(CalculationMethod calculationMethod, TransportMode transportMode, FuelType fuelType,
                 EnergySource energySource, BigDecimal value, String unit, String source, int referenceYear) {}
}
```

- O `record` aninhado segue o mesmo padrão do `CarbonEmissionRepository.MonthlyEmission`. O `EmissionFactor` do domínio continua sendo o value object do valor.
- `EmissionFactorJpaEntity` (somente leitura, `@Immutable`), `EmissionFactorJpaRepository` (Spring Data, com `findByCalculationMethodAndTransportModeAndFuelType` e `findByCalculationMethodAndEnergySource`) e `EmissionFactorRepositoryImpl`.
- `ListEmissionFactorsUseCase` → `List<EmissionFactorResponseDTO>`, com os mesmos campos do `Entry`.
- `CarbonEmissionController`: `GET /emission-factors`. O `anyRequest().authenticated()` do `SecurityConfig` já cobre a rota (RN-05).

## 4. Cálculo por atividade (Task 4)

```java
public record CarbonEmissionRequestDTO(CalculationMethod calculationMethod, BigDecimal cargoWeightKg,
                                       EnergySource energySource, BigDecimal consumption) {}
```

`StageType` ganha `isTransport()`, que devolve `this == TRANSPORT || this == DISTRIBUTION`.

`CalculateCarbonEmissionUseCase.execute`, depois das validações de hoje e do 409 da Task 1:

```java
if (request.calculationMethod() == null) throw new ValidationException("Metodologia é obrigatória.");
CarbonEmission.CarbonEmissionBuilder emission = CarbonEmission.builder().chain(chain).calculationMethod(request.calculationMethod());
BigDecimal factor, co2Kg;
if (chain.getStageType().isTransport()) {
    Transport transport = transportRepository.findByChainId(chainId)
            .orElseThrow(() -> new ValidationException("Registre o transporte da etapa antes de calcular a emissão."));
    BigDecimal weightKg = cargoWeightKg(chain.getBatch(), request.cargoWeightKg());  // RN-07
    factor = factors.findTransport(request.calculationMethod(), transport.getTransportMode(), transport.getFuelType())
            .orElseThrow(() -> missing(request.calculationMethod(), transport))           // RN-04
            .value();
    co2Kg = transport.getDistance().multiply(weightKg).multiply(factor).divide(THOUSAND);
    emission.cargoWeightKg(weightKg);
} else {
    // RN-08: fonte e consumo obrigatórios; consumo 0 é válido.
    ...findEnergy(...)...
    co2Kg = request.consumption().multiply(factor);
    emission.energySource(request.energySource()).consumption(request.consumption());
}
emission.emissionFactor(new EmissionFactor(factor)).co2Kg(co2Kg.setScale(4, RoundingMode.HALF_UP));
```

- `cargoWeightKg(batch, informado)`: `KG` → `quantity`; `TON` → `quantity × 1000`; nas outras unidades, o valor informado, que precisa ser maior que 0 (400). A unidade vem de `batch.getProduct().getUnit()` (o `Batch` do domínio já carrega o `Product`).
- As mensagens da RN-04 usam os labels em português de modo e combustível. Um `switch` pequeno no use case, sem enum de labels.
- `co2_kg` é `DECIMAL(19,4)`, por isso o `setScale(4)`.
- `CarbonEmissionResponseDTO` e `CarbonEmissionDtoMapper` ganham os três campos.
- `RegisterTransportUseCase`: `if (!chain.getStageType().isTransport()) throw new ValidationException("Transporte só pode ser registrado em Transporte ou Distribuição.")` (RN-10).

## 5. Emissão obrigatória (Task 5)

`RegisterChainStageUseCase`, logo depois da checagem do dono da etapa (passo 4 do plano da 017):

```java
// RN-11 (019): a etapa anterior precisa da emissão antes da próxima.
if (last != null && carbonEmissionRepository.findByChainId(last.getChainId()).isEmpty()) {
    throw new ConflictException("Calcule a emissão da etapa " + last.getStageType() + " antes de registrar a próxima.");
}
```

Os ITs que registram etapas seguidas (`BatchLifecycleIT`, `TenantIsolationIT` e outros que o `./mvnw verify` apontar) passam a calcular a emissão entre uma etapa e outra. Um helper `registerStageWithEmission` no próprio IT evita repetir o par de chamadas.

## 6. Score por intensidade (Task 6)

`V23__score_por_intensidade.sql`:

```sql
-- RN-14/RN-15 (019): penaliza kg CO2e por tonelada entregue, não o CO2 total.
-- ponytail: K = 10 calibrado com o seed; recalibrar se a distribuição real de intensidade fugir de 0–1500 kg/t.
CREATE OR REPLACE VIEW vw_supplier_sustainability AS
SELECT s.id AS supplier_id,
       COALESCE(c.active_certifications, 0) AS active_certifications,
       COALESCE(e.total_co2_kg, 0) AS total_co2_kg,
       CAST(GREATEST(0, 100 + 10 * COALESCE(c.active_certifications, 0)
                        - ROUND(COALESCE(i.co2_kg_per_ton, 0) / 10, 2)) AS DECIMAL(10,2)) AS sustainability_score
FROM company s
LEFT JOIN (SELECT supplier_id, COUNT(*) AS active_certifications
           FROM certification WHERE status = 'ACTIVE' GROUP BY supplier_id) c ON c.supplier_id = s.id
LEFT JOIN (SELECT supplier_id, SUM(co2_kg) AS total_co2_kg
           FROM vw_stage_emission GROUP BY supplier_id) e ON e.supplier_id = s.id
LEFT JOIN (SELECT per_batch.supplier_id, SUM(per_batch.co2_kg) / SUM(w.cargo_weight_kg / 1000) AS co2_kg_per_ton
           FROM (SELECT batch_id, supplier_id, SUM(co2_kg) AS co2_kg FROM vw_stage_emission GROUP BY batch_id, supplier_id) per_batch
           JOIN (SELECT ch.batch_id, ce.cargo_weight_kg
                 FROM chain ch JOIN carbon_emission ce ON ce.chain_id = ch.id
                 WHERE ch.stage_type = 'TRANSPORT' AND ce.cargo_weight_kg IS NOT NULL) w ON w.batch_id = per_batch.batch_id
           GROUP BY per_batch.supplier_id) i ON i.supplier_id = s.id
WHERE s.type = 'SUPPLIER';
```

- As colunas de saída são as mesmas de hoje, então `CompanyJpaRepository.findScoresByIds` e `findIdsRankedByScore` não mudam. O `GRANT SELECT` da view continua valendo, porque o `CREATE OR REPLACE` mantém o nome.
- Calibrar o `K` antes do commit: rodar a view no compose com o seed e conferir que os scores continuam espalhados (nem todos 0, nem todos acima de 100).
- Docs: o `000-base/reference.md` e o `supply-chain-verde.md` passam a descrever o score novo.

## 7. Testes

- `CalculateCarbonEmissionUseCaseTest`: deslocamento em `KG`, em `TON` e em `UNIT` (com e sem peso); etapa fixa com consumo 0 e com fonte ausente; combinação sem fator (400); emissão repetida (409); transporte ausente (400).
- `GetBatchCarbonFootprintUseCaseTest` e o `CarbonFootprintCalculatorTest` existente.
- `RegisterChainStageUseCaseTest`: etapa anterior sem emissão devolve 409.
- `RegisterTransportUseCaseTest`: transporte em `STORAGE` devolve 400.
- `EmissionFactorsIT`: `GET /emission-factors` com login (200, 18 linhas) e sem login (401).
- `MigrationCarbonIT` (ou o `ViewsAndProceduresIT`): depois da `V22`, nenhuma emissão com `IPCC` ou `EMEP_EEA`, a soma de `co2_kg` igual à do seed e `cargo_weight_kg` preenchido nos transportes antigos em `KG`/`TON`. Depois da `V23`, dois fornecedores montados no teste ficam na ordem da intensidade.
