#!/usr/bin/env bash
# Volume neuf uniquement. Ne pas activer set -x : les variables contiennent des secrets.
set -euo pipefail
# Accept an existing Windows CRLF file exactly as Spring/JDBC read it (strip line endings).
export MAINT_MIGRATOR_PASSWORD="$(tr -d '\r\n' < /run/secrets/migrator_password)"
export MAINT_RUNTIME_PASSWORD="$(tr -d '\r\n' < /run/secrets/runtime_password)"
psql -X --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set ON_ERROR_STOP=1 <<'SQL'
\getenv migrator_password MAINT_MIGRATOR_PASSWORD
\getenv runtime_password MAINT_RUNTIME_PASSWORD
CREATE ROLE maint_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS
  PASSWORD :'migrator_password';
CREATE ROLE maint_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS
  PASSWORD :'runtime_password';
REVOKE ALL ON DATABASE maintenance FROM PUBLIC;
GRANT CONNECT ON DATABASE maintenance TO maint_migrator, maint_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;
CREATE SCHEMA app AUTHORIZATION maint_migrator;
GRANT USAGE ON SCHEMA app TO maint_app;
GRANT USAGE ON SCHEMA public TO maint_migrator, maint_app;
ALTER DEFAULT PRIVILEGES FOR ROLE maint_migrator IN SCHEMA app REVOKE ALL ON TABLES FROM PUBLIC, maint_app;
ALTER DEFAULT PRIVILEGES FOR ROLE maint_migrator IN SCHEMA app REVOKE ALL ON SEQUENCES FROM PUBLIC, maint_app;
ALTER DEFAULT PRIVILEGES FOR ROLE maint_migrator REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
-- Chaque migration accorde explicitement les opérations runtime nécessaires.
-- Audit : écriture INSERT seulement ; aucun droit runtime sur l'historique Flyway.
ALTER ROLE maint_migrator IN DATABASE maintenance SET search_path = app, public;
ALTER ROLE maint_app IN DATABASE maintenance SET search_path = app, public;
ALTER ROLE maint_app SET statement_timeout = '15s';
ALTER ROLE maint_app SET idle_in_transaction_session_timeout = '30s';
SQL
unset MAINT_MIGRATOR_PASSWORD MAINT_RUNTIME_PASSWORD
