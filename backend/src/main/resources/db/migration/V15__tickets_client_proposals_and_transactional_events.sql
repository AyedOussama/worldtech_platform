CREATE TABLE app.ticket (
    ticket_id uuid PRIMARY KEY, account_id uuid NOT NULL, site_id uuid NOT NULL, equipment_id uuid,
    site_time_zone varchar(100) NOT NULL,
    title varchar(160) NOT NULL CHECK(length(trim(title))>0), description varchar(4000) NOT NULL CHECK(length(trim(description))>0),
    requested_priority varchar(8) NOT NULL CHECK(requested_priority IN ('LOW','NORMAL','HIGH','CRITICAL')),
    retained_priority varchar(8) CHECK(retained_priority IN ('LOW','NORMAL','HIGH','CRITICAL')),
    state varchar(16) NOT NULL CHECK(state IN ('OPEN','QUALIFIED','IN_PROGRESS','WAITING_CLIENT','RESOLVED','CLOSED','CANCELLED')),
    qualification_kind varchar(16) CHECK(qualification_kind IN ('COVERED','OUT_OF_CONTRACT')),
    contract_revision_id uuid,
    proposal_number bigint NOT NULL DEFAULT 0 CHECK(proposal_number>=0), version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
    created_by uuid NOT NULL REFERENCES app.app_user(user_id),
    created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    qualified_at timestamptz, started_at timestamptz, resolved_at timestamptz, closed_at timestamptz,
    FOREIGN KEY(account_id,site_id) REFERENCES app.site(account_id,site_id),
    FOREIGN KEY(account_id,site_id,equipment_id) REFERENCES app.equipment(account_id,site_id,equipment_id),
    FOREIGN KEY(account_id,contract_revision_id) REFERENCES app.contract_revision(account_id,revision_id),
    UNIQUE(account_id,ticket_id), UNIQUE(account_id,site_id,ticket_id),
    CHECK((qualified_at IS NULL AND retained_priority IS NULL AND qualification_kind IS NULL AND contract_revision_id IS NULL)
        OR (qualified_at IS NOT NULL AND retained_priority IS NOT NULL AND qualification_kind IS NOT NULL
            AND ((qualification_kind='COVERED' AND contract_revision_id IS NOT NULL) OR (qualification_kind='OUT_OF_CONTRACT' AND contract_revision_id IS NULL)))),
    CHECK(state IN ('OPEN','CANCELLED') OR qualified_at IS NOT NULL),
    CHECK(state<>'OPEN' OR qualified_at IS NULL),
    CHECK(state<>'RESOLVED' OR resolved_at IS NOT NULL), CHECK(state<>'CLOSED' OR (resolved_at IS NOT NULL AND closed_at IS NOT NULL)),
    CHECK(qualified_at IS NULL OR qualified_at>=created_at), CHECK(started_at IS NULL OR started_at>=created_at),
    CHECK(resolved_at IS NULL OR resolved_at>=created_at), CHECK(closed_at IS NULL OR closed_at>=resolved_at)
);
CREATE INDEX ticket_site_page_idx ON app.ticket(account_id,site_id,created_at,ticket_id);
CREATE INDEX ticket_site_state_idx ON app.ticket(account_id,site_id,state,created_at,ticket_id);
CREATE INDEX ticket_equipment_idx ON app.ticket(account_id,site_id,equipment_id) WHERE equipment_id IS NOT NULL;
CREATE TABLE app.client_financial_proposal (
    account_id uuid NOT NULL, ticket_id uuid NOT NULL, proposal_number bigint NOT NULL CHECK(proposal_number>=0),
    remuneration numeric, expenses numeric,
    currency char(3) NOT NULL DEFAULT 'EUR' CHECK(currency='EUR'), tax_basis varchar(2) NOT NULL DEFAULT 'HT' CHECK(tax_basis='HT'),
    remuneration_basis varchar(5) NOT NULL DEFAULT 'FIXED' CHECK(remuneration_basis='FIXED'),
    expenses_basis varchar(5) NOT NULL DEFAULT 'FIXED' CHECK(expenses_basis='FIXED'),
    author_user_id uuid NOT NULL REFERENCES app.app_user(user_id), created_at timestamptz NOT NULL,
    -- Unconstrained numeric + explicit scale check deliberately prevents numeric(12,2)'s silent rounding.
    CHECK(remuneration IS NULL OR (remuneration>=0 AND remuneration<=9999999999.99 AND scale(remuneration)<=2)),
    CHECK(expenses IS NULL OR (expenses>=0 AND expenses<=9999999999.99 AND scale(expenses)<=2)),
    PRIMARY KEY(ticket_id,proposal_number), FOREIGN KEY(account_id,ticket_id) REFERENCES app.ticket(account_id,ticket_id)
);
ALTER TABLE app.ticket ADD CONSTRAINT ticket_current_proposal_fk FOREIGN KEY(ticket_id,proposal_number)
    REFERENCES app.client_financial_proposal(ticket_id,proposal_number) DEFERRABLE INITIALLY DEFERRED;
