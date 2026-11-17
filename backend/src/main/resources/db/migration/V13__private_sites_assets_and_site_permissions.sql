CREATE TABLE app.site (
    site_id uuid PRIMARY KEY,
    account_id uuid NOT NULL REFERENCES app.client_account(account_id),
    name varchar(120) NOT NULL CHECK(length(trim(name))>0),
    address varchar(500) NOT NULL CHECK(length(trim(address))>0),
    time_zone varchar(100) NOT NULL,
    contacts jsonb NOT NULL DEFAULT '[]' CHECK(jsonb_typeof(contacts)='array' AND jsonb_array_length(contacts)<=10),
    access_instructions varchar(2000),
    archived boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    UNIQUE(account_id,site_id)
);
CREATE INDEX site_account_page_idx ON app.site(account_id,created_at,site_id) WHERE NOT archived;
CREATE FUNCTION app.valid_asset_attributes(attributes jsonb) RETURNS boolean LANGUAGE sql IMMUTABLE STRICT
SET search_path=pg_catalog AS $$
    SELECT CASE WHEN jsonb_typeof(attributes)<>'object' THEN false ELSE
        (SELECT count(*)<=20 AND coalesce(bool_and(
            key ~ '^[A-Za-z][A-Za-z0-9_-]{0,39}$' AND jsonb_typeof(value)='string'
            AND length(value #>> '{}') BETWEEN 1 AND 200 AND (value #>> '{}') !~ '[[:cntrl:]]'
        ),true) FROM jsonb_each(attributes)) END
$$;
REVOKE ALL ON FUNCTION app.valid_asset_attributes(jsonb) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.valid_asset_attributes(jsonb) TO maint_app;
CREATE TABLE app.equipment (
    equipment_id uuid PRIMARY KEY,
    account_id uuid NOT NULL,
    site_id uuid NOT NULL,
    label varchar(120) NOT NULL CHECK(length(trim(label))>0), category varchar(80) NOT NULL CHECK(length(trim(category))>0),
    internal_reference varchar(100), brand varchar(100), model varchar(100), serial_number varchar(100),
    location varchar(200), condition_description varchar(200),
    attributes jsonb NOT NULL DEFAULT '{}' CHECK(app.valid_asset_attributes(attributes)),
    archived boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    FOREIGN KEY(account_id,site_id) REFERENCES app.site(account_id,site_id),
    UNIQUE(account_id,site_id,equipment_id)
);
CREATE INDEX equipment_scope_page_idx ON app.equipment(account_id,site_id,created_at,equipment_id) WHERE NOT archived;
CREATE TABLE app.site_access_policy (
    member_id uuid PRIMARY KEY,
    account_id uuid NOT NULL,
    all_sites boolean NOT NULL DEFAULT false,
    version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
    updated_at timestamptz NOT NULL,
    FOREIGN KEY(account_id,member_id) REFERENCES app.client_member(account_id,member_id),
    UNIQUE(account_id,member_id)
);
CREATE TABLE app.site_access_grant (
    account_id uuid NOT NULL, member_id uuid NOT NULL, site_id uuid NOT NULL,
    PRIMARY KEY(member_id,site_id),
    FOREIGN KEY(account_id,member_id) REFERENCES app.site_access_policy(account_id,member_id),
    FOREIGN KEY(account_id,site_id) REFERENCES app.site(account_id,site_id)
);
CREATE TABLE app.asset_change (
    event_id uuid PRIMARY KEY, account_id uuid NOT NULL REFERENCES app.client_account(account_id),
    resource_kind varchar(16) NOT NULL CHECK(resource_kind IN ('SITE','EQUIPMENT','SITE_ACCESS')),
    resource_id uuid NOT NULL, revision bigint NOT NULL CHECK(revision>=0),
    actor_user_id uuid NOT NULL REFERENCES app.app_user(user_id),
    action varchar(24) NOT NULL CHECK(action IN ('CREATED','UPDATED','ARCHIVED','ACCESS_REPLACED')),
    all_sites boolean, site_ids uuid[], occurred_at timestamptz NOT NULL,
    UNIQUE(resource_kind,resource_id,revision)
);
GRANT SELECT,INSERT,UPDATE ON app.site,app.equipment,app.site_access_policy TO maint_app;
GRANT SELECT,INSERT,DELETE ON app.site_access_grant TO maint_app;
GRANT SELECT,INSERT ON app.asset_change TO maint_app;
