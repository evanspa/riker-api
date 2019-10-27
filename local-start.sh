#!/bin/bash

################################################################################
# Locations
################################################################################
readonly RIKERAPP_UBERJAR_VERSION="$1"
readonly RIKERAPP_UBERJARS_DIR="$HOME/Documents/BitBucket-repos/riker-app/target"
readonly RIKERAPP_UBERJAR_NAME="riker-app-${RIKERAPP_UBERJAR_VERSION}-standalone.jar"

################################################################################
# Classpath entries
################################################################################
readonly RIKERAPP_TEMPLATES_ROOT_DIR="$HOME/Documents/BitBucket-repos/riker-app/email-templates"

################################################################################
# Config variables
################################################################################
readonly R_DB_NAME="riker"
readonly R_DB_SERVER_HOST="localhost"
readonly R_DB_SERVER_PORT="5432"
readonly R_DB_USERNAME="postgres"
readonly R_DB_PASSWORD=""
readonly R_JDBC_DRIVER_CLASS="org.postgresql.Driver"
readonly R_JDBC_SUBPROTOCOL="postgresql"
readonly R_HYPERMEDIA_BASE_URL="https://www.rikerapp.com"
readonly R_EMAIL_LIVE_MODE="true"
readonly R_MAILGUN_API_KEY="key-395c6cdac26816949ca4e475e0a2b133"
readonly R_APP_URI_PREFIX="/riker/d/"
readonly R_SMTP_HOST="localhost"
readonly RIKERAPP_SERVER_MAIN_CLASS="riker_app.app.endpoint.main"
readonly RIKERAPP_SERVER_JVM_INIT_MEMORY="256m"
readonly RIKERAPP_SERVER_JVM_MAX_MEMORY="512m"
readonly RIKERAPP_SERVER_RUNDIR="$HOME/run"
readonly RIKERAPP_SERVER_LOGSDIR="$HOME/Documents/BitBucket-repos/riker-app/logs"
readonly RIKERAPP_SERVER_PID="$RIKERAPP_SERVER_RUNDIR/rikerapp-server.pid"
readonly RIKERAPP_SERVER_OUT="$RIKERAPP_SERVER_LOGSDIR/rikerapp-server.out"
readonly RIKERAPP_SERVER_DESC="Riker App server"
readonly R_NREPL_SERVER_PORT=7889
readonly R_TRIAL_PERIOD_IN_DAYS=90
readonly R_TRIAL_PERIOD_EXP_GRACE_PERIOD_IN_DAYS=10
readonly R_TRIAL_ALMOST_EXPIRES_THRESHOLD_IN_DAYS=5
readonly R_SUBSCRIPTION_AMOUNT=1100
readonly R_STRIPE_SECRET_KEY="sk_live_g8P6owQhkYuxHh2VcDfkWPTv"
readonly R_STRIPE_SUBSCRIPTION_NAME="basic"
readonly R_STRIPE_WEBHOOK_SECRET_PATH_COMP="sQAq0BDF02KwqWzzqI4OZcoutvuOUZ09cxXH5BpoDVDy0hntHo9qQECU0QvHYTh_"
readonly R_NEW_USER_NOTIFICATION_FROM_EMAIL="alerts@rikerapp.com"
readonly R_NEW_USER_NOTIFICATION_TO_EMAIL="evansp2@gmail.com"
readonly R_NEW_USER_NOTIFICATION_SUBJECT="New Riker sign-up!"
readonly R_ERR_NOTIFICATION_FROM_EMAIL="alerts@rikerapp.com"
readonly R_ERR_NOTIFICATION_TO_EMAIL="evansp2@gmail.com"
readonly R_ERR_NOTIFICATION_SUBJECT="Riker error"

################################################################################
# JVM property names
################################################################################
readonly R_APP_VERSION_LKUP_KEY="r.app.version"
readonly R_APP_URI_PREFIX_LKUP_KEY="r.uri.prefix"
readonly R_DB_NAME_LKUP_KEY="r.db.name"
readonly R_DB_SERVER_HOST_LKUP_KEY="r.db.server.host"
readonly R_DB_SERVER_PORT_LKUP_KEY="r.db.server.port"
readonly R_DB_USERNAME_LKUP_KEY="r.db.username"
readonly R_DB_PASSWORD_LKUP_KEY="r.db.password"
readonly R_JDBC_DRIVER_CLASS_LKUP_KEY="r.jdbc.driver.class"
readonly R_JDBC_SUBPROTOCOL_LKUP_KEY="r.jdbc.subprotocol"
readonly R_HYPERMEDIA_BASE_URL_LKUP_KEY="r.base.url"
readonly R_EMAIL_LIVE_MODE_KEY="r.email.live.mode"
readonly R_MAILGUN_API_KEY_KEY="r.mailgun.api.key"
readonly R_SMTP_HOST_KEY="r.smtp.host"
readonly R_NREPL_SERVER_PORT_KEY="r.nrepl.server.port"
readonly R_TRIAL_PERIOD_IN_DAYS_KEY="r.trial.period.in.days"
readonly R_TRIAL_PERIOD_EXP_GRACE_PERIOD_IN_DAYS_KEY="r.trial.period.expired.grace.period.in.days"
readonly R_TRIAL_ALMOST_EXPIRES_THRESHOLD_IN_DAYS_KEY="r.trial.almost.expires.threshold.in.days"
readonly R_SUBSCRIPTION_AMOUNT_KEY="r.subscription.amount"
readonly R_STRIPE_SECRET_KEY_KEY="r.stripe.secret.key"
readonly R_STRIPE_SUBSCRIPTION_NAME_KEY="r.stripe.subscription.name"
readonly R_STRIPE_WEBHOOK_SECRET_PATH_COMP_KEY="r.stripe.webhook.secret.path.comp"
readonly R_NEW_USER_NOTIFICATION_FROM_EMAIL_KEY="r.new.user.notification.from.email"
readonly R_NEW_USER_NOTIFICATION_TO_EMAIL_KEY="r.new.user.notification.to.email"
readonly R_NEW_USER_NOTIFICATION_SUBJECT_KEY="r.new.user.notification.subject"
readonly R_ERR_NOTIFICATION_FROM_EMAIL_KEY="r.err.notification.from.email"
readonly R_ERR_NOTIFICATION_TO_EMAIL_KEY="r.err.notification.to.email"
readonly R_ERR_NOTIFICATION_SUBJECT_KEY="r.err.notification.subject"
readonly R_LOGBACK_LOGSDIR_KEY="RIKERAPP_LOGS_DIR"