CREATE TABLE app.ticket_change (
    account_id uuid NOT NULL, site_id uuid NOT NULL, ticket_id uuid NOT NULL, revision bigint NOT NULL CHECK(revision>=0),
    action varchar(24) NOT NULL CHECK(action IN ('CREATED','PROPOSAL_REVISED','QUALIFY','START','WAIT','RESUME','RESOLVE','CLOSE','REOPEN','CANCEL')),
    state varchar(16) NOT NULL CHECK(state IN ('OPEN','QUALIFIED','IN_PROGRESS','WAITING_CLIENT','RESOLVED','CLOSED','CANCELLED')),
    actor_user_id uuid NOT NULL REFERENCES app.app_user(user_id), public_reason varchar(2000), occurred_at timestamptz NOT NULL,
    PRIMARY KEY(ticket_id,revision), FOREIGN KEY(account_id,site_id,ticket_id) REFERENCES app.ticket(account_id,site_id,ticket_id)
);
CREATE TABLE app.ticket_public_message (
    account_id uuid NOT NULL, site_id uuid NOT NULL, ticket_id uuid NOT NULL, message_number bigint NOT NULL CHECK(message_number>=0),
    message_id uuid NOT NULL UNIQUE, author_user_id uuid NOT NULL REFERENCES app.app_user(user_id),
    message_text varchar(4000) NOT NULL CHECK(length(trim(message_text))>0), created_at timestamptz NOT NULL,
    PRIMARY KEY(ticket_id,message_number), FOREIGN KEY(account_id,site_id,ticket_id) REFERENCES app.ticket(account_id,site_id,ticket_id)
);
CREATE TABLE app.ticket_internal_note (
    account_id uuid NOT NULL, site_id uuid NOT NULL, ticket_id uuid NOT NULL, message_number bigint NOT NULL CHECK(message_number>=0),
    message_id uuid NOT NULL UNIQUE, author_user_id uuid NOT NULL REFERENCES app.app_user(user_id),
    message_text varchar(4000) NOT NULL CHECK(length(trim(message_text))>0), created_at timestamptz NOT NULL,
    PRIMARY KEY(ticket_id,message_number), FOREIGN KEY(account_id,site_id,ticket_id) REFERENCES app.ticket(account_id,site_id,ticket_id)
);
GRANT SELECT,INSERT ON app.ticket,app.client_financial_proposal,app.ticket_change,app.ticket_public_message,app.ticket_internal_note TO maint_app;
GRANT UPDATE(state,retained_priority,qualification_kind,contract_revision_id,proposal_number,version,updated_at,qualified_at,started_at,resolved_at,closed_at) ON app.ticket TO maint_app;

-- Backward-compatible extension: onboarding's existing version/state receipts remain unchanged.
ALTER TABLE app.command_receipt ADD COLUMN result_resource_id uuid;

-- Metadata only, written atomically by producers. Delivery/revalidation/retries belong to lot 11.
CREATE TABLE app.business_event_outbox (
    event_id uuid PRIMARY KEY, account_id uuid NOT NULL REFERENCES app.client_account(account_id),
    site_id uuid, resource_id uuid NOT NULL, resource_version bigint NOT NULL CHECK(resource_version>=0),
    event_kind varchar(32) NOT NULL CHECK(event_kind IN ('CONTRACT_REVISED','TICKET_CREATED','TICKET_CHANGED','TICKET_MESSAGE','TICKET_INTERNAL_NOTE')),
    actor_user_id uuid NOT NULL REFERENCES app.app_user(user_id), occurred_at timestamptz NOT NULL,
    FOREIGN KEY(account_id,site_id) REFERENCES app.site(account_id,site_id),
    UNIQUE(resource_id,event_kind,resource_version)
);
GRANT SELECT,INSERT ON app.business_event_outbox TO maint_app;
