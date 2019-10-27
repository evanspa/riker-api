#!/bin/bash

readonly HOST="rikerapp"
readonly MAINTENANCE_MARKER_FILE="/home/fprest/documents/maintenance-on"

ssh ${HOST} -t rm ${MAINTENANCE_MARKER_FILE}
