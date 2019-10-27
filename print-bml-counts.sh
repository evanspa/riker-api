#!/bin/bash

readonly HOST="rikerapp"
readonly DATABASE="riker"

readonly BML_COUNTS_SQL="select count(bml.*), ua.email from body_journal_log bml, user_account ua where bml.user_id = ua.id and bml.deleted_at is null group by ua.email"

ssh ${HOST} -t psql -d ${DATABASE} -c \"${BML_COUNTS_SQL}\"
