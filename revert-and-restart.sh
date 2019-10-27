#!/bin/bash

readonly VERSION="$1"
readonly PROJECT="riker-app"
readonly USER="fprest"
readonly HOST="rikerapp"
readonly REMOTE_DEST_DIR="/home/${USER}/documents/riker-app/scripts"
readonly SERVICE_NAME="rikerapp-server"
readonly BUILD_VERSION_URL="https://www.rikerapp.com/riker/d/build-version"
readonly POST_RESTART_SLEEP_AMOUNT=30

echo "Proceeding to generate new service script..."
mkdir -p gen
sed -e s/{{VERSION}}/${VERSION}/g bin/rikerapp-server-template.sh > gen/rikerapp-server.sh

echo "Proceeding to upload service scripts..."
scp start-rikerapp-server.sh ${USER}@${HOST}:/${REMOTE_DEST_DIR}
scp stop-rikerapp-server.sh ${USER}@${HOST}:/${REMOTE_DEST_DIR}
scp gen/rikerapp-server.sh ${USER}@${HOST}:/${REMOTE_DEST_DIR}

echo "Proceeding to restart rikerapp-server..."
ssh ${HOST} -t "sudo monit stop ${SERVICE_NAME} && sudo monit start ${SERVICE_NAME}"

echo "Proceeding to wait ${POST_RESTART_SLEEP_AMOUNT} seconds to give server time to restart..."
sleep ${POST_RESTART_SLEEP_AMOUNT}

echo "Proceeding to validate deployment..."
STATUS_CODE=$(curl -s -o /dev/null -w "%{http_code}" ${BUILD_VERSION_URL})
if [ "${STATUS_CODE}" == "200" ]; then
    echo "200 code returned."
    RESPONSE=$(curl -s https://www.rikerapp.com)
    VERSION_DEPLOYED=$(curl -s ${BUILD_VERSION_URL})
    if [[ $VERSION_DEPLOYED == *"${VERSION}"* ]]; then
        echo "Version: ${VERSION} deployment validated."
    else
        echo "Version: ${VERSION} deployment NOT validated.  Version currently deployed: ${VERSION_DEPLOYED}"
    fi
else
    echo "ERROR!  Status code: [${STATUS_CODE}]"
fi



echo "Done."
