#!/bin/bash

readonly HOST="rikerapp"
readonly DATABASE="riker"

readonly SET_COUNTS_SQL="select count(s.*), ua.email from set s, user_account ua where s.user_id = ua.id and s.deleted_at is null group by ua.email"

ssh ${HOST} -t psql -d ${DATABASE} -c \"${SET_COUNTS_SQL}\"
