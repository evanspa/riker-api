#!/bin/bash

readonly EMAIL=$1
readonly PASSWORD=$2
readonly HOST="dev.rikerapp.com"
readonly PORT="3006"

curl -v -H "Accept-Language: en-US" \
     -H "Accept-Charset: UTF-8" \
     -H "Accept: application/vnd.riker.plan-v0.0.1+json" \
     -X GET \
     http://${HOST}:${PORT}/riker/d/plan \
     -o tmp.html
