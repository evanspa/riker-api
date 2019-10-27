#!/bin/bash

readonly HOST="rikerapp"
readonly DATABASE="riker"

readonly EVANSP2_USER_ID="2"

ssh ${HOST} -t psql -d ${DATABASE} -c \"update user_account set next_invoice_amount = null, last_invoice_at = null, last_invoice_amount = null, current_card_last4 = null, current_card_brand = null, current_card_exp_month = null, current_card_exp_year = null, paid_enrollment_established_at = null, paid_enrollment_cancelled_at = null, stripe_customer_id = null, paid_enrollment_cancelled_reason = null, last_charge_id = null, next_invoice_at = null, trial_almost_expired_notice_sent_at = null where id = ${EVANSP2_USER_ID}\"
