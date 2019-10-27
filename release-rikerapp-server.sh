#!/bin/bash

readonly VERSION="$1"
readonly PROJECT="riker-app"
readonly TAG_LABEL=${VERSION}
readonly USER="fprest"
readonly HOST="rikerapp"
readonly REMOTE_DEST_BASE_DIR="/home/${USER}/documents/riker-app"
readonly REMOTE_SCRIPTS_DEST_DIR="${REMOTE_DEST_BASE_DIR}/scripts"
readonly REMOTE_EMAIL_TEMPLATES_DEST_DIR="${REMOTE_DEST_BASE_DIR}/rikerapp-email-templates"
readonly REMOTE_UBERJARS_DEST_DIR="${REMOTE_DEST_BASE_DIR}/rikerapp-uberjars"
readonly SERVICE_NAME="rikerapp-server"
readonly WS_LOCAL_BUILD_VERSION_URL="http://localhost:3006/riker/d/build-version"
readonly POST_STOP_SLEEP_AMOUNT=30
readonly POST_START_SLEEP_AMOUNT=60

echo "Proceeding to commit and tag riker-app (and push tag)..."
git add .
git commit -m "release commit for version ${VERSION}"
git tag -f -a $TAG_LABEL -m "version ${VERSION}"
git push -f --tags --quiet

echo "Proceeding to make riker-app uber jar..."
lein ring uberjar
scp target/riker-app-${VERSION}-standalone.jar ${USER}@${HOST}:${REMOTE_UBERJARS_DEST_DIR}

echo "Proceeding to generate new service script..."
mkdir -p gen
sed -e s/{{VERSION}}/${VERSION}/g rikerapp-server-template.sh > gen/rikerapp-server.sh

echo "Proceeding to upload service scripts..."
scp start-rikerapp-server.sh ${USER}@${HOST}:${REMOTE_SCRIPTS_DEST_DIR}
scp stop-rikerapp-server.sh ${USER}@${HOST}:${REMOTE_SCRIPTS_DEST_DIR}
scp gen/rikerapp-server.sh ${USER}@${HOST}:${REMOTE_SCRIPTS_DEST_DIR}

echo "Proceeding to upload email templates..."
scp email-templates/* ${USER}@${HOST}:${REMOTE_EMAIL_TEMPLATES_DEST_DIR}

echo "Proceeding to stop rikerapp-server... (and then wait ${RESTART_SLEEP_AMOUNT})..."
ssh ${HOST} -t "sudo monit stop ${SERVICE_NAME}"
echo "...waiting ${POST_STOP_SLEEP_AMOUNT} sec to give server time to stop..."
sleep ${POST_STOP_SLEEP_AMOUNT}
echo "Proceeding to start rikerapp-server..."
ssh ${HOST} -t "sudo monit start ${SERVICE_NAME}"
echo "...waiting ${POST_START_SLEEP_AMOUNT} sec to give server time to start..."
sleep ${POST_START_SLEEP_AMOUNT}

echo "Proceeding to validate deployment..."
STATUS_CODE=$(ssh ${HOST} -t "curl -s -o /dev/null -w '%{http_code}' ${WS_LOCAL_BUILD_VERSION_URL}")
if [ "${STATUS_CODE}" == "200" ]; then
    echo "Good.  200 code returned from Riker build version web service endpoint (using local url)."
    echo "Proceeding to validate correct build version actually deployed..."
    VERSION_DEPLOYED=$(ssh ${HOST} -t "curl -s ${WS_LOCAL_BUILD_VERSION_URL}")
    if [[ ${VERSION_DEPLOYED} == *"${VERSION}"* ]]; then
        echo "Good.  Version: ${VERSION_DEPLOYED} returned from WS build version endpoint.  Deployment fully validated."
    else
        echo "Oops.  Version: ${VERSION} NOT returned from WS build version endpoint.  Deployment NOT validated.  Version currently returned: ${VERSION_DEPLOYED}"
    fi
else
    echo "Oops.  Status code: [${STATUS_CODE}] returned from Riker build version web service endpoint."
fi

echo "Done."
