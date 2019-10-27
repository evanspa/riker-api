#!/bin/bash

readonly EMAIL=$1
readonly PASSWORD=$2
readonly HOST="dev.rikerapp.com"

curl -v -H "Content-Type: application/vnd.riker.user-v0.0.1+json;charset=UTF-8" \
     -H "Accept-Language: en-US" \
     -H "Accept-Charset: UTF-8" \
     -H "Accept: application/vnd.riker.user-v0.0.1+json" \
     -H "r-establish-session: true" \
     -H "r-desired-embedded-format: id-keyed" \
     -X POST \
     -d "{\"user/email\": \""${EMAIL}"\", \"user/password\": \""${PASSWORD}\""}" \
     http://${HOST}:4040/riker/d/users \
     -o tmp.json
