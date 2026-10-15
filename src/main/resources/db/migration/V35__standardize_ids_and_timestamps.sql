-- Views dependem de carbon_emission.calculated_at, cujo tipo muda abaixo.
DROP VIEW vw_monthly_emission;
DROP VIEW vw_supplier_product_emission;
DROP VIEW vw_stage_emission;

-- Sem triggers de auditoria durante o backfill; voltam no fim com a PK 'id'.
DROP TRIGGER audit_address ON address;
DROP TRIGGER audit_users ON users;
DROP TRIGGER audit_supplier ON supplier;
DROP TRIGGER audit_certification ON certification;
DROP TRIGGER audit_product ON product;
DROP TRIGGER audit_batch ON batch;
DROP TRIGGER audit_chain ON chain;
DROP TRIGGER audit_transport ON transport;
DROP TRIGGER audit_carbon_emission ON carbon_emission;
DROP TRIGGER audit_report ON report;

ALTER TABLE address RENAME COLUMN address_id TO id;
ALTER TABLE users RENAME COLUMN user_id TO id;
ALTER TABLE supplier RENAME COLUMN supplier_id TO id;
ALTER TABLE certification RENAME COLUMN certification_id TO id;
ALTER TABLE product RENAME COLUMN product_id TO id;
ALTER TABLE batch RENAME COLUMN batch_id TO id;
ALTER TABLE chain RENAME COLUMN chain_id TO id;
ALTER TABLE transport RENAME COLUMN transport_id TO id;
ALTER TABLE carbon_emission RENAME COLUMN emission_id TO id;
ALTER TABLE report RENAME COLUMN report_id TO id;
ALTER TABLE audit_log RENAME COLUMN log_id TO id;

-- created_at: colunas existentes são horário/data de São Paulo.
ALTER TABLE supplier RENAME COLUMN registered_at TO created_at;
ALTER TABLE report RENAME COLUMN generated_at TO created_at;
ALTER TABLE audit_log RENAME COLUMN performed_at TO created_at;
ALTER TABLE carbon_emission RENAME COLUMN calculated_at TO created_at;

ALTER TABLE users ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'America/Sao_Paulo',
    ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE supplier ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at::timestamp AT TIME ZONE 'America/Sao_Paulo',
    ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE report ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'America/Sao_Paulo',
    ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE audit_log ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'America/Sao_Paulo',
    ALTER COLUMN created_at SET DEFAULT now();
ALTER TABLE carbon_emission ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at::timestamp AT TIME ZONE 'America/Sao_Paulo',
    ALTER COLUMN created_at SET DEFAULT now();

ALTER TABLE address ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE product ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE transport ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE certification ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE batch ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE chain ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- Onde há data de negócio, ela é a melhor aproximação da criação.
UPDATE certification SET created_at = issued_at::timestamp AT TIME ZONE 'America/Sao_Paulo';
UPDATE batch SET created_at = produced_at::timestamp AT TIME ZONE 'America/Sao_Paulo';
UPDATE chain SET created_at = started_at AT TIME ZONE 'America/Sao_Paulo';

CREATE FUNCTION set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW IS DISTINCT FROM OLD THEN
        NEW.updated_at := now();
    END IF;
    RETURN NEW;
END;
$$;

DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['address', 'users', 'supplier', 'certification', 'product', 'batch',
                             'chain', 'transport', 'carbon_emission', 'report'] LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now()', t);
        EXECUTE format('UPDATE %I SET updated_at = created_at', t);
        EXECUTE format('CREATE TRIGGER set_updated_at_%s BEFORE UPDATE ON %I '
                       'FOR EACH ROW EXECUTE FUNCTION set_updated_at()', t, t);
    END LOOP;
END;
$$;

-- Igual à V30, gravando em created_at (era performed_at) e sem as colunas de busca, removidas na V34.
CREATE OR REPLACE FUNCTION capture_audit_log()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = pg_catalog, public
AS $$
DECLARE
    old_row JSONB;
    new_row JSONB;
    before_snapshot JSONB;
    after_snapshot JSONB;
    actor_setting TEXT;
    actor_id BIGINT;
    entity_id BIGINT;
    changed_key TEXT;
    state_column TEXT;
    action_value audit_action;
    has_changes BOOLEAN := FALSE;
    excluded_columns TEXT[] := ARRAY[
        'password', 'password_hash', 'token', 'access_token', 'refresh_token',
        'secret', 'client_secret', 'authorization', 'credential', 'credentials'
    ];
