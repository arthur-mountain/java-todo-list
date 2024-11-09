#!/usr/bin/env bash

set -a
source .env
set +a

envsubst \
  <database/migrations/init_users.template \
  >database/migrations/02_init_users.sql
