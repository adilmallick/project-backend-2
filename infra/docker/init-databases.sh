#!/bin/bash
# =============================================================================
# Database-per-Service Provisioning Script
# Executed automatically by PostgreSQL container upon initial database creation.
#
# DECISION: Automatically provision separate databases for each microservice.
# ALTERNATIVES: Single shared database with prefixed tables (rejected: violates database-per-service pattern).
# INVARIANT: Every microservice must own its own isolated database.
# SEE: ADR-001
# =============================================================================

set -e
set -u

function create_user_and_database() {
    local database=$1
    echo "Creating database: '$database' for user: '$POSTGRES_USER'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        SELECT 'CREATE DATABASE $database OWNER $POSTGRES_USER'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$database')\gexec
        GRANT ALL PRIVILEGES ON DATABASE $database TO $POSTGRES_USER;
EOSQL
}

if [ -n "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
    echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
        create_user_and_database $db
    done
    echo "All microservice databases created successfully."
fi
