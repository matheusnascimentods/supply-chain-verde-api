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
        'secret', 'client_secret', 'authorization', 'credential', 'credentials',
        'search_vector', 'name_search'
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

UPDATE audit_log
SET before_data = before_data - 'search_vector' - 'name_search',
    after_data = after_data - 'search_vector' - 'name_search'
WHERE before_data ?| ARRAY['search_vector', 'name_search']
   OR after_data ?| ARRAY['search_vector', 'name_search'];
