#!/bin/bash

readonly MAINTENANCE_STARTS_AT="$1"
readonly MAINTENANCE_DURATION="$2"
readonly HOST="rikerapp"
readonly DATABASE="riker"

readonly MAINTENANCE_WINDOW_TABLE="maintenance_window"
readonly CLEAR_MAINTENANCE_WINDOW_SQL="delete from ${MAINTENANCE_WINDOW_TABLE}"
readonly SCHEDULE_MAINTENANCE_SQL="insert into ${MAINTENANCE_WINDOW_TABLE} values(current_timestamp, timestamp '${MAINTENANCE_STARTS_AT}', ${MAINTENANCE_DURATION})"

ssh ${HOST} -t psql -d ${DATABASE} -c \"${CLEAR_MAINTENANCE_WINDOW_SQL}\"
ssh ${HOST} -t psql -d ${DATABASE} -c \"${SCHEDULE_MAINTENANCE_SQL}\"
