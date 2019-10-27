#!/bin/bash

readonly USER="fprest"
readonly HOST="rikerapp"
readonly REMOTE_DEST_BASE_DIR="/home/${USER}/documents/riker-app"
readonly REMOTE_DEST_NGINX_DIR="${REMOTE_DEST_BASE_DIR}/nginx"

scp etc/nginx/sites-available/rikerapp.com ${USER}@${HOST}:${REMOTE_DEST_NGINX_DIR}
scp etc/nginx/sites-available/www.rikerapp.com ${USER}@${HOST}:${REMOTE_DEST_NGINX_DIR}
scp etc/nginx/sites-available/riker_config.conf ${USER}@${HOST}:${REMOTE_DEST_NGINX_DIR}
