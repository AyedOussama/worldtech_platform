CREATE TABLE app.mission (
    mission_id uuid PRIMARY KEY, account_id uuid NOT NULL, site_id uuid NOT NULL,
    ticket_id uuid, contract_revision_id uuid,
    title varchar(160) NOT NULL CHECK(length(trim(title))>0),
    public_summary varchar(2000) NOT NULL CHECK(length(trim(public_summary))>0),
    window_start timestamptz NOT NULL, window_end timestamptz NOT NULL CHECK(window_end>window_start),
    public_latitude numeric NOT NULL CHECK(public_latitude>=-90 AND public_latitude<=90 AND scale(public_latitude)<=6),
    public_longitude numeric NOT NULL CHECK(public_longitude>=-180 AND public_longitude<=180 AND scale(public_longitude)<=6),
    public_label varchar(200),
    public_location geography(Point,4326) NOT NULL,
    state varchar(16) NOT NULL CHECK(state IN ('DRAFT','PUBLISHED')),
    financial_revision bigint NOT NULL DEFAULT -1 CHECK(financial_revision>=-1),
    version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
    created_by uuid NOT NULL REFERENCES app.app_user(user_id),
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, published_at timestamptz,
    FOREIGN KEY(account_id,site_id) REFERENCES app.site(account_id,site_id),
    FOREIGN KEY(account_id,site_id,ticket_id) REFERENCES app.ticket(account_id,site_id,ticket_id),
    FOREIGN KEY(account_id,contract_revision_id) REFERENCES app.contract_revision(account_id,revision_id),
    UNIQUE(account_id,mission_id), UNIQUE(account_id,site_id,mission_id),
    CHECK((state='DRAFT' AND financial_revision=-1 AND published_at IS NULL)
        OR (state='PUBLISHED' AND financial_revision>=0 AND published_at IS NOT NULL)),
    CHECK(window_start>=timestamptz '2000-01-01 00:00:00+00' AND window_end<=timestamptz '2201-01-01 00:00:00+00'),
    CHECK(ST_SRID(public_location::geometry)=4326 AND GeometryType(public_location::geometry)='POINT')
);
CREATE INDEX mission_site_page_idx ON app.mission(account_id,site_id,created_at,mission_id);
CREATE INDEX mission_site_state_idx ON app.mission(account_id,site_id,state,created_at,mission_id);
CREATE INDEX mission_published_location_gix ON app.mission USING gist(public_location) WHERE state='PUBLISHED';
CREATE TABLE app.mission_skill (
    account_id uuid NOT NULL, mission_id uuid NOT NULL, skill_order smallint NOT NULL CHECK(skill_order>=0 AND skill_order<10),
    skill varchar(80) NOT NULL CHECK(length(trim(skill))>0),
    PRIMARY KEY(mission_id,skill_order), UNIQUE(mission_id,skill),
    FOREIGN KEY(account_id,mission_id) REFERENCES app.mission(account_id,mission_id)
);
CREATE UNIQUE INDEX mission_skill_case_insensitive_idx ON app.mission_skill(mission_id,lower(skill));
CREATE TABLE app.mission_financial_revision (
    account_id uuid NOT NULL, mission_id uuid NOT NULL, revision_number bigint NOT NULL CHECK(revision_number>=0),
    remuneration numeric NOT NULL, expenses numeric,
    currency char(3) NOT NULL DEFAULT 'EUR' CHECK(currency='EUR'), tax_basis varchar(2) NOT NULL DEFAULT 'HT' CHECK(tax_basis='HT'),
    remuneration_basis varchar(5) NOT NULL DEFAULT 'FIXED' CHECK(remuneration_basis='FIXED'),
    expenses_basis varchar(5) NOT NULL DEFAULT 'FIXED' CHECK(expenses_basis='FIXED'),
    reason varchar(2000), author_user_id uuid NOT NULL REFERENCES app.app_user(user_id), created_at timestamptz NOT NULL,
    -- Unconstrained numeric + explicit scale check deliberately prevents silent rounding.
    CHECK(remuneration>=0 AND remuneration<=9999999999.99 AND scale(remuneration)<=2),
    CHECK(expenses IS NULL OR (expenses>=0 AND expenses<=9999999999.99 AND scale(expenses)<=2)),
    PRIMARY KEY(mission_id,revision_number), FOREIGN KEY(account_id,mission_id) REFERENCES app.mission(account_id,mission_id)
);
GRANT SELECT,INSERT ON app.mission,app.mission_skill,app.mission_financial_revision TO maint_app;
GRANT UPDATE(state,financial_revision,version,updated_at,published_at) ON app.mission TO maint_app;

ALTER TABLE app.business_event_outbox DROP CONSTRAINT business_event_outbox_event_kind_check;
ALTER TABLE app.business_event_outbox ADD CONSTRAINT business_event_outbox_event_kind_check
    CHECK(event_kind IN ('CONTRACT_REVISED','TICKET_CREATED','TICKET_CHANGED','TICKET_MESSAGE','TICKET_INTERNAL_NOTE',
        'MISSION_CREATED','MISSION_PUBLISHED','MISSION_REVISED'));
