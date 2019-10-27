#!/bin/bash

readonly CUSTOMER=$1
readonly EMAIL=$2
readonly HOST="dev.rikerapp.com"
readonly WEBHOOK_SECRET_PATH="sQAq0BDF02KwqWzzqI4OZcoutvuOUZ09cxXH5BpoDVDy0hntHo9qQECU0QvHYTh_"
readonly PORT="3006"

curl -v -H "Content-Type: application/json" \
     -H "Accept-Language: en-US" \
     -H "Accept-Charset: UTF-8" \
     -H "Accept: application/json" \
     -X POST \
     -d "{\"type\": \"customer.created\", \"data\": {\"object\": {\"id\": \""${CUSTOMER}"\", \"email\": \""${EMAIL}\""}}}" \
     http://${HOST}:${PORT}/riker/d/${WEBHOOK_SECRET_PATH}/stripe-events
