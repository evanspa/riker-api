(ns riker-app.core.user-dao
  (:require [pe-core-utils.core :as ucore]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as j]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [cemerick.friend.credentials :refer [hash-bcrypt bcrypt-verify]]
            [clj-mailgun.core :as mailgun]
            [clostache.parser :as clostache]
            [riker-app.core.jdbc :as jcore]
            [riker-app.core.user-validation :as val]
            [riker-app.core.user-ddl :as uddl]))

(declare load-user-by-id-with-maintenance-status)
(declare load-verification-token-by-plaintext-token)
(declare load-password-reset-token-by-plaintext-token)
(declare create-and-save-verification-token)
(declare create-and-save-password-reset-token)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn next-user-account-id
  [db-spec]
  (jcore/seq-next-val db-spec "user_account_id_seq"))

(defn next-auth-token-id
  [db-spec]
  (jcore/seq-next-val db-spec "authentication_token_id_seq"))

(defn next-verification-token-id
  [db-spec]
  (jcore/seq-next-val db-spec "account_verification_token_id_seq"))

(defn next-password-reset-token-id
  [db-spec]
  (jcore/seq-next-val db-spec "password_reset_token_id_seq"))

(defn rs->user
  [user-rs]
  (let [from-sql-time-fn #(c/from-sql-time %)]
    [(:id user-rs)
     (-> user-rs
         (ucore/replace-if-contains :name                           :user/name)
         (ucore/replace-if-contains :email                          :user/email)
         (ucore/replace-if-contains :username                       :user/username)
         (ucore/replace-if-contains :id                             :user/id)
         (ucore/replace-if-contains :updated_count                  :user/updated-count)
         (ucore/replace-if-contains :hashed_password                :user/hashed-password)
         (ucore/replace-if-contains :next_invoice_at                :user/next-invoice-at                from-sql-time-fn)
         (ucore/replace-if-contains :next_invoice_amount            :user/next-invoice-amount)
         (ucore/replace-if-contains :last_invoice_at                :user/last-invoice-at                from-sql-time-fn)
         (ucore/replace-if-contains :last_invoice_amount            :user/last-invoice-amount)
         (ucore/replace-if-contains :stripe_customer_id             :user/stripe-customer-id)
         (ucore/replace-if-contains :last_charge_id                 :user/last-charge-id)
         (ucore/replace-if-contains :latest_stripe_token_id         :user/latest-stripe-token-id)
         (ucore/replace-if-contains :current_card_last4             :user/current-card-last4)
         (ucore/replace-if-contains :current_card_brand             :user/current-card-brand)
         (ucore/replace-if-contains :current_card_exp_month         :user/current-card-exp-month)
         (ucore/replace-if-contains :current_card_exp_year          :user/current-card-exp-year)
         (ucore/replace-if-contains :trial_cancelled_reasons        :user/trial-cancelled-reasons)
         (ucore/replace-if-contains :is_payment_past_due            :user/is-payment-past-due)
         (ucore/replace-if-contains :new_movements_added_at         :user/new-movements-added-at         from-sql-time-fn)
         (ucore/replace-if-contains :informed_of_maintenance_at     :user/informed-of-maintenance-at     from-sql-time-fn)
         (ucore/replace-if-contains :maintenance_starts_at          :user/maintenance-starts-at          from-sql-time-fn)
         (ucore/replace-if-contains :maintenance_duration           :user/maintenance-duration)
         (ucore/replace-if-contains :max_allowed_set_import         :user/max-allowed-set-import)
         (ucore/replace-if-contains :max_allowed_bml_import         :user/max-allowed-bml-import)
         (ucore/replace-if-contains :facebook_user_id               :user/facebook-user-id)
         (ucore/replace-if-contains :current_plan_price             :user/current-plan-price)
         (ucore/replace-if-contains :updated_at                     :user/updated-at                     from-sql-time-fn)
         (ucore/replace-if-contains :deleted_at                     :user/deleted-at                     from-sql-time-fn)
         (ucore/replace-if-contains :verified_at                    :user/verified-at                    from-sql-time-fn)
         (ucore/replace-if-contains :trial_ends_at                  :user/trial-ends-at                  from-sql-time-fn)
         (ucore/replace-if-contains :trial_almost_expired_notice_sent_at :user/trial-almost-expired-notice-sent-at from-sql-time-fn)
         (ucore/replace-if-contains :app_store_receipt_validation_url :user/app-store-receipt-validation-url)
         (ucore/replace-if-contains :app_store_receipt_data_base64  :user/app-store-receipt-data-base64)
         (ucore/replace-if-contains :validate_app_store_receipt_at  :user/validate-app-store-receipt-at  from-sql-time-fn)
         (ucore/replace-if-contains :paid_enrollment_established_at :user/paid-enrollment-established-at from-sql-time-fn)
         (ucore/replace-if-contains :paid_enrollment_cancelled_at   :user/paid-enrollment-cancelled-at   from-sql-time-fn)
         (ucore/replace-if-contains :paid_enrollment_cancelled_reason :user/paid-enrollment-cancelled-reason)
         (ucore/replace-if-contains :final_failed_payment_attempt_occurred_at :user/final-failed-payment-attempt-occurred-at from-sql-time-fn)
         (ucore/replace-if-contains :created_at                     :user/created-at                     from-sql-time-fn))]))

(defn get-schema-version
  [db-spec]
  (let [rs (j/query db-spec
                    [(format "select schema_version from %s" uddl/tbl-schema-version)]
                    {:result-set-fn first})]
    (when rs
      (:schema_version rs))))

(defn set-schema-version
  [db-spec schema-version]
  (j/with-db-transaction [conn db-spec]
    (j/delete! conn :schema_version [])
    (j/insert! conn :schema_version {:schema_version schema-version})))

(defn load-user-by-col
  [db-spec col col-val active-only]
  (jcore/load-entity-by-col db-spec uddl/tbl-user-account col "=" col-val rs->user active-only))

(defn load-user-by-col-with-maintenance-status
  [db-spec col col-val active-only]
  (jcore/load-entity-by-col-three-tables db-spec
                                         uddl/tbl-user-account
                                         uddl/tbl-maintenance-window
                                         uddl/tbl-current-subscription-plan-info
                                         col
                                         "="
                                         col-val
                                         rs->user
                                         active-only))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Auth Token Invalidation Reason codes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def invalrsn-logout                                  0)
