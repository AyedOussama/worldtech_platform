-- Lot 06 is additive. Revisions, their coverage, calendars and objectives are immutable to runtime.
CREATE TABLE app.maintenance_contract (
    contract_id uuid PRIMARY KEY,
    account_id uuid NOT NULL REFERENCES app.client_account(account_id),
    version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    UNIQUE(account_id,contract_id)
);
CREATE TABLE app.contract_revision (
    revision_id uuid PRIMARY KEY, account_id uuid NOT NULL, contract_id uuid NOT NULL,
    revision_number bigint NOT NULL CHECK(revision_number>=0),
    public_name varchar(160) NOT NULL CHECK(length(trim(public_name))>0), internal_notes varchar(2000),
    valid_from timestamptz NOT NULL, valid_until timestamptz NOT NULL,
    calendar_mode varchar(8) NOT NULL CHECK(calendar_mode IN ('ALWAYS','WEEKLY')),
    reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
    author_user_id uuid NOT NULL REFERENCES app.app_user(user_id), created_at timestamptz NOT NULL,
    CHECK(valid_from>=timestamptz '2000-01-01 00:00:00+00' AND valid_until<=timestamptz '2201-01-01 00:00:00+00' AND valid_until>valid_from),
    FOREIGN KEY(account_id,contract_id) REFERENCES app.maintenance_contract(account_id,contract_id),
    UNIQUE(account_id,revision_id), UNIQUE(contract_id,revision_number)
);
ALTER TABLE app.maintenance_contract ADD CONSTRAINT maintenance_contract_current_revision_fk
    FOREIGN KEY(contract_id,version) REFERENCES app.contract_revision(contract_id,revision_number) DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX maintenance_contract_page_idx ON app.maintenance_contract(account_id,created_at,contract_id);
CREATE TABLE app.contract_site_coverage (
    account_id uuid NOT NULL, revision_id uuid NOT NULL, site_id uuid NOT NULL,
    coverage_mode varchar(12) NOT NULL CHECK(coverage_mode IN ('SITE','EQUIPMENT')),
    PRIMARY KEY(revision_id,site_id), UNIQUE(account_id,revision_id,site_id,coverage_mode),
    FOREIGN KEY(account_id,revision_id) REFERENCES app.contract_revision(account_id,revision_id),
    FOREIGN KEY(account_id,site_id) REFERENCES app.site(account_id,site_id)
);
CREATE INDEX contract_coverage_site_idx ON app.contract_site_coverage(account_id,site_id,revision_id);
CREATE TABLE app.contract_equipment_coverage (
    account_id uuid NOT NULL, revision_id uuid NOT NULL, site_id uuid NOT NULL, equipment_id uuid NOT NULL,
    coverage_mode varchar(12) NOT NULL DEFAULT 'EQUIPMENT' CHECK(coverage_mode='EQUIPMENT'),
    PRIMARY KEY(revision_id,equipment_id),
    FOREIGN KEY(account_id,revision_id,site_id,coverage_mode) REFERENCES app.contract_site_coverage(account_id,revision_id,site_id,coverage_mode),
    FOREIGN KEY(account_id,site_id,equipment_id) REFERENCES app.equipment(account_id,site_id,equipment_id)
);
CREATE TABLE app.contract_weekly_window (
    account_id uuid NOT NULL, revision_id uuid NOT NULL, day_of_week smallint NOT NULL CHECK(day_of_week BETWEEN 1 AND 7),
    start_minute smallint NOT NULL, end_minute smallint NOT NULL,
    CHECK(start_minute>=0 AND start_minute<end_minute AND end_minute<=1440),
    PRIMARY KEY(revision_id,day_of_week,start_minute),
    FOREIGN KEY(account_id,revision_id) REFERENCES app.contract_revision(account_id,revision_id)
);
CREATE TABLE app.contract_day_override (
    account_id uuid NOT NULL, revision_id uuid NOT NULL, service_date date NOT NULL CHECK(service_date BETWEEN date '2000-01-01' AND date '2200-12-31'),
    PRIMARY KEY(revision_id,service_date), UNIQUE(account_id,revision_id,service_date),
    FOREIGN KEY(account_id,revision_id) REFERENCES app.contract_revision(account_id,revision_id)
);
CREATE TABLE app.contract_override_window (
    account_id uuid NOT NULL, revision_id uuid NOT NULL, service_date date NOT NULL,
    start_minute smallint NOT NULL, end_minute smallint NOT NULL,
    CHECK(start_minute>=0 AND start_minute<end_minute AND end_minute<=1440),
    PRIMARY KEY(revision_id,service_date,start_minute),
    FOREIGN KEY(account_id,revision_id,service_date) REFERENCES app.contract_day_override(account_id,revision_id,service_date)
);
CREATE TABLE app.contract_sla_rule (
    account_id uuid NOT NULL, revision_id uuid NOT NULL,
    priority varchar(8) NOT NULL CHECK(priority IN ('LOW','NORMAL','HIGH','CRITICAL')),
    metric varchar(20) NOT NULL CHECK(metric IN ('ACKNOWLEDGEMENT','INTERVENTION_START','RESOLUTION')),
    target_minutes integer NOT NULL CHECK(target_minutes BETWEEN 1 AND 525600),
    pause_while_waiting_client boolean NOT NULL,
    PRIMARY KEY(revision_id,priority,metric),
    FOREIGN KEY(account_id,revision_id) REFERENCES app.contract_revision(account_id,revision_id)
);
GRANT SELECT,INSERT ON app.maintenance_contract,app.contract_revision,app.contract_site_coverage,app.contract_equipment_coverage,
    app.contract_weekly_window,app.contract_day_override,app.contract_override_window,app.contract_sla_rule TO maint_app;
GRANT UPDATE(version,updated_at) ON app.maintenance_contract TO maint_app;
