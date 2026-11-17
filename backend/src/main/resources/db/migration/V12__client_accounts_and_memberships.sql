ALTER TABLE app.client_onboarding ADD CONSTRAINT client_dossier_account_source_unique UNIQUE(dossier_id,user_id,account_type,state);
CREATE TABLE app.client_account (
    account_id uuid PRIMARY KEY,
    dossier_id uuid NOT NULL UNIQUE,
    founder_user_id uuid NOT NULL,
    account_type varchar(16) NOT NULL CHECK(account_type IN ('BUSINESS','INDIVIDUAL')),
    source_state varchar(24) NOT NULL DEFAULT 'APPROVED' CHECK(source_state='APPROVED'),
    display_name varchar(240) NOT NULL CHECK(length(trim(display_name))>0),
    version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    FOREIGN KEY(dossier_id,founder_user_id,account_type,source_state) REFERENCES app.client_onboarding(dossier_id,user_id,account_type,state),
    UNIQUE(account_id,account_type,founder_user_id)
);
CREATE TABLE app.client_member (
    member_id uuid PRIMARY KEY,
    account_id uuid NOT NULL,
    account_type varchar(16) NOT NULL,
    founder_user_id uuid NOT NULL,
    user_id uuid NOT NULL REFERENCES app.app_user(user_id),
    active boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    FOREIGN KEY(account_id,account_type,founder_user_id) REFERENCES app.client_account(account_id,account_type,founder_user_id),
    CHECK(account_type='BUSINESS' OR user_id=founder_user_id),
    UNIQUE(account_id,member_id)
);
CREATE UNIQUE INDEX client_member_current_idx ON app.client_member(account_id,user_id) WHERE active;
CREATE INDEX client_member_user_scope_idx ON app.client_member(user_id,account_id) WHERE active;
CREATE INDEX client_account_page_idx ON app.client_account(created_at,account_id);
CREATE TABLE app.client_account_change (
    event_id uuid PRIMARY KEY, account_id uuid NOT NULL REFERENCES app.client_account(account_id),
    resource_kind varchar(16) NOT NULL CHECK(resource_kind IN ('ACCOUNT','MEMBER')),
    resource_id uuid NOT NULL, revision bigint NOT NULL CHECK(revision>=0),
    actor_user_id uuid NOT NULL REFERENCES app.app_user(user_id),
    action varchar(24) NOT NULL CHECK(action IN ('CREATED','RENAMED','MEMBER_ADDED','MEMBER_REMOVED')),
    occurred_at timestamptz NOT NULL,
    UNIQUE(resource_kind,resource_id,revision)
);
GRANT SELECT,INSERT,UPDATE ON app.client_account,app.client_member TO maint_app;
GRANT SELECT,INSERT ON app.client_account_change TO maint_app;
