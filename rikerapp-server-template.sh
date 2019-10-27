#!/bin/bash

################################################################################
# Locations
################################################################################
readonly RIKERAPP_UBERJAR_VERSION="{{VERSION}}"
readonly RIKERAPP_UBERJARS_DIR="$HOME/documents/riker-app/rikerapp-uberjars"
readonly RIKERAPP_UBERJAR_NAME="riker-app-${RIKERAPP_UBERJAR_VERSION}-standalone.jar"

################################################################################
# Classpath entries
################################################################################
readonly RIKERAPP_TEMPLATES_ROOT_DIR="$HOME/documents/riker-app/rikerapp-email-templates"

################################################################################
# Config variables
################################################################################
readonly R_DB_NAME="riker"
readonly R_DB_SERVER_HOST="localhost"
readonly R_DB_SERVER_PORT="5432"
readonly R_DB_USERNAME="fprest"
readonly R_DB_PASSWORD="fprest"
readonly R_JDBC_DRIVER_CLASS="org.postgresql.Driver"
readonly R_JDBC_SUBPROTOCOL="postgresql"
readonly R_HYPERMEDIA_BASE_URL="https://www.rikerapp.com"
readonly R_EMAIL_LIVE_MODE="true"
readonly R_DOMAIN="rikerapp.com"
readonly R_MAILGUN_API_KEY="key-395c6cdac26816949ca4e475e0a2b133"
readonly R_LOGIN_COOKIE_SECURE="true"
readonly R_MAX_ALLOWED_SET_IMPORT=100000
readonly R_MAX_ALLOWED_BML_IMPORT=10000
readonly R_APP_URI_PREFIX="/riker/d/"
readonly R_SMTP_HOST="localhost"
readonly RIKERAPP_SERVER_MAIN_CLASS="riker_app.app.endpoint.main"
readonly RIKERAPP_SERVER_JVM_INIT_MEMORY="512m"
readonly RIKERAPP_SERVER_JVM_MAX_MEMORY="1024m"
readonly RIKERAPP_SERVER_RUN_DIR="$HOME/run"
readonly RIKERAPP_SERVER_LOGS_DIR="$HOME/logs"
readonly RIKERAPP_SERVER_LOGS_ROLLOVER_DIR="$HOME/Dropbox/riker-rolled-logs"
readonly RIKERAPP_SERVER_PID="$RIKERAPP_SERVER_RUN_DIR/rikerapp-server.pid"
readonly RIKERAPP_SERVER_OUT="$RIKERAPP_SERVER_LOGS_DIR/rikerapp-server-startup.out"
readonly RIKERAPP_SERVER_DESC="Riker App server"
readonly R_NREPL_SERVER_PORT=7889
readonly R_TRIAL_PERIOD_IN_DAYS=90
readonly R_TRIAL_PERIOD_EXP_GRACE_PERIOD_IN_DAYS=10
readonly R_TRIAL_ALMOST_EXPIRES_THRESHOLD_IN_DAYS=5
readonly R_SUBSCRIPTION_AMOUNT=1149
readonly R_STRIPE_SUBSCRIPTION_NAME="basic"
readonly R_STRIPE_SECRET_KEY="sk_live_g8P6owQhkYuxHh2VcDfkWPTv"
readonly R_STRIPE_WEBHOOK_SECRET_PATH_COMP="2FnyvmoRQy1BfMOiLoE7SnJjQJF_KVFE1eGHs_qgTcuKxbSmXbOxz9h2V9W7bt28o_qEy6Mkc02gq0flQTPDCmsRsfwfbWg_uiYr"
readonly R_APPLE_SEARCH_ADS_ATTRIBUTION_UPLOAD_SECRET_PATH_COMP="NWSkl-eahwMvWUYDeu3gqziDXeq28XN84dpAhwfOGroOMJl23OCooKmQTbnyVJzVMDkdeWDCajUtS7_x"
readonly R_FACEBOOK_APP_SECRET="2d8b70f4db70e42b5b1a5bbd978d5962"
readonly R_FACEBOOK_CALLBACK_SECRET_PATH_COMP="zNPlCGdOaLK_ZEH3Da6MYWm_x-dd-SrnDR02HorO"
readonly R_APP_STORE_RECEIPT_VALIDATION_URL="https://buy.itunes.apple.com/verifyReceipt"
readonly R_APP_STORE_RECEIPT_VALIDATION_SHARED_SECRET="459efd354e5246cc8d0f85a6b2546c3b"
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
readonly R_DOMAIN_KEY="r.domain"
readonly R_MAILGUN_API_KEY_KEY="r.mailgun.api.key"
readonly R_LOGIN_COOKIE_SECURE_KEY="r.login.cookie.secure"
readonly R_MAX_ALLOWED_SET_IMPORT_KEY="r.max.allowed.set.import"
readonly R_MAX_ALLOWED_BML_IMPORT_KEY="r.max.allowed.bml.import"
readonly R_SMTP_HOST_KEY="r.smtp.host"
readonly R_NREPL_SERVER_PORT_KEY="r.nrepl.server.port"
readonly R_TRIAL_PERIOD_IN_DAYS_KEY="r.trial.period.in.days"
readonly R_TRIAL_PERIOD_EXP_GRACE_PERIOD_IN_DAYS_KEY="r.trial.period.expired.grace.period.in.days"
readonly R_TRIAL_ALMOST_EXPIRES_THRESHOLD_IN_DAYS_KEY="r.trial.almost.expires.threshold.in.days"
readonly R_SUBSCRIPTION_AMOUNT_KEY="r.subscription.amount"
readonly R_STRIPE_SECRET_KEY_KEY="r.stripe.secret.key"
readonly R_STRIPE_SUBSCRIPTION_NAME_KEY="r.stripe.subscription.name"
readonly R_STRIPE_WEBHOOK_SECRET_PATH_COMP_KEY="r.stripe.webhook.secret.path.comp"
readonly R_APPLE_SEARCH_ADS_ATTRIBUTION_UPLOAD_SECRET_PATH_COMP_KEY="r.apple.search.ads.attribution.upload.secret.path.comp"
readonly R_FACEBOOK_APP_SECRET_KEY="r.facebook.app.secret"
readonly R_FACEBOOK_CALLBACK_SECRET_PATH_COMP_KEY="r.facebook.callback.secret.path.comp"
readonly R_APP_STORE_RECEIPT_VALIDATION_URL_KEY="r.app.store.receipt.validation.url"
readonly R_APP_STORE_RECEIPT_VALIDATION_SHARED_SECRET_KEY="r-app-store-receipt-validation-shared-secret"
readonly R_NEW_USER_NOTIFICATION_FROM_EMAIL_KEY="r.new.user.notification.from.email"
readonly R_NEW_USER_NOTIFICATION_TO_EMAIL_KEY="r.new.user.notification.to.email"
readonly R_NEW_USER_NOTIFICATION_SUBJECT_KEY="r.new.user.notification.subject"
readonly R_ERR_NOTIFICATION_FROM_EMAIL_KEY="r.err.notification.from.email"
readonly R_ERR_NOTIFICATION_TO_EMAIL_KEY="r.err.notification.to.email"
readonly R_ERR_NOTIFICATION_SUBJECT_KEY="r.err.notification.subject"
readonly R_LOGBACK_LOGS_DIR_KEY="RIKERAPP_LOGS_DIR"
readonly R_LOGBACK_LOGS_ROLLOVER_DIR_KEY="RIKERAPP_LOGS_ROLLOVER_DIR"