(def invalrsn-admin-individual                        1)
(def invalrsn-admin-mass-comp-event                   2)
(def invalrsn-testing                                 3)
(def invalrsn-password-reset                          4)
(def invalrsn-logout-all-other                        5)
(def invalrsn-continue-with-facebook                  6)
(def invalrsn-facebook-deauth                         7)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Account Deleted Reason codes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def delrsn-user-cancelled-subscription  0)
(def delrsn-final-payment-attempt-failed 1)
(def delrsn-trial-and-grace-expired      2)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Loading a user
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn load-user-by-id-if-modified-since
  [db-spec user-id modified-since]
  (let [{users :entities}
        (jcore/entities-modified-since db-spec
                                       uddl/tbl-user-account
                                       "id"
                                       "="
                                       user-id
                                       "updated_at"
                                       "deleted_at"
                                       modified-since
                                       :user/id
                                       :user/deleted-at
                                       :user/updated-at
                                       rs->user)]
    (if (> (count users) 0)
      (load-user-by-id-with-maintenance-status db-spec user-id)
      nil)))

(defn load-user-by-facebook-user-id
  "Loads and returns a user entity given the user's email address.  Returns
   nil if no user is found."
  ([db-spec facebook-user-id]
   (load-user-by-facebook-user-id db-spec facebook-user-id true))
  ([db-spec facebook-user-id active-only]
   (load-user-by-col db-spec "facebook_user_id" facebook-user-id active-only)))

(defn load-user-by-email
  "Loads and returns a user entity given the user's email address.  Returns
   nil if no user is found."
  ([db-spec email]
   (load-user-by-email db-spec email true))
  ([db-spec email active-only]
   (load-user-by-col db-spec "email" email active-only)))

(defn load-user-by-email-with-maintenance-status
  "Loads and returns a user entity given the user's email address.  Returns
   nil if no user is found."
  ([db-spec email]
   (load-user-by-email-with-maintenance-status db-spec email true))
  ([db-spec email active-only]
   (load-user-by-col-with-maintenance-status db-spec "email" email active-only)))

(defn load-user-by-username
  "Loads and returns a user entity given the user's username.  Returns nil if no
  user is found."
  ([db-spec username]
   (load-user-by-username db-spec username true))
  ([db-spec username active-only]
   (load-user-by-col db-spec "username" username active-only)))

