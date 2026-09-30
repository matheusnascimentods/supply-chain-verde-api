CREATE TEMPORARY TABLE demo_batches (
    batch_id BIGINT PRIMARY KEY,
    supplier_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    produced_at DATE NOT NULL,
    seed_number BIGINT NOT NULL
) ON COMMIT DROP;

WITH demo_products AS (
    SELECT product_id, row_number() OVER (ORDER BY product_id) AS seed_number
    FROM (SELECT product_id FROM product ORDER BY product_id DESC LIMIT 60) newest
),
demo_suppliers AS (
    SELECT supplier_id, row_number() OVER (ORDER BY supplier_id) AS seed_number
    FROM (SELECT supplier_id FROM supplier ORDER BY supplier_id DESC LIMIT 41) newest
),
batch_seed AS (
    SELECT products.product_id,
           suppliers.supplier_id,
           products.seed_number,
           (CURRENT_DATE - ((products.seed_number * 3) % 150)::integer) AS produced_at
    FROM demo_products products
    JOIN demo_suppliers suppliers
      ON suppliers.seed_number = ((products.seed_number - 1) % 41) + 1
),
inserted_batches AS (
    INSERT INTO batch (product_id, supplier_id, quantity, produced_at)
    SELECT product_id,
           supplier_id,
           250 + seed_number * 37,
           produced_at
    FROM batch_seed
    RETURNING batch_id, product_id, supplier_id, produced_at
)
INSERT INTO demo_batches (batch_id, supplier_id, product_id, produced_at, seed_number)
SELECT inserted.batch_id, inserted.supplier_id, inserted.product_id, inserted.produced_at, seed.seed_number
FROM inserted_batches inserted
JOIN batch_seed seed USING (product_id, supplier_id, produced_at);

CREATE TEMPORARY TABLE demo_chains (
    chain_id BIGINT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    supplier_id BIGINT NOT NULL,
    stage_number INTEGER NOT NULL,
    stage_type stage_type NOT NULL,
    started_at TIMESTAMP NOT NULL
) ON COMMIT DROP;

WITH stage_seed AS (
    SELECT batches.batch_id,
           batches.supplier_id,
           batches.seed_number,
           stage.number AS stage_number,
           (ARRAY['PRODUCTION', 'PROCESSING', 'TRANSPORT', 'RETAIL']::stage_type[])[stage.number] AS stage_type,
           (batches.produced_at::timestamp + (stage.number * INTERVAL '1 day')) AS started_at
    FROM demo_batches batches
    CROSS JOIN LATERAL generate_series(
        1,
        CASE (batches.seed_number % 4)
            WHEN 0 THEN 1
            WHEN 1 THEN 2
            WHEN 2 THEN 3
            ELSE 4
        END
    ) AS stage(number)
),
inserted_chains AS (
    INSERT INTO chain (
        batch_id,
        origin_address_id,
        destination_address_id,
        responsible_user_id,
        stage_type,
        started_at,
        ended_at
    )
    SELECT seed.batch_id,
           NULL,
           NULL,
           (SELECT user_id FROM users WHERE email = CASE
               WHEN seed.stage_type = 'PRODUCTION' THEN 'khvicha.kvaratskhelia@psg.example.com'
               WHEN seed.stage_type = 'PROCESSING' THEN 'ousmane.dembele@psg.example.com'
               WHEN seed.stage_type = 'TRANSPORT' THEN 'desire.doue@psg.example.com'
               ELSE 'marquinhos@psg.example.com'
           END),
           seed.stage_type,
           seed.started_at,
           seed.started_at + INTERVAL '8 hours'
    FROM stage_seed seed
    RETURNING chain_id, batch_id, stage_type, started_at
)
INSERT INTO demo_chains (chain_id, batch_id, supplier_id, stage_number, stage_type, started_at)
SELECT inserted.chain_id,
       inserted.batch_id,
       batches.supplier_id,
       seed.stage_number,
       inserted.stage_type,
       inserted.started_at
FROM inserted_chains inserted
JOIN demo_batches batches USING (batch_id)
JOIN stage_seed seed USING (batch_id, stage_type, started_at);

INSERT INTO transport (chain_id, transport_mode, distance, fuel_type, capacity)
SELECT chains.chain_id,
       (ARRAY['ROAD', 'RAIL', 'MARITIME', 'AIR']::transport_mode[])[((chains.chain_id % 4) + 1)::integer],
       35 + (chains.chain_id % 900),
       (ARRAY['BIODIESEL', 'ELECTRIC', 'ETHANOL', 'DIESEL']::fuel_type[])[((chains.chain_id % 4) + 1)::integer],
       8000 + (chains.chain_id % 6) * 5000
FROM demo_chains chains
WHERE chains.stage_type = 'TRANSPORT';

INSERT INTO carbon_emission (chain_id, emission_factor, co2_kg, calculation_method, calculated_at)
SELECT chains.chain_id,
       CASE chains.stage_type
           WHEN 'PRODUCTION' THEN 0.01500000
           WHEN 'PROCESSING' THEN 0.03200000
           WHEN 'TRANSPORT' THEN 0.06800000
           ELSE 0.00900000
       END,
       round((chains.chain_id % 37 + 1) * CASE chains.stage_type
           WHEN 'PRODUCTION' THEN 0.42
           WHEN 'PROCESSING' THEN 0.68
           WHEN 'TRANSPORT' THEN 1.25
           ELSE 0.21
       END, 4),
       (ARRAY['DEFRA', 'GHG_PROTOCOL', 'IPCC', 'EMEP_EEA']::calculation_method[])[((chains.chain_id % 4) + 1)::integer],
       CASE WHEN chains.chain_id % 4 = 0
           THEN date_trunc('month', CURRENT_DATE)::date
           ELSE chains.started_at::date
       END
