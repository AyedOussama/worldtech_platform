-- Keep V1-V6 immutable. The V3 login writer predates authenticated_at and omits it.
-- A default preserves that INSERT shape during a rolling deployment/application rollback.
-- Current writers continue to provide their injected UTC Clock explicitly.
ALTER TABLE app.auth_session ALTER COLUMN authenticated_at SET DEFAULT CURRENT_TIMESTAMP;