(defn load-user-by-stripe-customer-id
  "Loads and returns a user entity given the user's stripe customer id.  Returns
   nil if no user is found."
  ([db-spec stripe-customer-id]
   (load-user-by-stripe-customer-id db-spec stripe-customer-id true))
  ([db-spec stripe-customer-id active-only]
   (load-user-by-col db-spec "stripe_customer_id" stripe-customer-id active-only)))

(defn load-user-by-id
  "Loads and returns a user entity given the user's id.  Returns nil if no user
  is found."
  ([db-spec id]
   (load-user-by-id db-spec id true))
  ([db-spec id active-only]
   (load-user-by-col db-spec "id" id active-only)))

(defn load-user-by-id-with-maintenance-status
  "Loads and returns a user entity given the user's id.  Returns nil if no user
  is found."
  ([db-spec id]
   (load-user-by-id-with-maintenance-status db-spec id true))
  ([db-spec id active-only]
   (load-user-by-col-with-maintenance-status db-spec "id" id active-only)))

(defn load-user-by-authtoken
  "Loads and returns a user entity given an authentication token.  Returns
  nil if no associated user is found."
  ([db-spec user-id plaintext-authtoken]
   (load-user-by-authtoken db-spec user-id plaintext-authtoken true))
  ([db-spec user-id plaintext-authtoken active-only]
   {:pre [(not (nil? plaintext-authtoken))]}
   (let [tokens-rs (j/query db-spec
                            [(format (str "SELECT hashed_token "
                                          "FROM %s "
                                          "WHERE user_id = ? AND "
                                          "invalidated_at IS NULL AND "
                                          "(expires_at IS NULL OR expires_at > ?) "
                                          "ORDER BY created_at DESC")
                                     uddl/tbl-auth-token)
                             user-id
                             (c/to-timestamp (t/now))]
                            {:row-fn :hashed_token})]
     (when (some #(bcrypt-verify plaintext-authtoken %) tokens-rs)
       (load-user-by-id-with-maintenance-status db-spec user-id active-only)))))

(defn load-user-by-verification-token
  "Loads and returns a user entity given an verification token.  Returns
  nil if no associated user is found."
  ([db-spec email plaintext-verification-token]
   (load-user-by-verification-token db-spec email plaintext-verification-token true))
  ([db-spec email plaintext-verification-token active-only]
   {:pre [(not (nil? plaintext-verification-token))]}
   (let [tokens-rs (j/query db-spec
                            [(format (str "SELECT hashed_token "
                                          "FROM %s "
                                          "WHERE user_id IN (select id from %s where email = ?) AND "
                                          "flagged_at IS NULL AND "
                                          "(expires_at IS NULL OR expires_at > ?) "
                                          "ORDER BY created_at DESC")
                                     uddl/tbl-account-verification-token
                                     uddl/tbl-user-account)
                             email
                             (c/to-timestamp (t/now))]
                            {:row-fn :hashed_token})]
     (when (some #(bcrypt-verify plaintext-verification-token %) tokens-rs)
       (load-user-by-email db-spec email active-only)))))

(defn load-user-by-password-reset-token
  "Loads and returns a user entity given a password reset token.  Returns
  nil if no associated user is found."
  ([db-spec email plaintext-password-reset-token]
   (load-user-by-password-reset-token db-spec email plaintext-password-reset-token true))
  ([db-spec email plaintext-password-reset-token active-only]
   {:pre [(not (nil? plaintext-password-reset-token))]}
   (let [tokens-rs (j/query db-spec
                            [(format (str "SELECT hashed_token "
                                          "FROM %s "
                                          "WHERE user_id IN (select id from %s where email = ?) "
                                          "ORDER BY created_at DESC")
                                     uddl/tbl-password-reset-token
                                     uddl/tbl-user-account)
                             email]
                            {:row-fn :hashed_token})]
     (when (some #(bcrypt-verify plaintext-password-reset-token %) tokens-rs)
       (load-user-by-email db-spec email active-only)))))

(defn users
  ([db-spec]
   (users db-spec true))
  ([db-spec active-only]
   (jcore/load-entities db-spec
                        uddl/tbl-user-account
                        "updated_at"
                        "desc"
                        rs->user
                        active-only)))

(defn users-with-almost-expired-trial-periods-and-not-notified
  [db-spec expires-within-days]
  (let [now (t/now)
        now-sql (c/to-timestamp now)
        now-shifted (t/plus now (t/days expires-within-days))
        now-shifted-sql (c/to-timestamp now-shifted)]
    (j/query db-spec
             [(format (str "select * from %s where "
                           "final_failed_payment_attempt_occurred_at is null and "
                           "paid_enrollment_cancelled_at is null and "
                           "trial_almost_expired_notice_sent_at is null and "
                           "paid_enrollment_established_at is null and "
                           "verified_at is not null and "
                           "trial_ends_at < ?")
                      uddl/tbl-user-account)
              now-shifted-sql]
             {:row-fn rs->user})))

(defn users-with-iap-subscription-needing-validation
  [db-spec]
  (let [now (t/now)
        now-sql (c/to-timestamp now)]
    (j/query db-spec
             [(format (str "select * from %s where "
                           "final_failed_payment_attempt_occurred_at is null and "
                           "paid_enrollment_cancelled_at is null and "
                           "paid_enrollment_established_at is not null and "
                           "validate_app_store_receipt_at is not null and "
                           "validate_app_store_receipt_at <= ?")
                      uddl/tbl-user-account)
              now-sql]
             {:row-fn rs->user})))

(defn total-users-count
  [db-spec]
  (:num (first (j/query db-spec [(format "select count(id) as num from %s" uddl/tbl-user-account)]))))

(defn trial-period-count
  [db-spec]
  (let [now-sql (c/to-timestamp (t/now))]
    (:num (first (j/query db-spec [(format (str "select count(id) as num from %s where "
                                                "final_failed_payment_attempt_occurred_at is null and "
                                                "paid_enrollment_cancelled_at is null and "
                                                "paid_enrollment_established_at is null and "
                                                "trial_ends_at > ?")
                                           uddl/tbl-user-account)
                                   now-sql])))))

(defn trial-period-expired-count
  [db-spec]
  (let [now-sql (c/to-timestamp (t/now))]
    (:num (first (j/query db-spec [(format (str "select count(id) as num from %s where "
                                                "final_failed_payment_attempt_occurred_at is null and "
                                                "paid_enrollment_cancelled_at is null and "
                                                "paid_enrollment_established_at is null and "
                                                "trial_ends_at <= ?")
                                           uddl/tbl-user-account)
                                   now-sql])))))

(defn almost-expired-trial-count
  [db-spec expires-within-days]
  (let [now (t/now)
        now-sql (c/to-timestamp now)
        now-shifted (t/plus now (t/days expires-within-days))
        now-shifted-sql (c/to-timestamp now-shifted)]
    (:num (first (j/query db-spec [(format (str "select count(id) as num from %s where "
                                                "final_failed_payment_attempt_occurred_at is null and "
                                                "paid_enrollment_cancelled_at is null and "
                                                "paid_enrollment_established_at is null and "
                                                "trial_ends_at < ?")
                                           uddl/tbl-user-account)
                                   now-shifted-sql])))))

(defn paid-enrollment-good-standing-count
  [db-spec]
  (:num (first (j/query db-spec [(format (str "select count(id) as num from %s where "
                                              "final_failed_payment_attempt_occurred_at is null and "
                                              "paid_enrollment_cancelled_at is null and "
                                              "paid_enrollment_established_at is not null and "
                                              "is_payment_past_due = false") uddl/tbl-user-account)]))))

(defn paid-enrollment-cancelled-count
  [db-spec]
  (:num (first (j/query db-spec [(format (str "select count(id) as num from %s where "
                                              "final_failed_payment_attempt_occurred_at is null and "
                                              "paid_enrollment_cancelled_at is not null and "
                                              "paid_enrollment_established_at is not null and "
                                              "is_payment_past_due = false") uddl/tbl-user-account)]))))

(defn payment-past-due-count
  [db-spec]
  (:num (first (j/query db-spec [(format (str "select count(id) as num from %s where "
                                              "paid_enrollment_established_at is not null and "
                                              "is_payment_past_due = true") uddl/tbl-user-account)]))))

(defn final-payment-past-due-count
  [db-spec]
  (:num (first (j/query db-spec [(format (str "select count(id) as num from %s where "
                                              "paid_enrollment_established_at is not null and "
                                              "final_failed_payment_attempt_occurred_at is not null and "
                                              "is_payment_past_due = true") uddl/tbl-user-account)]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Saving and other user-related operations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def user-key-pairs
  [[:user/name                           :name]
   [:user/email                          :email]
   [:user/username                       :username]
   [:user/password                       :hashed_password                hash-bcrypt]
   [:user/hashed-password                :hashed_password]
   [:user/trial-ends-at                  :trial_ends_at                  c/to-timestamp]
   [:user/paid-enrollment-established-at :paid_enrollment_established_at c/to-timestamp]
   [:user/paid-enrollment-cancelled-at   :paid_enrollment_cancelled_at   c/to-timestamp]
   [:user/paid-enrollment-cancelled-reason :paid_enrollment_cancelled_reason]
   [:user/trial-almost-expired-notice-sent-at :trial_almost_expired_notice_sent_at c/to-timestamp]
   [:user/final-failed-payment-attempt-occurred-at :final_failed_payment_attempt_occurred_at c/to-timestamp]
   [:user/stripe-customer-id             :stripe_customer_id]
   [:user/last-charge-id                 :last_charge_id]
   [:user/latest-stripe-token-id         :latest_stripe_token_id]
   [:user/next-invoice-at                :next_invoice_at                c/to-timestamp]
   [:user/next-invoice-amount            :next_invoice_amount]
   [:user/last-invoice-at                :last_invoice_at                c/to-timestamp]
   [:user/last-invoice-amount            :last_invoice_amount]
   [:user/current-card-last4             :current_card_last4]
   [:user/current-card-brand             :current_card_brand]
   [:user/current-card-exp-month         :current_card_exp_month]
   [:user/current-card-exp-year          :current_card_exp_year]
   [:user/trial-cancelled-reasons        :trial_cancelled_reasons]
   [:user/new-movements-added-at         :new_movements_added_at         c/to-timestamp]
   [:user/informed-of-maintenance-at     :informed_of_maintenance_at     c/to-timestamp]
   [:user/maintenance-starts-at          :maintenance_starts_at          c/to-timestamp]
   [:user/maintenance-duration           :maintenance_duration]
   [:user/current-plan-price             :current_plan_price]
   [:user/max-allowed-set-import         :max_allowed_set_import]
   [:user/max-allowed-bml-import         :max_allowed_bml_import]
   [:user/facebook-user-id               :facebook_user_id]
   [:user/app-store-receipt-data-base64  :app_store_receipt_data_base64]
   [:user/app-store-receipt-validation-url :app_store_receipt_validation_url]
   [:user/validate-app-store-receipt-at  :validate_app_store_receipt_at  c/to-timestamp]
   [:user/is-payment-past-due            :is_payment_past_due]
   [:user/deleted-at                     :deleted_at                     c/to-timestamp]
   [:user/deleted-reason                 :deleted_reason]])

(def user-uniq-constraints
  [[uddl/constr-user-account-uniq-email val/su-email-already-registered]
   [uddl/constr-user-account-uniq-username val/su-username-already-registered]])

(defn load-authtokens-by-user-id
  [db-spec user-id]
  (j/query db-spec
           [(format "select * from %s where user_id = ? and invalidated_at is null" uddl/tbl-auth-token)
            user-id]))

(defn load-verification-tokens-by-user-id
  [db-spec user-id]
  (j/query db-spec
           [(format "select * from %s where user_id = ? and verified_at is null and expires_at is null and flagged_at is null"
                    uddl/tbl-account-verification-token)
            user-id]))

(defn load-password-reset-tokens-by-user-id
  [db-spec user-id]
  (j/query db-spec
           [(format "select * from %s where user_id = ?" uddl/tbl-password-reset-token)
            user-id]))

(defn load-authtoken-by-plaintext-token
  [db-spec user-id plaintext-token]
  (let [tokens (load-authtokens-by-user-id db-spec user-id)]
    (some #(when (bcrypt-verify plaintext-token (:hashed_token %)) %) tokens)))

(defn load-verification-token-by-plaintext-token
  [db-spec user-id plaintext-token]
  (let [tokens (load-verification-tokens-by-user-id db-spec user-id)]
    (some #(when (bcrypt-verify plaintext-token (:hashed_token %)) %) tokens)))

(defn load-password-reset-token-by-plaintext-token
  [db-spec user-id plaintext-token]
  (let [tokens (load-password-reset-tokens-by-user-id db-spec user-id)]
    (some #(when (bcrypt-verify plaintext-token (:hashed_token %)) %) tokens)))

(defn invalidate-user-token
  [db-spec user-id plaintext-token reason]
  (let [authtoken (load-authtoken-by-plaintext-token db-spec
                                                     user-id
                                                     plaintext-token)]
    (when authtoken
      (j/delete! db-spec :authentication_token ["id = ?" (:id authtoken)]))))

(defn invalidate-user-tokens
  [db-spec user-id reason]
  (j/delete! db-spec :authentication_token ["user_id = ?" user-id]))

(defn invalidate-all-tokens
  [db-spec reason]
  (j/delete! db-spec :authentication_token nil))

(defn logout-user-token
  [db-spec user-id plaintext-token]
  (invalidate-user-token db-spec user-id plaintext-token invalrsn-logout))

(defn save-new-user
  [db-spec new-id user]
  (jcore/save-new-entity db-spec
                         new-id
                         user
                         val/save-new-user-validation-mask
                         val/su-any-issues
                         load-user-by-id-with-maintenance-status
                         :user_account
                         user-key-pairs
                         nil
                         :user/created-at
                         :user/updated-at
                         user-uniq-constraints
                         nil))

(def ^:dynamic *email-live-mode* false)

(defn send-email
  [mustache-template
   data
   subject-line
   from
   to
   domain
   mailgun-api-key]
  (when *email-live-mode*
    (let [credentials {:api-key mailgun-api-key :domain domain}
          params {:from from
                  :to to
                  :subject subject-line
                  :html (clostache/render-resource mustache-template data)}]
      (mailgun/send-email credentials params))))

(defn check-trial-and-grace-expired
  [user trial-expired-grace-period-in-days]
  (if (and (nil? (:user/paid-enrollment-established-at user))
           (not (nil? (:user/trial-ends-at user)))
           (t/after? (t/now) (t/plus (:user/trial-ends-at user) (t/days trial-expired-grace-period-in-days))))
    (throw (ex-info nil {:cause :trial-and-grace-expired}))
    user))

(defn send-verification-notice
  [db-spec
   user-id
   mustache-template
   subject-line
   from
   verification-url-maker-fn
   verification-flagged-url-maker-fn
   trial-expired-grace-period-in-days
   domain
   mailgun-api-key]
  (let [[_ loaded-user :as loaded-user-result] (load-user-by-id db-spec user-id true)]
    (when loaded-user-result
      (let [loaded-user-result (-> loaded-user-result
                                   (check-trial-and-grace-expired trial-expired-grace-period-in-days))]
        (when (and (nil? (:user/verified-at loaded-user))
                   (not (nil? (:user/email loaded-user))))
          (let [email (:user/email loaded-user)
                verification-token (create-and-save-verification-token db-spec
                                                                       user-id
                                                                       (:user/email loaded-user))
                verification-url (verification-url-maker-fn email verification-token)]
            (log/debug "verification email sent to: [" email "], verification-url: [" verification-url "]")
            (send-email mustache-template
                        (merge {:verification-url verification-url
                                :flagged-url (verification-flagged-url-maker-fn email verification-token)}
                               loaded-user)
                        subject-line
                        from
                        (:user/email loaded-user)
                        domain
                        mailgun-api-key)))))))

(defn send-password-reset-notice
  [db-spec
   email
   mustache-template
   subject-line
   from
   password-reset-url-maker-fn
   password-reset-flagged-url-maker-fn
   trial-expired-grace-period-in-days
   domain
   mailgun-api-key]
  (let [[_ loaded-user :as loaded-user-result] (load-user-by-email db-spec email true)]
    (if loaded-user-result
      (try
        (let [loaded-user (-> loaded-user
                              (check-trial-and-grace-expired trial-expired-grace-period-in-days))]
          (if (not (nil? (:user/verified-at loaded-user)))
            (let [user-id (:user/id loaded-user)
                  password-reset-token (create-and-save-password-reset-token db-spec user-id (:user/email loaded-user))
                  password-reset-url (password-reset-url-maker-fn email password-reset-token)]
              (log/debug "password-reset-url: [" password-reset-url "]")
              (send-email mustache-template
                          (merge {:password-reset-url password-reset-url
                                  :flagged-url (password-reset-flagged-url-maker-fn email password-reset-token)}
                                 loaded-user)
                          subject-line
                          from
                          email
                          domain
                          mailgun-api-key))
            (throw (IllegalArgumentException. (str val/pwd-reset-unverified-acct)))))
        (catch clojure.lang.ExceptionInfo e
          (let [cause (-> e ex-data :cause)]
            (cond
              (= cause :trial-and-grace-expired) (throw (IllegalArgumentException. (str val/pwd-reset-trial-and-grace-exp)))))))
      (throw (IllegalArgumentException. (str val/pwd-reset-unknown-email))))))

(defn save-user
  ([db-spec id user]
   (save-user db-spec id nil user))
  ([db-spec id auth-token-id user]
   (save-user db-spec id auth-token-id user nil))
  ([db-spec id auth-token-id user if-unmodified-since]
   (jcore/save-entity db-spec
                      id
                      user
                      val/save-user-validation-mask
                      val/su-any-issues
                      load-user-by-id
                      :user_account
                      user-key-pairs
                      :user/updated-at
                      user-uniq-constraints
                      nil
                      if-unmodified-since)))

(defn verify-user
  [db-spec
   email
   plaintext-verification-token
   trial-expired-grace-period-in-days]
  (let [[user-id user :as user-result] (load-user-by-verification-token db-spec
                                                                        email
                                                                        plaintext-verification-token false)]
    (when user-result
      (let [user (-> user
                     (check-trial-and-grace-expired trial-expired-grace-period-in-days))
            verification-token-rs (load-verification-token-by-plaintext-token db-spec
                                                                              user-id
                                                                              plaintext-verification-token)]
        (when verification-token-rs
          (when (and (nil? (:verified_at verification-token-rs))
                     (nil? (:flagged_at verification-token-rs)))
            (let [now-sql (c/to-timestamp (t/now))]
              (j/update! db-spec
                         :account_verification_token
                         {:verified_at now-sql}
                         ["id = ?" (:id verification-token-rs)])
              (j/update! db-spec
                         :user_account
                         {:verified_at now-sql
                          :updated_at now-sql}
                         ["id = ?" user-id]))))
        user))))

(defn- do-action-on-password-reset-token
  [password-reset-token-rs should-be-prepared-already action-fn]
  (if password-reset-token-rs
    (if (or (not should-be-prepared-already)
            (not (nil? (:accessed_at password-reset-token-rs))))
      (if (nil? (:used_at password-reset-token-rs))
        (if (nil? (:flagged_at password-reset-token-rs))
          (let [expires-at (c/from-sql-time (:expires_at password-reset-token-rs))]
            (if (not (nil? expires-at))
              (if (t/after? expires-at (t/now))
                (action-fn)
                (throw (IllegalArgumentException. (str val/pwd-reset-token-expired))))
              (action-fn)))
          (throw (IllegalArgumentException. (str val/pwd-reset-token-flagged))))
        (throw (IllegalArgumentException. (str val/pwd-reset-token-already-used))))
      (throw (IllegalArgumentException. (str val/pwd-reset-token-not-prepared))))
    (throw (IllegalArgumentException. (str val/pwd-reset-token-not-found)))))

(defn prepare-password-reset
  [db-spec
   email
   plaintext-password-reset-token
   trial-expired-grace-period-in-days]
  (let [[user-id user :as user-result] (load-user-by-password-reset-token db-spec
                                                                          email
                                                                          plaintext-password-reset-token false)]
    (when user-result
      (let [user (-> user
                     (check-trial-and-grace-expired trial-expired-grace-period-in-days))
            password-reset-token-rs (load-password-reset-token-by-plaintext-token db-spec
                                                                                  user-id
                                                                                  plaintext-password-reset-token)]
        (letfn [(do-prepare-password-reset []
                  (let [now-sql (c/to-timestamp (t/now))]
                    (j/update! db-spec
                               :password_reset_token
                               {:accessed_at now-sql}
                               ["id = ?" (:id password-reset-token-rs)])))]
          (do-action-on-password-reset-token password-reset-token-rs
                                             false
                                             do-prepare-password-reset))
        user))))

(defn reset-password
  [db-spec email plaintext-password-reset-token new-password]
  (let [[user-id user :as user-result] (load-user-by-password-reset-token db-spec
                                                                          email
                                                                          plaintext-password-reset-token
                                                                          false)]
    (if (and (not (nil? user-result))
             (= user-id (:user/id user)))
      (let [password-reset-token-rs (load-password-reset-token-by-plaintext-token db-spec
                                                                                  user-id
                                                                                  plaintext-password-reset-token)]
        (letfn [(do-reset-password []
                  (let [now-sql (c/to-timestamp (t/now))]
                    (j/delete! db-spec :password_reset_token ["user_id = ?" user-id])
                    (save-user db-spec user-id {:user/password new-password
                                                :user/facebook-user-id nil})
                    (invalidate-user-tokens db-spec user-id invalrsn-password-reset)))]
          (do-action-on-password-reset-token password-reset-token-rs
                                             true
                                             do-reset-password))
        user)
      (throw (IllegalArgumentException. (str val/pwd-reset-token-not-found))))))

(defn create-and-save-auth-token
  ([db-spec user-id new-id]
   (create-and-save-auth-token db-spec
                               user-id
                               new-id
                               nil))
  ([db-spec user-id new-id exp-date]
   (let [uuid (str (java.util.UUID/randomUUID))]
     (j/insert! db-spec
                :authentication_token
                {:id new-id
                 :user_id user-id
                 :hashed_token (hash-bcrypt uuid)
                 :created_at (c/to-timestamp (t/now))
                 :expires_at (c/to-timestamp exp-date)})
     uuid)))

(defn create-and-save-verification-token
  ([db-spec user-id email-address]
   (let [uuid (str (java.util.UUID/randomUUID))
         now (t/now)
         now-sql (c/to-timestamp now)
         expires-at nil ;(t/plus now (t/weeks 2))
         expires-at-sql (c/to-timestamp expires-at)]
     (j/insert! db-spec
                :account_verification_token
                {:id (next-verification-token-id db-spec)
                 :user_id user-id
                 :sent_to_email email-address
                 :hashed_token (hash-bcrypt uuid)
                 :created_at now-sql
                 :expires_at expires-at-sql})
     uuid)))

(defn create-and-save-password-reset-token
  [db-spec user-id email-address]
  (let [uuid (str (java.util.UUID/randomUUID))
        now (t/now)
        now-sql (c/to-timestamp now)
        expires-at (t/plus now (t/weeks 1))
        expires-at-sql (c/to-timestamp expires-at)]
    (j/insert! db-spec
               :password_reset_token
               {:id (next-password-reset-token-id db-spec)
                :user_id user-id
                :sent_to_email email-address
                :hashed_token (hash-bcrypt uuid)
                :created_at now-sql
                :expires_at expires-at-sql})
    uuid))

(defn authenticate-user-by-authtoken
  [db-spec
   user-id
   plaintext-authtoken
   trial-expired-grace-period-in-days]
  (load-user-by-authtoken db-spec user-id plaintext-authtoken false))

(defmulti authenticate-user-by-password
  "Authenticates a user given an email address (or username) and password.  Upon
   a successful authentication, returns the associated user entity; otherwise
   returns nil."
  (fn [db-spec
       username-or-email
       plaintext-password
       trial-expired-grace-period-in-days]
    {:pre [(and (not (nil? db-spec))
                (not (empty? username-or-email))
                (not (empty? plaintext-password)))]}
    (if (.contains username-or-email "@")
      :email
      :username)))

(defmethod authenticate-user-by-password :email
  [db-spec
   email
   plaintext-password
   trial-expired-grace-period-in-days]
  (let [[_ user :as result] (load-user-by-email-with-maintenance-status db-spec email false)]
    (when (and user
               (not (nil? (:user/hashed-password user)))
               (bcrypt-verify plaintext-password (:user/hashed-password user)))
      (-> user
          (check-trial-and-grace-expired trial-expired-grace-period-in-days))
      result)))

(defmethod authenticate-user-by-password :username
  [db-spec
   username
   plaintext-password
   trial-expired-grace-period-in-days]
  (let [[_ user :as result] (load-user-by-username db-spec username false)]
    (when (and user
               (bcrypt-verify plaintext-password (:user/hashed-password user)))
      (-> user
          (check-trial-and-grace-expired trial-expired-grace-period-in-days))
      result)))
