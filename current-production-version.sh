#!/bin/bash

readonly BUILD_VERSION_URL="https://www.rikerapp.com/riker/d/build-version"

echo "Proceeding to fetch current production build version..."
STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" ${BUILD_VERSION_URL})
if [ "${STATUS_CODE}" == "200" ]; then
    echo "200 code returned."
    VERSION_DEPLOYED=$(curl -s ${BUILD_VERSION_URL})
    echo "Current production version: [${VERSION_DEPLOYED}]"
else
    echo "ERROR!  Status code: [${STATUS_CODE}]"
fi
