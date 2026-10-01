DROP TABLE audit_log;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'supply_chain_audit_owner') THEN
        CREATE ROLE supply_chain_audit_owner NOLOGIN;
    END IF;
END;
$$;

ALTER ROLE supply_chain_audit_owner NOLOGIN;

CREATE TABLE audit_log (
    log_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action audit_action NOT NULL,
    affected_table VARCHAR(120) NOT NULL,
    affected_entity_id BIGINT NOT NULL,
    before_data JSONB,
    after_data JSONB,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_user
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_log_user_id ON audit_log (user_id);
CREATE INDEX idx_audit_log_performed_at ON audit_log (performed_at DESC, log_id DESC);
CREATE INDEX idx_audit_log_action ON audit_log (action);
CREATE INDEX idx_audit_log_affected_entity ON audit_log (affected_table, affected_entity_id);

ALTER TABLE audit_log OWNER TO supply_chain_audit_owner;
ALTER SEQUENCE audit_log_log_id_seq OWNER TO supply_chain_audit_owner;
REVOKE ALL ON TABLE audit_log FROM PUBLIC;
GRANT SELECT ON TABLE audit_log TO CURRENT_USER;

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
BEGIN
    actor_setting := NULLIF(current_setting('app.user_id', TRUE), '');
    IF actor_setting IS NOT NULL THEN
        IF actor_setting !~ '^[0-9]+$' THEN
            RAISE EXCEPTION 'Invalid app.user_id audit context';
        END IF;
        actor_id := actor_setting::BIGINT;
    END IF;

    IF TG_OP = 'INSERT' THEN
        new_row := to_jsonb(NEW) - ARRAY[
            'password', 'password_hash', 'token', 'access_token', 'refresh_token',
            'secret', 'client_secret', 'authorization', 'credential', 'credentials'
        ];
        entity_id := (new_row ->> TG_ARGV[0])::BIGINT;
        after_snapshot := new_row;
        action_value := 'INSERT';
    ELSIF TG_OP = 'DELETE' THEN
        old_row := to_jsonb(OLD) - ARRAY[
            'password', 'password_hash', 'token', 'access_token', 'refresh_token',
            'secret', 'client_secret', 'authorization', 'credential', 'credentials'
        ];
        entity_id := (old_row ->> TG_ARGV[0])::BIGINT;
        before_snapshot := old_row;
        action_value := 'DELETE';
    ELSE
        old_row := to_jsonb(OLD) - ARRAY[
            'password', 'password_hash', 'token', 'access_token', 'refresh_token',
            'secret', 'client_secret', 'authorization', 'credential', 'credentials'
        ];
        new_row := to_jsonb(NEW) - ARRAY[
            'password', 'password_hash', 'token', 'access_token', 'refresh_token',
            'secret', 'client_secret', 'authorization', 'credential', 'credentials'
        ];
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
        before_data, after_data, performed_at
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

ALTER FUNCTION capture_audit_log() OWNER TO supply_chain_audit_owner;
GRANT USAGE ON SCHEMA public TO supply_chain_audit_owner;
GRANT INSERT ON TABLE audit_log TO supply_chain_audit_owner;
GRANT USAGE, SELECT ON SEQUENCE audit_log_log_id_seq TO supply_chain_audit_owner;
REVOKE EXECUTE ON FUNCTION capture_audit_log() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION capture_audit_log() TO CURRENT_USER;

CREATE TRIGGER audit_address AFTER INSERT OR UPDATE OR DELETE ON address
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('address_id');
CREATE TRIGGER audit_users AFTER INSERT OR UPDATE OR DELETE ON users
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('user_id');
CREATE TRIGGER audit_supplier AFTER INSERT OR UPDATE OR DELETE ON supplier
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('supplier_id');
CREATE TRIGGER audit_certification AFTER INSERT OR UPDATE OR DELETE ON certification
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('certification_id', 'status');
CREATE TRIGGER audit_product AFTER INSERT OR UPDATE OR DELETE ON product
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('product_id');
CREATE TRIGGER audit_batch AFTER INSERT OR UPDATE OR DELETE ON batch
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('batch_id');
CREATE TRIGGER audit_chain AFTER INSERT OR UPDATE OR DELETE ON chain
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('chain_id', 'stage_type');
CREATE TRIGGER audit_transport AFTER INSERT OR UPDATE OR DELETE ON transport
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('transport_id');
CREATE TRIGGER audit_carbon_emission AFTER INSERT OR UPDATE OR DELETE ON carbon_emission
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('emission_id');
CREATE TRIGGER audit_report AFTER INSERT OR UPDATE OR DELETE ON report
FOR EACH ROW EXECUTE FUNCTION capture_audit_log('report_id');
