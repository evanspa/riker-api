(ns riker-app.app.jobs
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as j]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [riker-app.core.jdbc :as jcore]
            [clojurewerkz.quartzite.triggers :as qt]
            [clojurewerkz.quartzite.jobs :as qj]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojurewerkz.quartzite.schedule.daily-interval :refer [schedule
                                                                    with-interval-in-days
                                                                    on-every-day
                                                                    starting-daily-at
                                                                    time-of-day]]
            [riker-app.utils :as rutils]
            [riker-app.core.user-ddl :as uddl]
            [riker-app.core.ddl :as rddl]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.dao :as dao]
            [riker-app.app.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Job Utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn make-job
  [job job-key-str]
  (qj/build
   (qj/of-type job)
   (qj/with-identity (qj/key job-key-str))))

(defn make-daily-trigger
  [key-str interval-in-days hour-start]
  (qt/build
   (qt/with-identity (qt/key key-str))
   (qt/start-now)
   (qt/with-schedule (schedule
                      (with-interval-in-days interval-in-days)
                      (on-every-day)
                      (starting-daily-at (time-of-day hour-start 00 00))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Job Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn validate-iap-subscriptions-job-fn
  []
  (try
    (let [users (usercore/users-with-iap-subscription-needing-validation config/db-spec)
          num-users (count users)]
      (if (> num-users 0)
        (do
          (log/info (format "[IAP Subscription Validation Job] - Proceeding to validate iap subscriptions for %s user(s)" num-users))
          (doseq [[user-id user-ent] users]
            (let [validation-url (:user/app-store-receipt-validation-url user-ent)
                  validation-url (if (nil? validation-url) config/r-app-store-receipt-validation-url validation-url)]
              (log/info (format "[IAP Subscription Validation Job] - Proceeding to validate iap receipt for user with id: [%s], using validation url: [%s]" user-id validation-url))
              (let [validation-date (:user/validate-app-store-receipt-at user-ent)]
                (letfn [(extend-iap-validation-date [conn]
                          (log/info (format "[IAP Subscription Validation Job] - Subscription status is good.  Proceeding to extend by 1 month the next validation check for user with id: [%s]" user-id))
                          (j/update! conn
                                     uddl/tbl-user-account
                                     {:validate_app_store_receipt_at (c/to-timestamp (t/plus validation-date (t/months 1)))}
                                     ["id = ?" user-id]))
                        (cancel-subscription [conn d]
                          (log/info (format "[IAP Subscription Validation Job] - Subscription status is cancelled.  Proceeding to mark as cancelled the account for user with id: [%s]" user-id))
                          (let [user-email (:user/email user-ent)]
                            (try
                              (usercore/save-user conn user-id {:user/paid-enrollment-cancelled-at d})
                              (usercore/send-email config/r-iap-subscription-cancelled-email-mustache-template
                                                   nil
                                                   config/r-subscription-cancelled-email-subject
                                                   config/r-support-email-address
                                                   user-email
                                                   config/r-domain
                                                   config/r-mailgun-api-key)
                              (catch Exception e
                                (rutils/log-e e (format "[IAP Subscription Validation Job] - Exception caught in job-fn attempting to cancel or send cancel email notice to user with id: [%s], email: [%s]" user-id user-email))))))]
                  (j/with-db-transaction [conn config/db-spec]
                    (let [receipt-data (:user/app-store-receipt-data-base64 user-ent)
                          iap-receipts (rutils/iap-latest-receipt-info receipt-data validation-url)
                          latest-receipt (last iap-receipts)
                          cancellation-date (get latest-receipt "cancellation_date")
                          expires-date (rutils/parse-iap-date (get latest-receipt "expires_date"))]
                      (log/info (format "[IAP Subscription Validation Job] - Cancellation date for user-id: [%s] is: [%s]" user-id cancellation-date))
                      (log/info (format "[IAP Subscription Validation Job] - Expires date for user-id: [%s] is: [%s]" user-id expires-date))
                      (if (not (nil? cancellation-date)) ; Apple customer support cancelled the subscription
                        (cancel-subscription conn cancellation-date)
                        (if (t/after? (t/now) expires-date) ; user cancelled the subscription in their iTunes
                          (cancel-subscription conn expires-date)
                          (extend-iap-validation-date conn))))))))))
        (log/info "[IAP Subscription Validation Job] - There are currently no users with iap-subscriptions requiring validation at this time.")))
    (catch Exception e
      (rutils/log-e e (format "[IAP Subscription Validation Job] - Exception caught in job-fn attempting to validate iap subscriptions")))))

(defn trial-almost-expired-notices-job-fn
  []
  (try
    (let [users (usercore/users-with-almost-expired-trial-periods-and-not-notified config/db-spec config/r-trial-almost-expires-threshold-in-days)
          num-users (count users)]
      (if (> num-users 0)
        (do
          (log/info (format "[Trial Almost Expired Job] - Proceeding to send 'trial almost expired' notices to %s user(s)" num-users))
          (doseq [[user-id user-ent] users]
            (let [now (t/now)
                  now-sql (c/to-timestamp now)
                  user-email (:user/email user-ent)]
              (try
                (usercore/send-email config/r-trial-period-almost-expired-notice-email-template
                                     (merge user-ent
                                            {:subscription-amount-usd (format "%.02f" (/ config/r-subscription-amount 100.0))
                                             :enrollment-url config/r-enrollment-web-url})
                                     config/r-trial-period-almost-expired-notice-email-subject
                                     config/r-support-email-address
                                     user-email
                                     config/r-domain
                                     config/r-mailgun-api-key)
                (j/with-db-transaction [conn config/db-spec]
                  (j/update! conn
                             uddl/tbl-user-account
                             {:trial_almost_expired_notice_sent_at now-sql}
                             ["id = ?" user-id]))
                (log/info (format "[Trial Almost Expired Job] - Sent 'trial almost expired' notice to user with id: [%s], email: [%s]" user-id user-email))
                (catch Exception e
                  (rutils/log-e e (format "[Trial Almost Expired Job] - Exception caught in job-fn attempting to send 'trial almost expired' email notice to user with id: [%s], email: [%s]" user-id user-email)))))))
        (log/info "[Trial Almost Expired Job] - There are currently no users with almost-expired trial accounts at this time.")))
    (catch Exception e
      (rutils/log-e e (format "[Trial Almost Expired Job] - Exception caught in job-fn attempting to fetch users with almost-expired trial accounts (for sending email notices to them)")))))

(defn aggregate-stats-job-fn
  []
  (try
    (j/with-db-transaction [conn config/db-spec]
      (let [total-users-count (usercore/total-users-count conn)
            trial-period-count (usercore/trial-period-count conn)
            trial-period-expired-count (usercore/trial-period-expired-count conn)
            almost-expired-trial-count (usercore/almost-expired-trial-count conn config/r-trial-almost-expires-threshold-in-days)
            paid-enrollment-good-standing-count (usercore/paid-enrollment-good-standing-count conn)
            paid-enrollment-cancelled-count (usercore/paid-enrollment-cancelled-count conn)
            final-payment-past-due-count (usercore/final-payment-past-due-count conn)
            payment-past-due-count (usercore/payment-past-due-count conn)
            total-set-count (dao/total-set-count conn)
            total-soreness-count (dao/total-soreness-count conn)
            total-body-journal-log-count (dao/total-body-journal-log-count conn)
            aggregate-counts {:total-users-count total-users-count
                              :payment-past-due-count payment-past-due-count
                              :trial-period-count trial-period-count
                              :trial-period-expired-count trial-period-expired-count
                              :almost-expired-trial-count almost-expired-trial-count
                              :paid-enrollment-good-standing-count paid-enrollment-good-standing-count
                              :paid-enrollment-cancelled-count paid-enrollment-cancelled-count
                              :final-failed-payment-attempt-occurred-count final-payment-past-due-count}]
        (j/insert! conn
                   rddl/tbl-aggregates
                   {:total_users_count total-users-count
                    :payment_past_due_count payment-past-due-count
                    :trial_period_count trial-period-count
                    :trial_period_expired_count trial-period-expired-count
                    :almost_expired_trial_count almost-expired-trial-count
                    :paid_enrollment_good_standing_count paid-enrollment-good-standing-count
                    :paid_enrollment_cancelled_count paid-enrollment-cancelled-count
                    :final_failed_payment_attempt_occurred_count final-payment-past-due-count
                    :total_set_count total-set-count
                    :total_body_journal_log_count total-body-journal-log-count
                    :total_soreness_count total-soreness-count
                    :logged_at (c/to-timestamp (t/now))})
        (usercore/send-email config/r-aggregate-stats-email-template
                             aggregate-counts
                             config/r-aggregate-stats-email-subject
                             config/r-stats-email-address
                             config/r-stats-email-address
                             config/r-domain
                             config/r-mailgun-api-key)
        (log/info "[Aggregate Stats Job] - Successfully processed aggregate stats (email notice sent, too).")
        ))
    (catch Exception e
      (rutils/log-e e "[Aggregate Stats Job] - Exception caught in job-fn attempting to process aggregate stats"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Jobs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defjob trial-almost-expired-notices [ctx] (trial-almost-expired-notices-job-fn))
(defjob validate-iap-subscriptions [ctx] (validate-iap-subscriptions-job-fn))
(defjob aggregate-stats [ctx] (aggregate-stats-job-fn))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Identified Jobs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def trial-almost-expired-notices-job (make-job trial-almost-expired-notices "jobs.trial.almost.expired.notices"))
(def validate-iap-subscriptions-job (make-job validate-iap-subscriptions "validate-iap-subscription.notices"))
(def aggregate-stats-job (make-job aggregate-stats "jobs.aggregate.stats"))