touch "$RIKERAPP_SERVER_OUT"
java -D${R_HYPERMEDIA_BASE_URL_LKUP_KEY}=${R_HYPERMEDIA_BASE_URL} \
-D${R_SMTP_HOST_KEY}=${R_SMTP_HOST} \
-D${R_APP_VERSION_LKUP_KEY}=${RIKERAPP_UBERJAR_VERSION} \
-D${R_APP_URI_PREFIX_LKUP_KEY}=${R_APP_URI_PREFIX} \
-D${R_DB_NAME_LKUP_KEY}=${R_DB_NAME} \
-D${R_DB_SERVER_HOST_LKUP_KEY}=${R_DB_SERVER_HOST} \
-D${R_DB_SERVER_PORT_LKUP_KEY}=${R_DB_SERVER_PORT} \
-D${R_DB_USERNAME_LKUP_KEY}=${R_DB_USERNAME} \
-D${R_DB_PASSWORD_LKUP_KEY}=${R_DB_PASSWORD} \
-D${R_MAILGUN_API_KEY_KEY}=${R_MAILGUN_API_KEY} \
-D${R_STRIPE_WEBHOOK_SECRET_PATH_COMP_KEY}=${R_STRIPE_WEBHOOK_SECRET_PATH_COMP} \
-D${R_EMAIL_LIVE_MODE_KEY}=${R_EMAIL_LIVE_MODE} \
-D${R_JDBC_DRIVER_CLASS_LKUP_KEY}=${R_JDBC_DRIVER_CLASS} \
-D${R_JDBC_SUBPROTOCOL_LKUP_KEY}=${R_JDBC_SUBPROTOCOL} \
-D${R_NREPL_SERVER_PORT_KEY}=${R_NREPL_SERVER_PORT} \
-D${R_TRIAL_PERIOD_IN_DAYS_KEY}=${R_TRIAL_PERIOD_IN_DAYS} \
-D${R_TRIAL_PERIOD_EXP_GRACE_PERIOD_IN_DAYS_KEY}=${R_TRIAL_PERIOD_EXP_GRACE_PERIOD_IN_DAYS} \
-D${R_TRIAL_ALMOST_EXPIRES_THRESHOLD_IN_DAYS_KEY}=${R_TRIAL_ALMOST_EXPIRES_THRESHOLD_IN_DAYS} \
-D${R_SUBSCRIPTION_AMOUNT_KEY}=${R_SUBSCRIPTION_AMOUNT} \
-D${R_STRIPE_SECRET_KEY_KEY}=${R_STRIPE_SECRET_KEY} \
-D${R_STRIPE_SUBSCRIPTION_NAME_KEY}=${R_STRIPE_SUBSCRIPTION_NAME} \
-D${R_NEW_USER_NOTIFICATION_FROM_EMAIL_KEY}=${R_NEW_USER_NOTIFICATION_FROM_EMAIL} \
-D${R_NEW_USER_NOTIFICATION_TO_EMAIL_KEY}=${R_NEW_USER_NOTIFICATION_TO_EMAIL} \
-D${R_NEW_USER_NOTIFICATION_SUBJECT_KEY}="${R_NEW_USER_NOTIFICATION_SUBJECT}" \
-D${R_ERR_NOTIFICATION_FROM_EMAIL_KEY}=${R_ERR_NOTIFICATION_FROM_EMAIL} \
-D${R_ERR_NOTIFICATION_TO_EMAIL_KEY}=${R_ERR_NOTIFICATION_TO_EMAIL} \
-D${R_ERR_NOTIFICATION_SUBJECT_KEY}="${R_ERR_NOTIFICATION_SUBJECT}" \
-D${R_LOGBACK_LOGSDIR_KEY}=${RIKERAPP_SERVER_LOGSDIR} \
-server \
-Xms${RIKERAPP_SERVER_JVM_INIT_MEMORY} \
-Xmx${RIKERAPP_SERVER_JVM_MAX_MEMORY} \
-classpath ${RIKERAPP_TEMPLATES_ROOT_DIR}:\
${RIKERAPP_UBERJARS_DIR}/${RIKERAPP_UBERJAR_NAME} \
${RIKERAPP_SERVER_MAIN_CLASS}