FROM demo_chains chains;

WITH new_suppliers AS (
    SELECT supplier_id, row_number() OVER (ORDER BY supplier_id) AS seed_number
    FROM (SELECT supplier_id FROM supplier ORDER BY supplier_id DESC LIMIT 30) newest
),
certification_seed AS (
    SELECT suppliers.supplier_id,
           suppliers.seed_number,
           cert.number AS certification_number,
           (ARRAY['Orgânico Brasil', 'ISO 14001', 'Programa de Rastreabilidade']::text[])[cert.number] AS certification,
           CASE cert.number
               WHEN 1 THEN CURRENT_DATE - 365
               WHEN 2 THEN CURRENT_DATE - 350
               ELSE CURRENT_DATE - 100
           END AS issued_at,
           CASE cert.number
               WHEN 1 THEN CURRENT_DATE - 1
               WHEN 2 THEN CURRENT_DATE + 15
               ELSE CURRENT_DATE + 730
           END AS expires_at,
           CASE cert.number
               WHEN 1 THEN 'EXPIRED'
               WHEN 2 THEN (ARRAY['ACTIVE', 'UNDER_REVIEW', 'SUSPENDED']::text[])[((suppliers.seed_number % 3) + 1)::integer]
               ELSE 'ACTIVE'
           END AS status
    FROM new_suppliers suppliers
    CROSS JOIN generate_series(1, 3) AS cert(number)
)
INSERT INTO certification (supplier_id, certification, issuing_body, issued_at, expires_at, status)
SELECT supplier_id,
       certification,
       CASE certification_number
           WHEN 1 THEN 'Instituto Nacional de Produção Orgânica'
           WHEN 2 THEN 'ABNT'
           ELSE 'Instituto Brasileiro de Rastreabilidade'
       END,
       issued_at,
       expires_at,
       status::certification_status
FROM certification_seed;

WITH report_seed AS (
    SELECT suppliers.supplier_id,
           suppliers.seed_number,
           periods.period_number,
           CASE periods.period_number
               WHEN 1 THEN CURRENT_DATE - 150
               WHEN 2 THEN CURRENT_DATE - 60
               ELSE CURRENT_DATE - 150
           END AS period_start_at,
           CASE periods.period_number
               WHEN 1 THEN CURRENT_DATE - 61
               ELSE CURRENT_DATE
           END AS period_end_at
    FROM (
        SELECT supplier_id, row_number() OVER (ORDER BY supplier_id) AS seed_number
        FROM (SELECT supplier_id FROM supplier ORDER BY supplier_id DESC LIMIT 30) newest
    ) suppliers
    CROSS JOIN generate_series(1, 3) AS periods(period_number)
),
report_totals AS (
    SELECT seed.supplier_id,
           seed.period_start_at,
           seed.period_end_at,
           coalesce(sum(emission.co2_kg), 0) AS total_co2_kg,
           count(DISTINCT batch.product_id)::integer AS tracked_product_count
    FROM report_seed seed
    LEFT JOIN demo_batches batch
      ON batch.supplier_id = seed.supplier_id
     AND batch.produced_at BETWEEN seed.period_start_at AND seed.period_end_at
    LEFT JOIN demo_chains chain
      ON chain.batch_id = batch.batch_id
     AND chain.started_at::date BETWEEN seed.period_start_at AND seed.period_end_at
    LEFT JOIN carbon_emission emission
      ON emission.chain_id = chain.chain_id
     AND emission.calculated_at BETWEEN seed.period_start_at AND seed.period_end_at
    GROUP BY seed.supplier_id, seed.period_start_at, seed.period_end_at
)
INSERT INTO report (
    supplier_id,
    period_start_at,
    period_end_at,
    total_co2_kg,
    tracked_product_count,
    generated_at
)
SELECT supplier_id,
       period_start_at,
       period_end_at,
       total_co2_kg,
       tracked_product_count,
       CURRENT_TIMESTAMP
FROM report_totals;

INSERT INTO audit_log (user_id, action, affected_table, performed_at)
SELECT (SELECT user_id FROM users WHERE email = CASE ((event.number - 1) % 4)
            WHEN 0 THEN 'warren.zaire-emery@psg.example.com'
            WHEN 1 THEN 'ousmane.dembele@psg.example.com'
            WHEN 2 THEN 'joao.neves@psg.example.com'
            ELSE 'marquinhos@psg.example.com'
        END),
       (ARRAY['INSERT', 'UPDATE', 'STATUS_CHANGE', 'INSERT']::audit_action[])[((event.number - 1) % 4) + 1],
       (ARRAY['supplier', 'product', 'batch', 'certification', 'chain', 'report']::text[])[((event.number - 1) % 6) + 1],
       CURRENT_TIMESTAMP - (event.number * INTERVAL '3 hours')
FROM generate_series(1, 60) AS event(number);
