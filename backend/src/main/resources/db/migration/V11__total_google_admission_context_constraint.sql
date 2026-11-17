-- SQL CHECK also accepts UNKNOWN. Make the optional admission tuple a total boolean predicate.
ALTER TABLE app.google_oauth_transaction DROP CONSTRAINT google_registration_context;
ALTER TABLE app.google_oauth_transaction ADD CONSTRAINT google_registration_context CHECK (
    CASE
        WHEN registration_kind IS NULL THEN registration_email IS NULL AND invitation_fingerprint IS NULL
        WHEN registration_kind='TECHNICIAN' THEN intention='LOGIN' AND registration_email IS NULL AND invitation_fingerprint IS NULL
        WHEN registration_kind='CLIENT' THEN intention='LOGIN' AND channel='WEB' AND registration_email IS NOT NULL AND invitation_fingerprint IS NOT NULL
        ELSE false
    END
);
