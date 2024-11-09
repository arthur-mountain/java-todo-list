#!/usr/bin/env bash

set -a
source .env
set +a

envsubst \
  <database/migrations/init_users.template \
  >database/migrations/init_users.sql
