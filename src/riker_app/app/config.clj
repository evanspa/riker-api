(ns riker-app.app.config
  (:require [clojure.java.jdbc :as j]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [ring.util.codec :refer [url-encode]]
            [riker-app.core.user-dao :as usercore]
            [riker-app.rest.user-meta :as usermeta])
  (:import com.mchange.v2.c3p0.ComboPooledDataSource))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; URI prefix
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-entity-uri-prefix (env :r-uri-prefix))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 'Authorization' header parts
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-auth-scheme            "r-auth")
(def r-auth-scheme-param-name "r-token")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Media sub-type prefix
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-mt-subtype-prefix "vnd.riker.")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Header names
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def rhdr-establish-session       "r-establish-session")
(def rhdr-auth-token              "r-auth-token")
(def rhdr-error-mask              "r-error-mask")
(def rhdr-if-unmodified-since     "r-if-unmodified-since")
(def rhdr-if-modified-since       "r-if-modified-since")
(def rhdr-login-failed-reason     "r-login-failed-reason")
(def rhdr-delete-reason           "r-delete-reason")
(def rhdr-desired-embedded-format "r-desired-embedded-format")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Default user settings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-default-user-setting-size-uom 0) ; 0=in, 1=cm
(def r-default-user-setting-distance-uom 0) ; 0=mi, 1=km
(def r-default-user-setting-weight-uom   0) ; 0=lb, 1=kg
(def r-default-user-setting-weight-inc-dec-amount 5)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Types of 'embedded' resource structerings
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def id-keyed-embedded-format "id-keyed")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; REPL port number
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-nrepl-server-port (Integer/parseInt (env :r-nrepl-server-port)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Application version and config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-base-url    (env :r-base-url))
(def r-app-version (env :r-app-version))
(def r-domain      (env :r-domain))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Database config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-db-name           (env :r-db-name))
(def r-db-server-host    (env :r-db-server-host))
(def r-db-server-port    (Integer/parseInt (env :r-db-server-port)))
(def r-db-username       (env :r-db-username))
(def r-db-password       (env :r-db-password))
(def r-jdbc-driver-class (env :r-db-driver-class))
(def r-jdbc-subprotocol  (env :r-jdbc-subprotocol))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cookie config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-login-cookie-secure (Boolean/parseBoolean (env :r-login-cookie-secure)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Stripe Config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-stripe-secret-key (env :r-stripe-secret-key))
(def r-stripe-subscription-name (env :r-stripe-subscription-name))
(def r-stripe-webhook-secret-path-comp (env :r-stripe-webhook-secret-path-comp))
(def r-invoice-payment-failed-email-subject "Riker subscription payment failed")
(def r-invoice-payment-failed-email-mustache-template "invoice-payment-failed.html.mustache")
(def r-invoice-payment-failed-final-attempt-email-subject "Riker subscription payment failed - final attempt")
(def r-invoice-payment-failed-final-attempt-email-mustache-template "invoice-payment-failed-final-attempt.html.mustache")
(def r-subscription-cancelled-with-refund-email-mustache-template "subscription-cancelled-with-refund.html.mustache")
(def r-subscription-cancelled-email-subject "Riker subscription cancelled")
(def r-subscription-expired-email-subject "Riker subscription expired")
(def r-subscription-amount (Integer/parseInt (env :r-subscription-amount)))
(def r-stripe-payment-info-updated-email-template "stripe-payment-info-updated.html.mustache")
(def r-stripe-payment-info-updated-email-subject "Riker subscription payment info updated")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Facebook Config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-facebook-app-secret (env :r-facebook-app-secret))
(def r-facebook-callback-secret-path-comp (env :r-facebook-callback-secret-path-comp))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Apple / App Store Config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-app-store-receipt-validation-url (env :r-app-store-receipt-validation-url))
(def r-app-store-receipt-validation-shared-secret (env :r-app-store-receipt-validation-shared-secret))
(def r-iap-subscription-cancelled-email-mustache-template "iap-subscription-cancelled.html.mustache")
(def r-apple-search-ads-attribution-upload-secret-path-comp (env :r-apple-search-ads-attribution-upload-secret-path-comp))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Trial period and other related config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-trial-period-in-days (Integer/parseInt (env :r-trial-period-in-days)))
(def r-trial-expired-grace-period-in-days (Integer/parseInt (env :r-trial-period-expired-grace-period-in-days)))
(def r-trial-almost-expires-threshold-in-days (Integer/parseInt (env :r-trial-almost-expires-threshold-in-days)))
(def r-trial-period-almost-expired-notice-email-subject "Riker trial almost expired")
(def r-trial-period-almost-expired-notice-email-template "trial-almost-expired.html.mustache")
(def r-enrollment-web-url (str r-base-url "/authenticatedEnrollSynchronize"))
(def r-update-payment-info-web-url (str r-base-url "/updatePaymentMethodSynchronize"))
(def r-trial-and-grace-expired-account-deleted-email-subject "Riker account closed")
(def r-trial-and-grace-expired-account-deleted-email-template "trial-and-expired-account-deleted.html.mustache")
(def r-subscription-established-stripe-email-template "subscription-established-stripe.html.mustache")
(def r-subscription-established-iap-email-template "subscription-established-iap.html.mustache")
(def r-subscription-established-email-subject "Riker account established")

