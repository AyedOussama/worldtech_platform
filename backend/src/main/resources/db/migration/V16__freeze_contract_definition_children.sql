-- INSERT-only children must not allow adding coverage/windows/objectives to an old revision.
-- No SECURITY DEFINER and no cross-domain data access; the account/revision FK remains authoritative.
ALTER TABLE app.contract_revision ADD COLUMN definition_transaction xid8 NOT NULL DEFAULT pg_current_xact_id();
CREATE FUNCTION app.stamp_contract_definition_transaction() RETURNS trigger LANGUAGE plpgsql
SET search_path=pg_catalog AS $$
BEGIN
    NEW.definition_transaction := pg_current_xact_id();
    RETURN NEW;
END
$$;
CREATE TRIGGER contract_revision_transaction BEFORE INSERT ON app.contract_revision
    FOR EACH ROW EXECUTE FUNCTION app.stamp_contract_definition_transaction();
CREATE FUNCTION app.require_new_contract_definition() RETURNS trigger LANGUAGE plpgsql
SET search_path=pg_catalog AS $$
BEGIN
    IF NOT EXISTS(SELECT 1 FROM app.contract_revision r WHERE r.account_id=NEW.account_id
        AND r.revision_id=NEW.revision_id AND r.definition_transaction=pg_current_xact_id()) THEN
        RAISE EXCEPTION 'Contract definition is immutable.' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END
$$;
CREATE TRIGGER contract_site_definition_frozen BEFORE INSERT ON app.contract_site_coverage
    FOR EACH ROW EXECUTE FUNCTION app.require_new_contract_definition();
CREATE TRIGGER contract_equipment_definition_frozen BEFORE INSERT ON app.contract_equipment_coverage
    FOR EACH ROW EXECUTE FUNCTION app.require_new_contract_definition();
CREATE TRIGGER contract_weekly_definition_frozen BEFORE INSERT ON app.contract_weekly_window
    FOR EACH ROW EXECUTE FUNCTION app.require_new_contract_definition();
CREATE TRIGGER contract_day_definition_frozen BEFORE INSERT ON app.contract_day_override
    FOR EACH ROW EXECUTE FUNCTION app.require_new_contract_definition();
CREATE TRIGGER contract_override_definition_frozen BEFORE INSERT ON app.contract_override_window
    FOR EACH ROW EXECUTE FUNCTION app.require_new_contract_definition();
CREATE TRIGGER contract_sla_definition_frozen BEFORE INSERT ON app.contract_sla_rule
    FOR EACH ROW EXECUTE FUNCTION app.require_new_contract_definition();
REVOKE ALL ON FUNCTION app.stamp_contract_definition_transaction(),app.require_new_contract_definition() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.stamp_contract_definition_transaction(),app.require_new_contract_definition() TO maint_app;
