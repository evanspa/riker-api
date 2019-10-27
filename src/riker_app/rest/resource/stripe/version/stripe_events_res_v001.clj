(ns riker-app.rest.resource.stripe.version.stripe-events-res-v001
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [clojure.java.jdbc :as j]
            [pe-core-utils.core :as ucore]
            [clj-stripe.common :as stripecommon]
            [clj-stripe.customers :as customers]
            [clj-stripe.invoices :as invoices]
            [clj-stripe.subscriptions :as subscriptions]
            [clj-stripe.charges :as charges]
            [riker-app.utils :as rutils]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.core.user-ddl :as uddl]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.stripe.stripe-events-res :refer [new-stripe-event-validator-fn
                                                                      body-data-in-transform-fn
                                                                      body-data-out-transform-fn
                                                                      process-new-stripe-event-fn]]
            [riker-app.app.config :as config]))

(declare process-stripe-event)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod new-stripe-event-validator-fn meta/v001
  [version stripe-event]
  0)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version new-stripe-event]
  (identity new-stripe-event))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- from-stripe-long
  [stripe-date-long]
  (c/from-long (Long. (* 1000 stripe-date-long))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Save new stripe token function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod process-new-stripe-event-fn meta/v001
  [ctx
   version
   db-spec
   base-url
   entity-uri-prefix
   stripe-events-uri
   plaintext-auth-token
   stripe-event
   merge-embedded-fn
   merge-links-fn
   invoice-payment-failed-email-subject
   invoice-payment-failed-email-mustache-template]
  (try
    (process-stripe-event db-spec
                          stripe-event
                          invoice-payment-failed-email-subject
                          invoice-payment-failed-email-mustache-template)
    {:status 204}
    (catch Exception e
      (rutils/log-e e (str "Exception processing stripe webhook event.  Stripe event: " stripe-event))
      {:status 500})))

(defmulti process-stripe-event
  (fn [db-spec
       stripe-event
       invoice-payment-failed-email-subject
       invoice-payment-failed-email-mustache-template]
    (:type stripe-event)))

(defmethod process-stripe-event :default
  [db-spec
   stripe-event
   invoice-payment-failed-email-subject
   invoice-payment-failed-email-mustache-template]
  (let [type (:type stripe-event)
        stripe-customer-id (get-in stripe-event [:data :object :customer])]
    (log/info (str "Received [" type "] Webhook for Stripe customer ID [" stripe-customer-id "].  Riker takes no action on this event."))))

(defmethod process-stripe-event "customer.created"
  [db-spec
   stripe-event
   invoice-payment-failed-email-subject
   invoice-payment-failed-email-mustache-template]
  (let [stripe-customer-id (get-in stripe-event [:data :object :id])
        user-email (get-in stripe-event [:data :object :email])]
    (log/info (str "Received [customer.created] Webhook for Stripe customer ID [" stripe-customer-id "], email: [" user-email "].  Riker takes no action on this event."))
    (usercore/send-email config/r-new-stripe-customer-notification-email-template
                         {:user-email user-email :stripe-customer-id stripe-customer-id}
                         config/r-new-stripe-customer-notification-email-subject
                         config/new-user-notification-to-email
                         config/new-user-notification-to-email
                         config/r-domain
                         config/r-mailgun-api-key)))

(defmethod process-stripe-event "invoice.payment_succeeded"
  [db-spec
   stripe-event
   invoice-payment-failed-email-subject
   invoice-payment-failed-email-mustache-template]
  (let [stripe-customer-id (get-in stripe-event [:data :object :customer])
        [loaded-user-entid loaded-user-ent] (usercore/load-user-by-stripe-customer-id db-spec stripe-customer-id)]
    (if (not (nil? loaded-user-ent))
      (let [enrollment-est-at (:user/paid-enrollment-established-at loaded-user-ent)
            invoice-date (from-stripe-long (get-in stripe-event [:data :object :date]))]
        ; only update the user for invoice #2 and beyond (the 'within' check
        ; below is used to determine if this invoice-paid event is for the
        ; user's initial invoice - which is created and paid pretty much simultaneously)
        (when (not (t/within? (t/interval enrollment-est-at (t/plus enrollment-est-at (t/hours 4)))
                              invoice-date))
          (let [next-invoice (stripecommon/with-token config/r-stripe-secret-key
                               (stripecommon/execute (invoices/get-upcoming-invoice (stripecommon/customer (:user/stripe-customer-id loaded-user-ent)))))]
            (letfn [(update-user [next-invoice-at next-invoice-amount]
                      (let [[user-id saved-user-ent] (usercore/save-user db-spec
                                                                         loaded-user-entid
                                                                         {:user/is-payment-past-due false
                                                                          :user/last-invoice-at invoice-date
                                                                          :user/last-invoice-amount (get-in stripe-event [:data :object :amount_due])
                                                                          :user/next-invoice-at next-invoice-at
                                                                          :user/next-invoice-amount next-invoice-amount
                                                                          :user/paid-enrollment-cancelled-at nil
                                                                          :user/deleted-at nil
                                                                          :user/final-failed-payment-attempt-occurred-at nil})]))]
              (if (and (not (nil? next-invoice))
                       (not (contains? next-invoice :error)))
                (update-user (from-stripe-long (:date next-invoice)) (:total next-invoice))
                (let [stripe-error-msg (if (not (nil? next-invoice)) (get-in next-invoice [:error :message]) "none")]
                  (log/info (str "There was an error attempting to fetch the customer's ["
                                 stripe-customer-id
                                 "] next invoice; proceeding to update the user's record with manually-"
                                 "calculated next-invoice-at and next-invoice-amount values.  "
                                 "Error message from Stripe: [" stripe-error-msg "]."))
                  (update-user (t/plus (t/now) (t/years 1)) config/r-subscription-amount)
                  (throw (Exception. (str "Error attempting to fetch customer's ["
                                          stripe-customer-id
                                          "] next invoice.  "
                                          "Error message from Stripe: [" stripe-error-msg "].")))))))))
      (do
        (log/info (str "Received [invoice.payment_succeeded] web hook, but the customer ID cannot be found."
                       "This probably means the user immediately cancelled their account after signing up."))
        (throw (IllegalArgumentException. "unknown customer id processing stripe event"))))))

