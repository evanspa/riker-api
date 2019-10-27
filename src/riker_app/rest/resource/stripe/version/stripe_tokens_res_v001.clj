(ns riker-app.rest.resource.stripe.version.stripe-tokens-res-v001
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [clj-stripe.common :as stripecommon]
            [clj-stripe.customers :as customers]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.app.config :as config]
            [riker-app.rest.user-utils :as userresutils]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.stripe.stripe-token-utils :as stripetokenresutils]
            [riker-app.rest.resource.stripe.stripe-tokens-res :refer [new-stripe-token-validator-fn
                                                                            body-data-in-transform-fn
                                                                            body-data-out-transform-fn
                                                                            process-new-stripe-token-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod new-stripe-token-validator-fn meta/v001
  [version stripe-token]
  0)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   new-stripe-token]
  (stripetokenresutils/stripe-token-in-transform new-stripe-token))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   user-id
   base-url
   entity-uri-prefix
   entity-uri
   updated-user]
  (userresutils/user-out-transform updated-user))

(def stripe-card-errors
  {"invalid_number"       "0"
   "invalid_expiry_month" "1"
   "invalid_expiry_year"  "2"
   "invalid_cvc"          "3"
   "incorrect_number"     "4"
   "expired_card"         "5"
   "incorrect_cvc"        "6"
   "incorrect_zip"        "7"
   "card_declined"        "8"
   "missing"              "9"
   "processing_error"     "10"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Save new stripe token function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod process-new-stripe-token-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   base-url
   entity-uri-prefix
   stripe-tokens-uri
   plaintext-auth-token
   stripe-token
   merge-embedded-fn
   merge-links-fn
   stripe-api-key
   stripe-subscription-name]
  (letfn [(error-check [result process-fn]
            (if (contains? result :error)
              (let [error (:error result)
                    type (:type error)
                    code (:code error)
                    code-num-str (get stripe-card-errors code)]
                (log/error "stripe error: " error)
                (if (and (= type "card_error")
                         (not (nil? code-num-str)))
                  (throw (IllegalArgumentException. code-num-str))
                  (throw (ex-info nil {:cause :unknown}))))
              (if (contains? result :id)
                (let [[_ saved-user] (process-fn result)]
                  {:status 200 :do-entity saved-user})
                (throw (ex-info nil {:cause :unknown})))))
          (process-customer-enroll [result]
            (let [card (:card stripe-token)
                  now (t/now)
                  invoice-amount (get-in (first (get-in result [:subscriptions :data])) [:plan :amount])]
                                        ; so yeah, for the sake of simplicity, I'm "guessing" for the 2 'next-'
                                        ; values (next-invoice-at and next-invoice-amount), and I'm also using wishful thinking that the user's card was
                                        ; charged successfully.  The alternate is too complicated/hard (wait
                                        ; to receive webhook call from stripe saying the user's invoice was
                                        ; paid, and THEN return a response to this call?).  Because "result"
                                        ; is valid, that means the card is "valid", and so, I'm confident that
                                        ; 99.99% of the time, the invoice will be paid, and I'll get that
                                        ; webhook call.  But for now, for this call, I'll assume everything's
                                        ; good.  If later, I get a webhook call saying the invoice was NOT
                                        ; paid, then, I'll suspend the user's account, and they'll have a
                                        ; grace period to fix it.
              (let [current-user (:user ctx)
                    save-result (usercore/save-user db-spec user-id {:user/stripe-customer-id (:id result)
                                                                     :user/latest-stripe-token-id (:id stripe-token)
                                                                     :user/paid-enrollment-established-at now
                                                                     :user/is-payment-past-due false
                                                                     :user/next-invoice-amount invoice-amount
                                                                     :user/next-invoice-at (t/plus now (t/years 1))
                                                                     :user/last-invoice-at now
                                                                     :user/last-invoice-amount invoice-amount
                                                                     :user/current-card-last4 (:last4 card)
                                                                     :user/current-card-brand (:brand card)
                                                                     :user/current-card-exp-month (:exp_month card)
                                                                     :user/current-card-exp-year (:exp_year card)
                                                                     :user/paid-enrollment-cancelled-at nil
                                                                     :user/validate-app-store-receipt-at nil
                                                                     :user/deleted-at nil
                                                                     :user/final-failed-payment-attempt-occurred-at nil})]
                (usercore/send-email config/r-subscription-established-stripe-email-template
                                     current-user
                                     config/r-subscription-established-email-subject
                                     config/r-support-email-address
                                     (:user/email current-user)
                                     config/r-domain
                                     config/r-mailgun-api-key)
                save-result)))
          (process-payment-method-update [result]
            (let [card (:card stripe-token)
                  current-user (:user ctx)
                  save-result (usercore/save-user db-spec user-id {:user/latest-stripe-token-id (:id stripe-token)
                                                                   :user/is-payment-past-due false
                                                                   :user/current-card-last4 (:last4 card)
                                                                   :user/current-card-brand (:brand card)
                                                                   :user/current-card-exp-month (:exp_month card)
                                                                   :user/current-card-exp-year (:exp_year card)
                                                                   :user/paid-enrollment-cancelled-at nil
                                                                   :user/validate-app-store-receipt-at nil
                                                                   :user/deleted-at nil
                                                                   :user/final-failed-payment-attempt-occurred-at nil})]
              (usercore/send-email config/r-stripe-payment-info-updated-email-template
                                   current-user
                                   config/r-stripe-payment-info-updated-email-subject
                                   config/r-support-email-address
                                   (:user/email current-user)
                                   config/r-domain
                                   config/r-mailgun-api-key)
              save-result))]
    (let [user (:user ctx)
          stripe-customer-id (:user/stripe-customer-id user)]
      (if (not (nil? stripe-customer-id))
        (let [result (stripecommon/with-token stripe-api-key
                       (stripecommon/execute (customers/update-customer stripe-customer-id
                                                                        (stripecommon/card (:id stripe-token))
                                                                        (customers/email (:email stripe-token))
                                                                        (stripecommon/plan stripe-subscription-name))))]
          (if (and (nil? (:user/paid-enrollment-cancelled-at user))
                   (nil? (:user/final-failed-payment-attempt-occurred-at user)))
            (error-check result process-payment-method-update)
            (error-check result process-customer-enroll))) ; re-enroll
        (let [result (stripecommon/with-token stripe-api-key
                       (stripecommon/execute (customers/create-customer (stripecommon/card (:id stripe-token))
                                                                        (customers/email (:email stripe-token))
                                                                        (stripecommon/plan stripe-subscription-name))))]
          (error-check result process-customer-enroll))))))
