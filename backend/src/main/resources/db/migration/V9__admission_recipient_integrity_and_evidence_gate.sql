-- Expand after V8 without rewriting its tested schema.
ALTER TABLE app.client_invitation ADD CONSTRAINT client_invitation_accepted_identity_unique UNIQUE(invitation_id,accepted_user_id);
ALTER TABLE app.client_onboarding ADD CONSTRAINT client_onboarding_recipient_fk
    FOREIGN KEY(invitation_id,user_id) REFERENCES app.client_invitation(invitation_id,accepted_user_id);

-- Confirmed product requirement. Until the document workflow and evidence policy are delivered,
-- even an erroneous SQL writer must not mark a technician dossier APPROVED.
-- Lot 09 must replace this gate with tested evidence checks in an explicit migration.
ALTER TABLE app.technician_onboarding ADD CONSTRAINT technician_evidence_required CHECK(state<>'APPROVED');
ALTER TABLE app.technician_onboarding_history ADD CONSTRAINT technician_evidence_history_required CHECK(action<>'APPROVED');
