(ns riker-app.rest.resource.user.version.user-res-v001
  (:require [riker-app.rest.user-meta :as meta]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-stripe.common :as stripecommon]
            [clj-stripe.customers :as customers]
            [clj-stripe.subscriptions :as subscriptions]
            [clj-stripe.charges :as charges]
            [pe-core-utils.core :as ucore]
            [riker-app.core.dao :as dao]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.user-validation :as userval]
            [riker-app.rest.user-utils :as userresutils]
            [riker-app.utils :as rutils]
            [riker-app.app.config :as config]
            [riker-app.rest.resource.user.user-res :refer [save-user-validator-fn
                                                           body-data-in-transform-fn
                                                           body-data-out-transform-fn
                                                           save-user-fn
                                                           load-user-fn]]))

(declare validate-receipt)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-user-validator-fn meta/v001
  [version user]
  (userval/save-user-validation-mask user))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   user]
  (identity user))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   user-id
   base-url
   entity-uri-prefix
   entity-uri
   user]
  (userresutils/user-out-transform user))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Save user function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-user-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   plaintext-auth-token ; in case you want to invalidate it
   user
   if-unmodified-since
   trial-expired-grace-period-in-days
   stripe-api-key]
  (let [current-user (:user ctx)
        current-user-email (:user/email current-user)]
    (when (and (not (nil? (:user/hashed-password current-user))) ; does the user even HAVE a password? (could be FB login)
               (or (not (nil? (:user/password user))) ; user is changing their password
                   (not (.equalsIgnoreCase current-user-email ; user is changing their email
                                           (:user/email user)))
                   (:user/cancel-subscription user))) ; user is cancelling subscription
      (let [current-password (:user/current-password user)]
        (if (not (nil? current-password))
          (let [user-ent (usercore/authenticate-user-by-password db-spec
                                                                 current-user-email
                                                                 current-password
                                                                 trial-expired-grace-period-in-days)]
            (when (nil? user-ent)
              (throw (IllegalArgumentException. (str userval/su-current-password-incorrect)))))
          (throw (IllegalArgumentException. (str userval/su-current-password-not-provided))))))
    (if (and (:user/cancel-subscription user)
             (nil? (:user/paid-enrollment-cancelled-at current-user))
             (nil? (:user/final-failed-payment-attempt-occurred-at current-user)))
      (do
        ; in this context, the "user" object is not really the user; it only
        ; contains 1 relevant key, :user/paid-enrollment-cancelled-reason. So we
        ; use the "current-user" object for most things.  Down below, we do
        ; merge the "user" object when saving the user so we persist the
        ; provided cancel reason.
        (let [last-invoice-amount (:user/last-invoice-amount current-user)
              days-since-last-invoice (t/in-days (t/interval (:user/last-invoice-at current-user) (t/now)))
              refund-amount (int (- last-invoice-amount (* (/ last-invoice-amount 365) days-since-last-invoice)))
              last-charge-id (:user/last-charge-id current-user)
              unsubscribe-result (stripecommon/with-token stripe-api-key
                                   (stripecommon/execute (subscriptions/unsubscribe-customer (stripecommon/customer (:user/stripe-customer-id current-user))
                                                                                             (subscriptions/immediately))))]
          (log/debug "refund-amount: [" refund-amount "], last-charge-id: [" last-charge-id "]")
          (if (not (clojure.string/blank? last-charge-id))
            (let [refund-result (stripecommon/with-token stripe-api-key
                                  (stripecommon/execute (charges/create-refund last-charge-id
                                                                               (stripecommon/amount refund-amount))))]
              (when (contains? refund-result :error)
                (do
                  (log/error "Stripe error attempting to process refund" (:error refund-result))
                  (try
                    (usercore/send-email config/r-need-to-manually-process-refund-email-template
                                         (-> current-user
                                             (assoc :message "Stripe error attempting to process refund")
                                             (assoc :stripe-result refund-result)
                                             (assoc :refund-amount (rutils/format-pennies-amount refund-amount)))
                                         config/r-need-to-manually-process-refund-email-subject
                                         config/r-support-email-address
                                         config/r-support-email-address
                                         config/r-domain
                                         config/r-mailgun-api-key)
                    (catch Exception e
                      (log/error e "Exception attempting to send [need-to-manually-process-refund-email] email"))))))
            (do ; this can happen if the user immediately cancels their account
                ; after creating it, and the Stripe 'charge.succeeded' webhook
                ; didn't come in yet, and thus the 'last_charge_id' column of
                ; the user account row is null.
              (log/error "Unknown charge ID at time of refund request")
              (try
                (usercore/send-email config/r-need-to-manually-process-refund-email-template
                                     (-> current-user
                                         (assoc :message "Unknown charge ID at time of refund request")
                                         (assoc :refund-amount (rutils/format-pennies-amount refund-amount)))
                                     config/r-need-to-manually-process-refund-email-subject
                                     config/r-support-email-address
                                     config/r-support-email-address
                                     config/r-domain
                                     config/r-mailgun-api-key)
                (catch Exception e
                  (log/error e "Exception attempting to send [need-to-manually-process-refund-email] email")))))
          (when (contains? unsubscribe-result :error)
            (do
              (log/error "Stripe error attempting to unsubscribe user" (:error unsubscribe-result))
              (try
                (usercore/send-email config/r-need-to-manually-unsubscribe-user-email-template
                                     (assoc current-user :stripe-result unsubscribe-result)
                                     config/r-need-to-manually-unsubscribe-user-email-subject
                                     config/r-support-email-address
                                     config/r-support-email-address
                                     config/r-domain
                                     config/r-mailgun-api-key)
                (catch Exception e
                  (log/error e "Exception attempting to send [need-to-manually-unsubscribe-user-email] email")))))
          (do
            (try
              (usercore/send-email config/r-subscription-cancelled-with-refund-email-mustache-template
                                   (-> current-user
                                       (assoc :refund-amount (rutils/format-pennies-amount refund-amount)))
                                   config/r-subscription-cancelled-email-subject
                                   config/r-support-email-address
                                   current-user-email
                                   config/r-domain
                                   config/r-mailgun-api-key)
              (catch Exception e
                (log/error e "Exception attempting to send [subscription-cancelled-with-refund-email] email")))
            (usercore/save-user db-spec
                                user-id
                                (merge user {:user/paid-enrollment-cancelled-at (t/now)})))))
      (let [receipt-data (:user/app-store-receipt-data-base64 user)]
        (if (not (nil? receipt-data)) ; user is enrolling using iOS IAP
          (let [now (t/now)]
            (letfn [(enroll-user-iap [addl-attrs]
                      ; we'll verify the user's valid subscription each month
                      ; (perhaps overkill, but will cover case if user has
                      ; Apple customer support cancel the subscription) - by
                      ; doing a monthly check, a user will at most get a month
                      ; free if they cancel at just the right time
                      (let [now (t/now)
                            save-result (usercore/save-user db-spec user-id (merge user
                                                                                   addl-attrs
                                                                                   {:user/validate-app-store-receipt-at (t/plus (:user/paid-enrollment-established-at addl-attrs)
                                                                                                                                (t/months 1))
                                                                                    :user/is-payment-past-due false
                                                                                    :user/paid-enrollment-cancelled-at nil
                                                                                    :user/deleted-at nil
                                                                                    :user/final-failed-payment-attempt-occurred-at nil}))]
                        (try
                          (usercore/send-email config/r-subscription-established-iap-email-template
                                               user
                                               config/r-subscription-established-email-subject
                                               config/r-support-email-address
                                               (:user/email user)
                                               config/r-domain
                                               config/r-mailgun-api-key)
                          (catch Exception e
                            (log/error e "Exception attempting to send [subscription-established-iap-email] email")))
                        save-result))]
              (let [validation-url (:user/app-store-receipt-validation-url current-user)
                    validation-url (if (nil? validation-url) config/r-app-store-receipt-validation-url validation-url)
                    iap-receipts (rutils/iap-latest-receipt-info receipt-data validation-url)
                    latest-receipt (last iap-receipts)]
                (if (not (nil? latest-receipt))
                  (let [purchased-date (rutils/parse-iap-date (get latest-receipt "purchase_date"))]
                    (enroll-user-iap {:user/paid-enrollment-established-at purchased-date}))
                  (enroll-user-iap {:user/paid-enrollment-established-at now}))))) ; if we get null response, assume the receipt is good and proceed with 'now'
          (do
            (let [save-result (usercore/save-user db-spec user-id plaintext-auth-token user if-unmodified-since)]
              (letfn [(send-notice [template]
                        (try
                          (usercore/send-email template
                                               {:original-email (:user/email current-user)
                                                :new-email (:user/email user)}
                                               config/r-account-updated-email-subject
                                               config/r-support-email-address
                                               (:user/email current-user)
                                               config/r-domain
                                               config/r-mailgun-api-key)
                          (catch Exception e
                            (log/error e (str "Exception attempting to send email[" template "] email"))))
                        save-result)]
                (if (not (nil? (:user/password user)))
                  (if (not (.equalsIgnoreCase current-user-email
                                              (:user/email user)))
                    (send-notice config/r-account-email-and-password-changed-email-template)
                    (send-notice config/r-account-password-changed-email-template))
                  (if (not (.equalsIgnoreCase current-user-email
                                                (:user/email user)))
                    (send-notice config/r-account-email-changed-email-template)
                    save-result))))))))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Load user function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod load-user-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   plaintext-auth-token
   if-modified-since]
  (usercore/load-user-by-id db-spec user-id true))
