-- LOGIN remains unable to register implicitly. Only explicit admission endpoints set this immutable context.
ALTER TABLE app.google_oauth_transaction
    ADD COLUMN registration_kind varchar(16) CHECK(registration_kind IN ('CLIENT','TECHNICIAN')),
    ADD COLUMN registration_email varchar(254),
    ADD COLUMN invitation_fingerprint char(64) REFERENCES app.client_invitation_link(fingerprint),
    ADD CONSTRAINT google_registration_context CHECK (
        (registration_kind IS NULL AND registration_email IS NULL AND invitation_fingerprint IS NULL)
        OR (registration_kind='TECHNICIAN' AND intention='LOGIN' AND registration_email IS NULL AND invitation_fingerprint IS NULL)
        OR (registration_kind='CLIENT' AND intention='LOGIN' AND channel='WEB' AND registration_email IS NOT NULL AND invitation_fingerprint IS NOT NULL)
    );