(def r-account-email-and-password-changed-email-template "email-and-password-changed.html.mustache")
(def r-account-email-changed-email-template "email-changed.html.mustache")
(def r-account-password-changed-email-template "password-changed.html.mustache")
(def r-account-updated-email-subject "Riker account updated")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Email config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-email-live-mode (Boolean/parseBoolean (env :r-email-live-mode)))
(alter-var-root (var usercore/*email-live-mode*) (fn [_] r-email-live-mode))
(def r-mailgun-api-key (env :r-mailgun-api-key))
(def r-support-email-address "Riker <support@rikerapp.com>")
(def r-stats-email-address "Riker Stats <stats@rikerapp.com>")
(def r-aggregate-stats-email-subject "Riker Aggregate Stats")
(def r-aggregate-stats-email-template "aggregate-stats.html.mustache")
(def r-need-to-manually-process-refund-email-template "need-to-manually-process-refund.html.mustache")
(def r-need-to-manually-process-refund-email-subject "Need to manually process refund")
(def r-need-to-manually-unsubscribe-user-email-template "need-to-manually-unsubscribe-user.html.mustache")
(def r-need-to-manually-unsubscribe-user-email-subject "Need to manually unsubscribe user")
(def r-new-stripe-customer-notification-email-template "new-stripe-customer-created.html.mustache")
(def r-new-stripe-customer-notification-email-subject "New Stripe customer created")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New user welcome and verification config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-welcome-and-verification-email-mustache-template "welcome-and-account-verification.html.mustache")
(def r-verification-email-mustache-template             "account-verification.html.mustache")
(def r-welcome-and-verification-email-subject-line      "Welcome to Riker")
(def r-verification-email-subject-line                  "Riker [account verification]")
(def r-verification-success-uri                         "/verificationSuccess")
(def r-verification-error-uri                           "/verificationError")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; New user internal-notification config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def new-user-notification-mustache-template "new-signup-notification.html.mustache")
(def new-user-notification-from-email        (env :r-new-user-notification-from-email))
(def new-user-notification-to-email          (env :r-new-user-notification-to-email))
(def new-user-notification-subject           (env :r-new-user-notification-subject))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Error internal-notification config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def err-notification-mustache-template "err-notification.html.mustache")
(def err-subject                        (env :r-err-notification-subject))
(def err-from-email                     (env :r-err-notification-from-email))
(def err-to-email                       (env :r-err-notification-to-email))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Omg, more config
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(log/debug "(env :r-max-allowed-set-import): " (env :r-max-allowed-set-import))
(def max-allowed-set-import (Integer/parseInt (env :r-max-allowed-set-import)))
(def max-allowed-bml-import (Integer/parseInt (env :r-max-allowed-bml-import)))

(defn r-verification-url-maker
  [email verification-token]
  (str r-base-url
       r-entity-uri-prefix
       usermeta/pathcomp-users
       "/"
       (url-encode email)
       "/"
       usermeta/pathcomp-verification
       "/"
       verification-token))

(defn r-verification-flagged-url-maker
  [email verification-token]
  (str r-base-url
       r-entity-uri-prefix
       usermeta/pathcomp-users
       "/"
       (url-encode email)
       "/"
       usermeta/pathcomp-verification-flagged
       "/"
       verification-token))

(def r-verification-success-web-url (str r-base-url r-verification-success-uri))
(def r-verification-error-web-url   (str r-base-url r-verification-error-uri))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Password reset related
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-password-reset-email-subject-line      "Riker [password reset]")
(def r-password-reset-email-mustache-template "password-reset.html.mustache")

(defn r-prepare-password-reset-url-maker
  [email password-reset-token]
  (str r-base-url
       r-entity-uri-prefix
       usermeta/pathcomp-users
       "/"
       (url-encode email)
       "/"
       usermeta/pathcomp-prepare-password-reset
       "/"
       password-reset-token))

(defn r-password-reset-web-url-maker
  [email password-reset-token]
  (str r-base-url
       "/"
       usermeta/pathcomp-users
       "/"
       (url-encode email)
       "/"
       usermeta/pathcomp-password-reset
       "/"
       password-reset-token))

(def r-password-reset-error-web-uri "/passwordResetPrepareError")
(def r-password-reset-error-web-url (str r-base-url r-password-reset-error-web-uri))

(defn r-password-reset-flagged-url-maker
  [email password-reset-token]
  (str r-base-url
       r-entity-uri-prefix
       usermeta/pathcomp-users
       "/"
       (url-encode email)
       "/"
       usermeta/pathcomp-password-reset-flagged
       "/"
       password-reset-token))

(defn db-spec-fn
  ([]
   (db-spec-fn nil))
  ([db-name]
   (let [subname-prefix (format "//%s:%s/" r-db-server-host r-db-server-port)
         db-spec {:classname r-jdbc-driver-class
                  :subprotocol r-jdbc-subprotocol
                  :subname (if db-name
                             (str subname-prefix db-name)
                             subname-prefix)
                  :user r-db-username
                  :password r-db-password}]
     db-spec)))

(def db-spec-without-db
  (with-meta
    (db-spec-fn nil)
    {:subprotocol r-jdbc-subprotocol}))

(def db-spec
  (with-meta
    (db-spec-fn r-db-name)
    {:subprotocol r-jdbc-subprotocol}))

(def ^:dynamic pooled-db-spec
  (with-meta
    (let [db-spec (db-spec-fn r-db-name)
          cpds (doto (ComboPooledDataSource.)
                 (.setDriverClass (:classname db-spec))
                 (.setJdbcUrl (str "jdbc:" (:subprotocol db-spec) ":" (:subname db-spec)))
                 (.setUser (:user db-spec))
                 (.setPassword (:password db-spec))
                 ;; expire excess connections after 30 minutes of inactivity:
                 #_(.setMaxIdleTimeExcessConnections (* 30 60))
                 ;; expire connections after 3 hours of inactivity:
                 #_(.setMaxIdleTime (* 3 60 60)))]
      {:datasource cpds})
    {:subprotocol r-jdbc-subprotocol}))
