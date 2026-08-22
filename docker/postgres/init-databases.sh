#!/bin/bash
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE dlt_planning'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'dlt_planning')\gexec
    SELECT 'CREATE DATABASE dlt_wellness'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'dlt_wellness')\gexec
EOSQL
