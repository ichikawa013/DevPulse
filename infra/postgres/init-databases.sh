#!/bin/sh

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
  CREATE DATABASE devpulse_users;
  GRANT ALL PRIVILEGES ON DATABASE devpulse_users TO $POSTGRES_USER;

  CREATE DATABASE devpulse_feed;
  GRANT ALL PRIVILEGES ON DATABASE devpulse_feed TO $POSTGRES_USER;

  CREATE DATABASE devpulse_notifications;
  GRANT ALL PRIVILEGES ON DATABASE devpulse_notifications TO $POSTGRES_USER;
EOSQL

echo "All databases created successfully."