mkdir -p $RIKERAPP_SERVER_RUN_DIR
mkdir -p $RIKERAPP_SERVER_LOGS_DIR
mkdir -p $RIKERAPP_SERVER_LOGS_ROLLOVER_DIR

if [ "$1" = "start" ] ; then
    if [ ! -z "$RIKERAPP_SERVER_PID" ]; then
        if [ -f "$RIKERAPP_SERVER_PID" ]; then
            if [ -s "$RIKERAPP_SERVER_PID" ]; then
                echo "Existing PID file found during start."
                if [ -r "$RIKERAPP_SERVER_PID" ]; then
                    PID=`cat "$RIKERAPP_SERVER_PID"`
                    ps -p $PID >/dev/null 2>&1
                    if [ $? -eq 0 ] ; then
                        echo "${RIKERAPP_SERVER_DESC} appears to still be running with PID $PID. Start aborted."
                        exit 1
                    else
                        echo "Removing/clearing stale PID file."
                        rm -f "$RIKERAPP_SERVER_PID" >/dev/null 2>&1
                        if [ $? != 0 ]; then
                            if [ -w "$RIKERAPP_SERVER_PID" ]; then
                                cat /dev/null > "$RIKERAPP_SERVER_PID"
                            else
                                echo "Unable to remove or clear stale PID file. Start aborted."
                                exit 1
                            fi
                        fi
                    fi
                else
                    echo "Unable to read PID file. Start aborted."
                    exit 1
                fi
            else
                rm -f "$RIKERAPP_SERVER_PID" >/dev/null 2>&1
                if [ $? != 0 ]; then
                    if [ ! -w "$RIKERAPP_SERVER_PID" ]; then
                        echo "Unable to remove or write to empty PID file. Start aborted."
                        exit 1
                    fi
                fi
            fi
        fi
    fi
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
-D${R_LOGIN_COOKIE_SECURE_KEY}=${R_LOGIN_COOKIE_SECURE} \
-D${R_MAX_ALLOWED_SET_IMPORT_KEY}=${R_MAX_ALLOWED_SET_IMPORT} \
-D${R_MAX_ALLOWED_BML_IMPORT_KEY}=${R_MAX_ALLOWED_BML_IMPORT} \
-D${R_STRIPE_WEBHOOK_SECRET_PATH_COMP_KEY}=${R_STRIPE_WEBHOOK_SECRET_PATH_COMP} \
-D${R_APPLE_SEARCH_ADS_ATTRIBUTION_UPLOAD_SECRET_PATH_COMP_KEY}=${R_APPLE_SEARCH_ADS_ATTRIBUTION_UPLOAD_SECRET_PATH_COMP} \
-D${R_FACEBOOK_APP_SECRET_KEY}=${R_FACEBOOK_APP_SECRET} \
-D${R_FACEBOOK_CALLBACK_SECRET_PATH_COMP_KEY}=${R_FACEBOOK_CALLBACK_SECRET_PATH_COMP} \
-D${R_EMAIL_LIVE_MODE_KEY}=${R_EMAIL_LIVE_MODE} \
-D${R_DOMAIN_KEY}=${R_DOMAIN} \
-D${R_JDBC_DRIVER_CLASS_LKUP_KEY}=${R_JDBC_DRIVER_CLASS} \
-D${R_JDBC_SUBPROTOCOL_LKUP_KEY}=${R_JDBC_SUBPROTOCOL} \
-D${R_NREPL_SERVER_PORT_KEY}=${R_NREPL_SERVER_PORT} \
-D${R_TRIAL_PERIOD_IN_DAYS_KEY}=${R_TRIAL_PERIOD_IN_DAYS} \
-D${R_TRIAL_PERIOD_EXP_GRACE_PERIOD_IN_DAYS_KEY}=${R_TRIAL_PERIOD_EXP_GRACE_PERIOD_IN_DAYS} \
-D${R_TRIAL_ALMOST_EXPIRES_THRESHOLD_IN_DAYS_KEY}=${R_TRIAL_ALMOST_EXPIRES_THRESHOLD_IN_DAYS} \
-D${R_SUBSCRIPTION_AMOUNT_KEY}=${R_SUBSCRIPTION_AMOUNT} \
-D${R_STRIPE_SECRET_KEY_KEY}=${R_STRIPE_SECRET_KEY} \
-D${R_STRIPE_SUBSCRIPTION_NAME_KEY}=${R_STRIPE_SUBSCRIPTION_NAME} \
-D${R_APP_STORE_RECEIPT_VALIDATION_URL_KEY}=${R_APP_STORE_RECEIPT_VALIDATION_URL} \
-D${R_APP_STORE_RECEIPT_VALIDATION_SHARED_SECRET_KEY}=${R_APP_STORE_RECEIPT_VALIDATION_SHARED_SECRET} \
-D${R_NEW_USER_NOTIFICATION_FROM_EMAIL_KEY}=${R_NEW_USER_NOTIFICATION_FROM_EMAIL} \
-D${R_NEW_USER_NOTIFICATION_TO_EMAIL_KEY}=${R_NEW_USER_NOTIFICATION_TO_EMAIL} \
-D${R_NEW_USER_NOTIFICATION_SUBJECT_KEY}="${R_NEW_USER_NOTIFICATION_SUBJECT}" \
-D${R_ERR_NOTIFICATION_FROM_EMAIL_KEY}=${R_ERR_NOTIFICATION_FROM_EMAIL} \
-D${R_ERR_NOTIFICATION_TO_EMAIL_KEY}=${R_ERR_NOTIFICATION_TO_EMAIL} \
-D${R_ERR_NOTIFICATION_SUBJECT_KEY}="${R_ERR_NOTIFICATION_SUBJECT}" \
-D${R_LOGBACK_LOGS_DIR_KEY}=${RIKERAPP_SERVER_LOGS_DIR} \
-D${R_LOGBACK_LOGS_ROLLOVER_DIR_KEY}=${RIKERAPP_SERVER_LOGS_ROLLOVER_DIR} \
-server \
-Xms${RIKERAPP_SERVER_JVM_INIT_MEMORY} \
-Xmx${RIKERAPP_SERVER_JVM_MAX_MEMORY} \
-classpath ${RIKERAPP_TEMPLATES_ROOT_DIR}:\
${RIKERAPP_UBERJARS_DIR}/${RIKERAPP_UBERJAR_NAME} \
${RIKERAPP_SERVER_MAIN_CLASS} \
>> $RIKERAPP_SERVER_OUT 2>&1 &
    if [ ! -z "$RIKERAPP_SERVER_PID" ]; then
        echo $! > "$RIKERAPP_SERVER_PID"
    fi
    echo "${RIKERAPP_SERVER_DESC} started."
