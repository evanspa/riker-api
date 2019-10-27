#!/bin/bash

readonly USER_EMAIL="$1"
readonly HOST="rikerapp"
readonly DATABASE="riker"

readonly USER_ACCOUNT_TABLE="user_account"
readonly IAP_RECEIPT_SANDBOX_VALIDATION_URL="https://sandbox.itunes.apple.com/verifyReceipt"
readonly UPDATE_SQL="update ${USER_ACCOUNT_TABLE} set app_store_receipt_validation_url = '${IAP_RECEIPT_SANDBOX_VALIDATION_URL}' where email = '${USER_EMAIL}'"

ssh ${HOST} -t psql -d ${DATABASE} -c \"${UPDATE_SQL}\"