(defmethod process-stripe-event "charge.succeeded"
  [db-spec
   stripe-event
   invoice-payment-failed-email-subject
   invoice-payment-failed-email-mustache-template]
  (let [type (:type stripe-event)
        stripe-customer-id (get-in stripe-event [:data :object :customer])
        [loaded-user-entid loaded-user-ent] (usercore/load-user-by-stripe-customer-id db-spec stripe-customer-id)
        stripe-charge-id (get-in stripe-event [:data :object :id])]
    (if (not (nil? loaded-user-ent))
      (do
        (usercore/save-user db-spec
                            loaded-user-entid
                            {:user/last-charge-id stripe-charge-id
                             :user/paid-enrollment-cancelled-at nil
                             :user/is-payment-past-due false
                             :user/deleted-at nil
                             :user/final-failed-payment-attempt-occurred-at nil})
        (log/info (str "Received [" type "] Webhook event for Stripe customer ID [" stripe-customer-id "] and processed it successfully. "
                       "User account with ID [" loaded-user-entid "] updated successfully.")))
      (log/info (str "Received [" type "] Webhook event for Stripe customer ID [" stripe-customer-id "], however there is currently "
                     "no user account with that customer ID.  This is not impossible.  If a user signs up, immediately "
                     "cancels and then signs up again, their customer ID is overwritten with a new one.  Delayed web hook calls for "
                     "the original customer ID will not be correlated to a user in the user account table.")))))

(defmethod process-stripe-event "invoice.payment_failed"
  [db-spec
   stripe-event
   invoice-payment-failed-email-subject
   invoice-payment-failed-email-mustache-template]
  (let [type (:type stripe-event)
        stripe-customer-id (get-in stripe-event [:data :object :customer])
        next-payment-attempt (get-in stripe-event [:data :object :next_payment_attempt])
        [loaded-user-entid loaded-user-ent] (usercore/load-user-by-stripe-customer-id db-spec stripe-customer-id)]
    (if (not (nil? loaded-user-ent))
      (if (nil? next-payment-attempt)
        (do
          (let [result (stripecommon/with-token config/r-stripe-secret-key
                         (stripecommon/execute (subscriptions/unsubscribe-customer (stripecommon/customer stripe-customer-id)
                                                                                   (subscriptions/immediately))))]
            (when (contains? result :error)
              (do
                (log/error "Stripe error attempting to unsubscribe user" (:error result))
                (usercore/send-email config/r-need-to-manually-unsubscribe-user-email-template
                                     (assoc loaded-user-ent :stripe-result result)
                                     config/r-need-to-manually-unsubscribe-user-email-subject
                                     config/r-support-email-address
                                     config/r-support-email-address
                                     config/r-domain
                                     config/r-mailgun-api-key))))
          (let [[user-id saved-user-ent] (usercore/save-user db-spec
                                                             loaded-user-entid
                                                             {:user/final-failed-payment-attempt-occurred-at (t/now)})]
            (usercore/send-email config/r-invoice-payment-failed-final-attempt-email-mustache-template
                                 (merge saved-user-ent
                                        {:amount-in-usd (rutils/format-pennies-amount (get-in stripe-event [:data :object :total]))})
                                 config/r-invoice-payment-failed-final-attempt-email-subject
                                 config/r-support-email-address
                                 (:user/email saved-user-ent)
                                 config/r-domain
                                 config/r-mailgun-api-key)
            (log/info (str "Received [" type "] Webhook event for Stripe customer ID [" stripe-customer-id "] and processed it successfully. "
                           "User account with ID [" loaded-user-entid "] updated successfully (invoice payment failed final attempt)."))))
        (do
          (let [[user-id saved-user-ent] (usercore/save-user db-spec loaded-user-entid {:user/is-payment-past-due true})]
            (usercore/send-email invoice-payment-failed-email-mustache-template
                                 (merge saved-user-ent
                                        {:amount-in-usd (rutils/format-pennies-amount (get-in stripe-event [:data :object :total]))
                                         :payment-update-web-url config/r-update-payment-info-web-url
                                         :next-payment-attempt (f/unparse (f/formatters :year-month-day)
                                                                          (from-stripe-long next-payment-attempt))})
                                 invoice-payment-failed-email-subject
                                 config/r-support-email-address
                                 (:user/email saved-user-ent)
                                 config/r-domain
                                 config/r-mailgun-api-key)
            (log/info (str "Received [" type "] Webhook event for Stripe customer ID [" stripe-customer-id "] and processed it successfully. "
                           "User account with ID [" loaded-user-entid "] updated successfully (invoice payment attempt failed).")))))
      (throw (IllegalArgumentException. "unknown customer id processing stripe event")))))