elif [ "$1" = "stop" ] ; then
    shift
    SLEEP=5
    if [ ! -z "$1" ]; then
        echo $1 | grep "[^0-9]" >/dev/null 2>&1
        if [ $? -gt 0 ]; then
            SLEEP=$1
            shift
        fi
    fi

    FORCE=0
    if [ "$1" = "-force" ]; then
        shift
        FORCE=1
    fi

    if [ ! -z "$RIKERAPP_SERVER_PID" ]; then
        if [ -f "$RIKERAPP_SERVER_PID" ]; then
            if [ -s "$RIKERAPP_SERVER_PID" ]; then
                kill -0 `cat "$RIKERAPP_SERVER_PID"` >/dev/null 2>&1
                if [ $? -gt 0 ]; then
                    echo "PID file found but no matching process was found. Stop aborted."
                    exit 1
                fi
            else
                echo "PID file is empty and has been ignored."
            fi
        else
            echo "\$RIKERAPP_SERVER_PID was set but the specified file does not exist.  Stop aborted."
            exit 1
        fi
    fi
    echo "Proceeding to signal the process to stop through OS signal."
    kill -15 `cat "$RIKERAPP_SERVER_PID"` > /dev/null 2>&1
    if [ ! -z "$RIKERAPP_SERVER_PID" ]; then
        if [ -f "$RIKERAPP_SERVER_PID" ]; then
            while [ $SLEEP -ge 0 ]; do
                kill -0 `cat "$RIKERAPP_SERVER_PID"` >/dev/null 2>&1
                if [ $? -gt 0 ]; then
                    rm -f "$RIKERAPP_SERVER_PID" >/dev/null 2>&1
                    if [ $? != 0 ]; then
                        if [ -w "$RIKERAPP_SERVER_PID" ]; then
                            cat /dev/null > "$RIKERAPP_SERVER_PID"
                            # If Riker App server has stopped don't try and force a stop with an empty PID file
                            FORCE=0
                        else
                            echo "The PID file could not be removed or cleared."
                        fi
                    fi
                    echo "${RIKERAPP_SERVER_DESC} stopped."
                    break
                fi
                if [ $SLEEP -gt 0 ]; then
                    sleep 1
                fi
                if [ $SLEEP -eq 0 ]; then
                    if [ $FORCE -eq 0 ]; then
                        echo "${RIKERAPP_SERVER_DESC} did not stop in time. PID file was not removed. To aid diagnostics a thread dump has been written to standard out."
                        kill -3 `cat "$RIKERAPP_SERVER_PID"`
                    fi
                fi
                SLEEP=`expr $SLEEP - 1 `
            done
        fi
    fi
    KILL_SLEEP_INTERVAL=5
    if [ $FORCE -eq 1 ]; then
        if [ -z "$RIKERAPP_SERVER_PID" ]; then
            echo "Kill failed: \$RIKERAPP_SERVER_PID not set"
        else
            if [ -f "$RIKERAPP_SERVER_PID" ]; then
                PID=`cat "$RIKERAPP_SERVER_PID"`
                echo "Killing ${RIKERAPP_SERVER_DESC} with the PID: $PID"
                kill -9 $PID
                while [ $KILL_SLEEP_INTERVAL -ge 0 ]; do
                    kill -0 `cat "$RIKERAPP_SERVER_PID"` >/dev/null 2>&1
                    if [ $? -gt 0 ]; then
                        rm -f "$RIKERAPP_SERVER_PID" >/dev/null 2>&1
                        if [ $? != 0 ]; then
                            if [ -w "$RIKERAPP_SERVER_PID" ]; then
                                cat /dev/null > "$RIKERAPP_SERVER_PID"
                            else
                                echo "The PID file could not be removed."
                            fi
                        fi
                        # Set this to zero else a warning will be issued about the process still running
                        KILL_SLEEP_INTERVAL=0
                        echo "The ${RIKERAPP_SERVER_DESC} process has been killed."
                        break
                    fi
                    if [ $KILL_SLEEP_INTERVAL -gt 0 ]; then
                        sleep 1
                    fi
                    KILL_SLEEP_INTERVAL=`expr $KILL_SLEEP_INTERVAL - 1 `
                done
                if [ $KILL_SLEEP_INTERVAL -gt 0 ]; then
                    echo "${RIKERAPP_SERVER_DESC} has not been killed completely yet. The process might be waiting on some system call or might be UNINTERRUPTIBLE."
                fi
            fi
        fi
    fi
else
    echo "Usage: rikerapp-server.sh ( commands ... )"
    echo "commands:"
    echo "  start             Start ${RIKERAPP_SERVER_DESC}"
    echo "  stop              Stop ${RIKERAPP_SERVER_DESC}, waiting up to 5 seconds for the process to end"
    echo "  stop n            Stop ${RIKERAPP_SERVER_DESC}, waiting up to n seconds for the process to end"
    echo "  stop -force       Stop ${RIKERAPP_SERVER_DESC}, wait up to 5 seconds and then use kill -KILL if still running"
    echo "  stop n -force     Stop ${RIKERAPP_SERVER_DESC}, wait up to n seconds and then use kill -KILL if still running"
    echo "Note: Waiting for the process to end and use of the -force option require that \$RIKERAPP_SERVER_PID is defined"
    exit 1
fi
