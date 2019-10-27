#!/bin/bash

readonly MAINTENANCE_STARTS_AT="$1"
readonly MAINTENANCE_DURATION="$2"
readonly DATABASE="riker"

readonly SCHEDULE_MAINTENANCE_SQL="insert into ${MAINTENANCE_WINDOW_TABLE} values(current_timestamp, timestamp '${MAINTENANCE_STARTS_AT}', ${MAINTENANCE_DURATION})"

psql -d ${DATABASE} -c 'delete from maintenance_window'
psql -d ${DATABASE} -c "insert into maintenance_window values(current_timestamp, timestamp '${MAINTENANCE_STARTS_AT}', ${MAINTENANCE_DURATION})"