BEGIN
    actor_setting := NULLIF(current_setting('app.user_id', TRUE), '');
    IF actor_setting IS NOT NULL THEN
        IF actor_setting !~ '^[0-9]+$' THEN
            RAISE EXCEPTION 'Invalid app.user_id audit context';
        END IF;
        actor_id := actor_setting::BIGINT;
    END IF;

    IF TG_OP = 'INSERT' THEN
        new_row := to_jsonb(NEW) - excluded_columns;
        entity_id := (new_row ->> TG_ARGV[0])::BIGINT;
        after_snapshot := new_row;
        action_value := 'INSERT';
    ELSIF TG_OP = 'DELETE' THEN
        old_row := to_jsonb(OLD) - excluded_columns;
        entity_id := (old_row ->> TG_ARGV[0])::BIGINT;
        before_snapshot := old_row;
        action_value := 'DELETE';
    ELSE
        old_row := to_jsonb(OLD) - excluded_columns;
        new_row := to_jsonb(NEW) - excluded_columns;
        entity_id := (new_row ->> TG_ARGV[0])::BIGINT;
        before_snapshot := '{}'::JSONB;
        after_snapshot := '{}'::JSONB;
        action_value := 'UPDATE';

        FOR changed_key IN SELECT jsonb_object_keys(old_row || new_row) LOOP
            IF old_row -> changed_key IS DISTINCT FROM new_row -> changed_key THEN
                has_changes := TRUE;
                before_snapshot := before_snapshot || jsonb_build_object(changed_key, old_row -> changed_key);
                after_snapshot := after_snapshot || jsonb_build_object(changed_key, new_row -> changed_key);
            END IF;
        END LOOP;

        IF NOT has_changes THEN
            RETURN NEW;
        END IF;

        IF COALESCE(TG_ARGV[1], '') <> '' THEN
            FOREACH state_column IN ARRAY string_to_array(TG_ARGV[1], ',') LOOP
                IF old_row -> state_column IS DISTINCT FROM new_row -> state_column THEN
                    action_value := 'STATUS_CHANGE';
                    EXIT;
                END IF;
            END LOOP;
        END IF;
    END IF;

    INSERT INTO public.audit_log (
        user_id, action, affected_table, affected_entity_id,
        before_data, after_data, created_at
    ) VALUES (
        actor_id, action_value, TG_TABLE_NAME, entity_id,
        before_snapshot, after_snapshot, clock_timestamp()
    );

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER audit_address AFTER INSERT OR UPDATE OR DELETE ON address
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('id');
CREATE TRIGGER audit_users AFTER INSERT OR UPDATE OR DELETE ON users
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('id');
CREATE TRIGGER audit_supplier AFTER INSERT OR UPDATE OR DELETE ON supplier
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('id');
CREATE TRIGGER audit_certification AFTER INSERT OR UPDATE OR DELETE ON certification
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('id', 'status');
CREATE TRIGGER audit_product AFTER INSERT OR UPDATE OR DELETE ON product
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('id');
CREATE TRIGGER audit_batch AFTER INSERT OR UPDATE OR DELETE ON batch
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('id');
CREATE TRIGGER audit_chain AFTER INSERT OR UPDATE OR DELETE ON chain
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('id', 'stage_type');
CREATE TRIGGER audit_transport AFTER INSERT OR UPDATE OR DELETE ON transport
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('id');
CREATE TRIGGER audit_carbon_emission AFTER INSERT OR UPDATE OR DELETE ON carbon_emission
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('id');
CREATE TRIGGER audit_report AFTER INSERT OR UPDATE OR DELETE ON report
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('id');

-- Histórico de auditoria com os nomes novos. Só a PK da própria tabela muda;
-- FKs dentro do snapshot (ex.: supplier_id num batch) ficam.
DO $$
DECLARE
    m TEXT[];
BEGIN
    FOREACH m SLICE 1 IN ARRAY ARRAY[
        ['address', 'address_id', 'id'], ['users', 'user_id', 'id'], ['supplier', 'supplier_id', 'id'],
        ['certification', 'certification_id', 'id'], ['product', 'product_id', 'id'], ['batch', 'batch_id', 'id'],
        ['chain', 'chain_id', 'id'], ['transport', 'transport_id', 'id'], ['carbon_emission', 'emission_id', 'id'],
        ['report', 'report_id', 'id'], ['supplier', 'registered_at', 'created_at'],
        ['report', 'generated_at', 'created_at'], ['carbon_emission', 'calculated_at', 'created_at']
    ] LOOP
        UPDATE audit_log
        SET before_data = CASE WHEN before_data ? m[2]
                THEN (before_data - m[2]) || jsonb_build_object(m[3], before_data -> m[2]) ELSE before_data END,
            after_data = CASE WHEN after_data ? m[2]
                THEN (after_data - m[2]) || jsonb_build_object(m[3], after_data -> m[2]) ELSE after_data END
        WHERE affected_table = m[1] AND (before_data ? m[2] OR after_data ? m[2]);
    END LOOP;
END;
$$;

-- Mesmas colunas de saída de antes; calculated_at é o dia da emissão em São Paulo.
CREATE VIEW vw_stage_emission AS
SELECT b.id        AS batch_id,
       b.supplier_id,
       p.id        AS product_id,
       p.name      AS product_name,
       p.category,
       p.unit,
       b.quantity,
       c.id        AS chain_id,
       c.stage_type,
       ce.co2_kg,
       ce.emission_factor,
       ce.calculation_method,
       CAST(ce.created_at AT TIME ZONE 'America/Sao_Paulo' AS DATE) AS calculated_at
FROM batch b
         JOIN product p          ON p.id = b.product_id
         JOIN chain c            ON c.batch_id = b.id
         JOIN carbon_emission ce ON ce.chain_id = c.id;

CREATE VIEW vw_supplier_product_emission AS
SELECT supplier_id,
       product_id,
       category,
       unit,
       COUNT(*)                          AS batch_count,
       SUM(batch_co2_kg)                 AS total_co2_kg,
       SUM(quantity)                     AS total_quantity,
       SUM(batch_co2_kg) / SUM(quantity) AS co2_kg_per_unit
FROM (
         SELECT batch_id, supplier_id, product_id, category, unit, quantity, SUM(co2_kg) AS batch_co2_kg
         FROM vw_stage_emission
         GROUP BY batch_id, supplier_id, product_id, category, unit, quantity
     ) per_batch
GROUP BY supplier_id, product_id, category, unit;

CREATE VIEW vw_monthly_emission AS
SELECT supplier_id,
       CAST(date_trunc('month', calculated_at) AS DATE) AS month,
       SUM(co2_kg)                                      AS co2_kg
FROM vw_stage_emission
GROUP BY supplier_id, CAST(date_trunc('month', calculated_at) AS DATE);
