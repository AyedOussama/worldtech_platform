-- PostGIS and the app schema are bootstrapped by a privileged operator, not by the runtime.
-- No identity or business tables are introduced before their own lots.
CREATE TABLE app.audit_event (
    event_id uuid PRIMARY KEY,
    occurred_at timestamp with time zone NOT NULL,
    actor_user_id uuid,
    action varchar(100) NOT NULL CHECK (length(trim(action)) > 0),
    resource_type varchar(100) NOT NULL CHECK (length(trim(resource_type)) > 0),
    resource_id uuid,
    trace_id uuid NOT NULL
);

COMMENT ON TABLE app.audit_event IS 'Append-only runtime audit metadata; never credentials or document contents.';
COMMENT ON COLUMN app.audit_event.actor_user_id IS 'Internal UUID reference; identity FK will be introduced with the identity lot.';

REVOKE ALL ON TABLE app.audit_event FROM PUBLIC, maint_app;
GRANT SELECT, INSERT ON TABLE app.audit_event TO maint_app;
-- No default grants: every future migration declares its own runtime privileges.
-- Flyway history remains owned by maint_migrator and inaccessible to maint_app.
