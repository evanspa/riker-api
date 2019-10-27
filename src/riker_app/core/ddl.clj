(ns riker-app.core.ddl
  (:require [clojure.java.jdbc :as j]
            [riker-app.core.jdbc :as jcore]
            [riker-app.core.user-ddl :as uddl]
            [riker-app.app.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reference Data Tables
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def tbl-body-segment "body_segment")
(def tbl-muscle-group "muscle_group")
(def tbl-muscle "muscle")
(def tbl-muscle-alias "muscle_alias")
(def tbl-movement "movement")
(def tbl-movement-variant "movement_variant")
(def tbl-movement-alias "movement_alias")
(def tbl-movement-primary-muscle "movement_primary_muscle")
(def tbl-movement-secondary-muscle "movement_secondary_muscle")
(def tbl-cardio-intensity "cardio_intensity")
(def tbl-cardio-type "cardio_type")
(def tbl-walking-pace "walking_pace")
(def tbl-origination-device "origination_device")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; User Data Tables
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def tbl-stripe-token "stripe_token")
(def tbl-user-settings "user_settings")
(def tbl-soreness "soreness_log")
(def tbl-superset "superset")
(def tbl-set "set")
(def tbl-body-journal-log "body_journal_log")
(def tbl-cardio-session "cardio_session")

(def user-tables [tbl-user-settings
                  tbl-soreness
                  tbl-set
                  tbl-superset
                  tbl-body-journal-log
                  tbl-cardio-session])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Apple Search Ads Attribution table
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def tbl-apple-search-ads-attribution "apple_search_ads_attribution")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Aggregates Table
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def tbl-aggregates "aggregates")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Column Names
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def col-updated-count "updated_count")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helper Functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- apply-ddl-updateable-table
  [db-spec create-table-ddl-fn tbl]
  (j/db-do-commands db-spec
                    true
                    [(create-table-ddl-fn tbl)])
  (jcore/with-try-catch-exec-as-query db-spec
    (jcore/auto-inc-trigger-fn db-spec tbl col-updated-count))
  (jcore/with-try-catch-exec-as-query db-spec
    (jcore/auto-inc-trigger db-spec
                            tbl
                            col-updated-count
                            (jcore/inc-trigger-fn-name tbl
                                                       col-updated-count))))

(defn- apply-ddl
  [db-spec ddl]
  (j/db-do-commands db-spec true [ddl]))

(defn- apply-ddl-as-qry
  [db-spec ddl]
  (try
    (j/query db-spec ddl)
    (catch Exception e)))

(defn- apply-ddl-trigger
  [db-spec trigger-name trigger-fn-ddl-fn trigger-ddl-fn]
  (jcore/with-try-catch-exec-as-query db-spec
    (trigger-fn-ddl-fn trigger-name))
  (jcore/with-try-catch-exec-as-query db-spec
    (trigger-ddl-fn trigger-name)))

(defn apply-ddl-collection
  [db-spec ddl]
  (doseq [[ddl-fn ddl-fn-args] ddl]
    (apply ddl-fn db-spec ddl-fn-args)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v24 DDL (not deployed to prod yet)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v24-ddl
  [[apply-ddl [(format "alter table %s add column facebook_user_id text unique null", uddl/tbl-user-account)]]
   [apply-ddl [(format "alter table %s alter column hashed_password drop not null", uddl/tbl-user-account)]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v23 DDL (was deployed to production on 07/14/2018)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v23-ddl
  [
   [apply-ddl [(format (str "CREATE TABLE IF NOT EXISTS %s ("
                            "id                     serial  PRIMARY KEY, "
                            (format "user_id        integer NULL REFERENCES %s (id), " uddl/tbl-user-account)
                            "iad_attribution        boolean NULL,"
                            "iad_org_name           text    NULL,"
                            "iad_campaign_id        integer NULL,"
                            "iad_campaign_name      text    NULL,"
                            "iad_purchase_date      text    NULL,"
                            "iad_conversion_date    text    NULL,"
                            "iad_conversion_type    text    NULL,"
                            "iad_click_date         text    NULL,"
                            "iad_adgroup_id         integer NULL,"
                            "iad_adgroup_name       text    NULL,"
                            "iad_keyword            text    NULL,"
                            "iad_keyword_matchtype  text    NULL,"
                            "iad_creativeset_id     text    NULL,"
                            "iad_creativeset_name   text    NULL)")
                       tbl-apple-search-ads-attribution)]]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v18 DDL (not in production yet)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; no ddl in this release...only data loads

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v17 DDL (was deployed to production on 06/26/2017)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; no ddl in this release...only data loads

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v16 DDL (was deployed to production on 06/04/2017)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v16-ddl
  [
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN app_store_receipt_validation_url text NULL" uddl/tbl-user-account)]]
   ])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v15 DDL (was deployed to production on 06/04/2017)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v15-ddl
  [
   [apply-ddl [(format "ALTER TABLE %s ALTER COLUMN weight TYPE numeric " tbl-set)]]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v14 DDL (was deployed to production on 05/30/2017)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; no ddl in this release...only data loads

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v13 DDL (was deployed to production on 04/08/2017)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v13-ddl
  [
   [apply-ddl [(format "ALTER TABLE %s ADD COLUMN neck_size numeric NULL" tbl-body-journal-log)]]
   [apply-ddl [(format "ALTER TABLE %s ADD COLUMN waist_size numeric NULL" tbl-body-journal-log)]]
   [apply-ddl [(format "ALTER TABLE %s ADD COLUMN thigh_size numeric NULL" tbl-body-journal-log)]]
   [apply-ddl [(format "ALTER TABLE %s ADD COLUMN forearm_size numeric NULL" tbl-body-journal-log)]]
   [apply-ddl [(format "ALTER TABLE %s DROP COLUMN height" tbl-body-journal-log)]]
   [apply-ddl [(format "ALTER TABLE %s DROP COLUMN body_fat_percentage" tbl-body-journal-log)]]
   [apply-ddl [(format "ALTER TABLE %s DROP COLUMN dob" tbl-user-settings)]]
   [apply-ddl [(format "ALTER TABLE %s DROP COLUMN gender_male" tbl-user-settings)]]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v12 DDL (was deployed to production on 03/22/2017)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v12-ddl
  [
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN imported_at timestamptz NULL" tbl-set)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN imported_at timestamptz NULL" tbl-body-journal-log)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN max_allowed_set_import integer NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN max_allowed_bml_import integer NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN abbrev_name text NULL" tbl-muscle-group)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN abbrev_name text NULL" tbl-movement-variant)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN abbrev_canonical_name text NULL" tbl-muscle)]]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v11 DDL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v11-ddl
  [
   [apply-ddl
    [(format (str "CREATE TABLE IF NOT EXISTS %s (current_plan_price integer NOT NULL)")
                      uddl/tbl-current-subscription-plan-info)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN app_store_receipt_data_base64 text NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN validate_app_store_receipt_at timestamptz NULL" uddl/tbl-user-account)]]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v10 DDL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v10-ddl
  [
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN paid_enrollment_cancelled_reason text NULL" uddl/tbl-user-account)]]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v9 DDL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v9-ddl
  [
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN paid_enrollment_cancelled_at timestamptz NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN final_failed_payment_attempt_occurred_at timestamptz NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN paid_enrollment_cancelled_count integer NULL" tbl-aggregates)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN final_failed_payment_attempt_occurred_count integer NULL" tbl-aggregates)]]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v8 DDL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v8-ddl
  [
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN sort_order integer NULL" tbl-movement-variant)]]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v6 DDL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v6-ddl
  [
   [apply-ddl
    [(format "DROP TABLE %s" tbl-muscle-alias)]]
   [apply-ddl
    [(format "DROP TABLE %s" tbl-movement-alias)]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                serial      PRIMARY KEY, "
           (format "muscle_id integer     NOT NULL REFERENCES %s (id), " tbl-muscle)
           "alias             text        UNIQUE NOT NULL, "
           "created_at        timestamptz NOT NULL, "
           "updated_at        timestamptz NOT NULL, "
           (format "%s        integer     NOT NULL, " col-updated-count)
           "deleted_at        timestamptz NULL)")
     tbl-muscle-alias]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                  serial      PRIMARY KEY, "
           (format "movement_id integer     NOT NULL REFERENCES %s (id), " tbl-movement)
           "alias               text        UNIQUE NOT NULL, "
           "created_at          timestamptz NOT NULL, "
           "updated_at          timestamptz NOT NULL, "
           (format "%s          integer     NOT NULL, " col-updated-count)
           "deleted_at          timestamptz NULL)")
     tbl-movement-alias]]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v4 DDL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v4-ddl
  [
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN description text NULL" tbl-movement-variant)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN new_movements_added_at timestamptz NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format (str "CREATE TABLE IF NOT EXISTS %s ("
                  "informed_of_maintenance_at timestamptz NOT NULL,"
                  "maintenance_starts_at      timestamptz NOT NULL,"
                  "maintenance_duration       integer     NOT NULl)")
             uddl/tbl-maintenance-window)]]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v3 DDL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v3-ddl
  [
   [apply-ddl
    [(format "ALTER TABLE %s ALTER COLUMN origination_device_id SET NOT NULL" tbl-set)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD CONSTRAINT fk_orig_device FOREIGN KEY (origination_device_id) REFERENCES %s(id)" tbl-set tbl-origination-device)]]
   [apply-ddl
    [(format "ALTER TABLE %s ALTER COLUMN origination_device_id SET NOT NULL" tbl-superset)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD CONSTRAINT fk_orig_device FOREIGN KEY (origination_device_id) REFERENCES %s(id)" tbl-superset tbl-origination-device)]]
   [apply-ddl
    [(format "ALTER TABLE %s ALTER COLUMN origination_device_id SET NOT NULL" tbl-body-journal-log)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD CONSTRAINT fk_orig_device FOREIGN KEY (origination_device_id) REFERENCES %s(id)" tbl-body-journal-log tbl-origination-device)]]
   [apply-ddl
    [(format "ALTER TABLE %s ALTER COLUMN origination_device_id SET NOT NULL" tbl-soreness)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD CONSTRAINT fk_orig_device FOREIGN KEY (origination_device_id) REFERENCES %s(id)" tbl-soreness tbl-origination-device)]]
   [apply-ddl
    [(format "ALTER TABLE %s ALTER COLUMN origination_device_id SET NOT NULL" tbl-cardio-session)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD CONSTRAINT fk_orig_device FOREIGN KEY (origination_device_id) REFERENCES %s(id)" tbl-cardio-session tbl-origination-device)]]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v2 DDL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v2-ddl
  [[apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN last_charge_id text NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN is_payment_past_due boolean NOT NULL" uddl/tbl-user-account)]]
   [apply-ddl-as-qry ["DROP FUNCTION IF EXISTS user_account_suspended_count_inc"]]
   [apply-ddl [(format "DROP TRIGGER IF EXISTS suspended_count_trigger on %s" uddl/tbl-user-account)]]
   [apply-ddl [(format "ALTER TABLE %s DROP COLUMN suspended_reason" uddl/tbl-user-account)]]
   [apply-ddl [(format "ALTER TABLE %s DROP COLUMN deleted_reason" uddl/tbl-user-account)]]
   [apply-ddl [(format "ALTER TABLE %s DROP COLUMN %s" uddl/tbl-user-account, uddl/col-suspended-count)]]
   [apply-ddl [(format "ALTER TABLE %s DROP COLUMN %s" uddl/tbl-user-account, uddl/col-suspended-at)]]
   [apply-ddl
    [(format (str "CREATE TABLE IF NOT EXISTS %s ("
                  "id                                  serial      PRIMARY KEY, "
                  "total_users_count                   integer     NULL,"
                  "trial_period_count                  integer     NULL,"
                  "trial_period_expired_count          integer     NULL,"
                  "almost_expired_trial_count          integer     NULL,"
                  "paid_enrollment_good_standing_count integer     NULL,"
                  "users_deleted_count                 integer     NULL,"
                  "payment_past_due_count              integer     NULL,"
                  "total_set_count                     integer     NULL,"
                  "total_superset_count                integer     NULL,"
                  "total_body_journal_log_count        integer     NULL,"
                  "total_cardio_session_count          integer     NULL,"
                  "total_soreness_count                integer     NULL,"
                  "event                               text        NULL,"
                  "logged_at                           timestamptz NOT NULL)")
             tbl-aggregates)]]
   [apply-ddl
    [(format (str "CREATE TABLE IF NOT EXISTS %s ("
                  "id                             serial      PRIMARY KEY, "
                  "user_id                        integer     UNIQUE NOT NULL,"
                  "deleted_reason                 integer     NOT NULL," ; user cancelled sub., trial+grace expired, final payment attempt failed
                  "user_cancelled_reasons_mask    integer     NULL,"
                  "deleted_at                     timestamptz NOT NULL,"
                  "user_email                     text        NOT NULL, "
                  "paid_enrollment_established_at timestamptz NULL,"
                  "stripe_customer_id             text        NULL, "
                  "stripe_customer_deleted_at     timestamptz NULL)")
             uddl/tbl-deleted-user-account)]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id              serial      PRIMARY KEY, "
           "name            text        UNIQUE NOT NULL,"
           "icon_image_name text        NOT NULL, "
           "created_at      timestamptz NOT NULL, "
           "updated_at      timestamptz NOT NULL, "
           (format "%s      integer     NOT NULL, " col-updated-count)
           "deleted_at      timestamptz NULL)")
     tbl-origination-device]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN trial_almost_expired_notice_sent_at timestamptz NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN origination_device_id integer NULL" tbl-set)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN origination_device_id integer NULL" tbl-superset)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN origination_device_id integer NULL" tbl-body-journal-log)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN origination_device_id integer NULL" tbl-soreness)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN origination_device_id integer NULL" tbl-cardio-session)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN latest_stripe_token_id text NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN next_invoice_at timestamptz NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN next_invoice_amount integer NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN last_invoice_at timestamptz NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN last_invoice_amount integer NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN current_card_last4 text NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN current_card_brand text NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN current_card_exp_month integer NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN current_card_exp_year integer NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ALTER COLUMN dob TYPE text" tbl-user-settings)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN trial_ends_at timestamptz NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN stripe_customer_id text NULL" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD UNIQUE (stripe_customer_id)" uddl/tbl-user-account)]]
   [apply-ddl
    [(format "ALTER TABLE %s ADD COLUMN paid_enrollment_established_at timestamptz NULL" uddl/tbl-user-account)]]
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v1 DDL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; none (only new data loads for V1)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v0 DDL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v0-ddl
  [[apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id         serial      PRIMARY KEY, "
           "name       text        UNIQUE NOT NULL,"
           "created_at timestamptz NOT NULL, "
           "updated_at timestamptz NOT NULL, "
           (format "%s integer     NOT NULL, " col-updated-count)
           "deleted_at timestamptz NULL)")
     tbl-body-segment]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                      serial      PRIMARY KEY, "
           (format "body_segment_id integer     NOT NULL REFERENCES %s (id), " tbl-body-segment)
           "name                    text        UNIQUE NOT NULL, "
           "created_at              timestamptz NOT NULL, "
           "updated_at              timestamptz NOT NULL, "
           (format "%s              integer     NOT NULL, " col-updated-count)
           "deleted_at              timestamptz NULL)")
     tbl-muscle-group]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                      serial      PRIMARY KEY, "
           (format "muscle_group_id integer     NOT NULL REFERENCES %s (id), " tbl-muscle-group)
           "canonical_name          text        UNIQUE NOT NULL, "
           "created_at              timestamptz NOT NULL, "
           "updated_at              timestamptz NOT NULL, "
           (format "%s              integer     NOT NULL, " col-updated-count)
           "deleted_at              timestamptz NULL)")
     tbl-muscle]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                serial      NOT NULL, "
           (format "muscle_id integer     NOT NULL REFERENCES %s (id), " tbl-muscle)
           "alias             text        UNIQUE NOT NULL, "
           "created_at        timestamptz NOT NULL, "
           "updated_at        timestamptz NOT NULL, "
           (format "%s        integer     NOT NULL, " col-updated-count)
           "deleted_at        timestamptz NULL, "
           "PRIMARY KEY (muscle_id, id))")
     tbl-muscle-alias]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                        serial      PRIMARY KEY, "
           "name                      text        UNIQUE NOT NULL, "
           "created_at                timestamptz NOT NULL, "
           "updated_at                timestamptz NOT NULL, "
           (format "%s                integer     NOT NULL, " col-updated-count)
           "deleted_at                timestamptz NULL)")
     tbl-movement-variant]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                        serial      PRIMARY KEY, "
           "canonical_name            text        UNIQUE NOT NULL, "
           "is_body_lift              boolean     NOT NULL, "
           "percentage_of_body_weight numeric     NULL, "
           "variant_mask              integer     NOT NULL, "
           "sort_order                integer     NOT NULL, "
           "created_at                timestamptz NOT NULL, "
           "updated_at                timestamptz NOT NULL, "
           (format "%s                integer     NOT NULL, " col-updated-count)
           "deleted_at                timestamptz NULL)")
     tbl-movement]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                  serial      NOT NULL, "
           (format "movement_id integer     NOT NULL REFERENCES %s (id), " tbl-movement)
           "alias               text        UNIQUE NOT NULL, "
           "created_at          timestamptz NOT NULL, "
           "updated_at          timestamptz NOT NULL, "
           (format "%s          integer     NOT NULL, " col-updated-count)
           "deleted_at          timestamptz NULL, "
           "PRIMARY KEY (movement_id, id))")
     tbl-movement-alias]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           (format "movement_id integer     NOT NULL REFERENCES %s (id), " tbl-movement)
           (format "muscle_id   integer     NOT NULL REFERENCES %s (id), " tbl-muscle)
           "created_at          timestamptz NOT NULL, "
           "updated_at          timestamptz NOT NULL, "
           (format "%s          integer     NOT NULL, " col-updated-count)
           "deleted_at          timestamptz NULL, "
           "PRIMARY KEY (movement_id, muscle_id))")
     tbl-movement-primary-muscle]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           (format "movement_id integer     NOT NULL REFERENCES %s (id), " tbl-movement)
           (format "muscle_id   integer     NOT NULL REFERENCES %s (id), " tbl-muscle)
           "created_at          timestamptz NOT NULL, "
           "updated_at          timestamptz NOT NULL, "
           (format "%s          integer     NOT NULL, " col-updated-count)
           "deleted_at          timestamptz NULL, "
           "PRIMARY KEY (movement_id, muscle_id))")
     tbl-movement-secondary-muscle]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id              serial      PRIMARY KEY,"
           "name            text        NOT NULL, "
           "attributes_mask integer     NOT NULL, "
           "created_at      timestamptz NOT NULL, "
           "updated_at      timestamptz NOT NULL, "
           (format "%s      integer     NOT NULL, " col-updated-count)
           "deleted_at      timestamptz NULL)")
     tbl-cardio-type]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                serial      PRIMARY KEY,"
           "name              text        NOT NULL, "
           "hb_bmr_multiplier numeric     NOT NULL, " ; Harris-Benedict
           "created_at        timestamptz NOT NULL, "
           "updated_at        timestamptz NOT NULL, "
           (format "%s        integer     NOT NULL, " col-updated-count)
           "deleted_at        timestamptz NULL)")
     tbl-cardio-intensity]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id         serial      PRIMARY KEY,"
           "name       text        NOT NULL, "
           "pace_mph   numeric     NOT NULL, "
           "created_at timestamptz NOT NULL, "
           "updated_at timestamptz NOT NULL, "
           (format "%s integer     NOT NULL, " col-updated-count)
           "deleted_at timestamptz NULL)")
     tbl-walking-pace]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                    serial      PRIMARY KEY, "
           (format "user_id       integer     NOT NULL REFERENCES %s (id), " uddl/tbl-user-account)
           "dob                   timestamptz NULL, "
           "gender_male           boolean     NULL, "
           "weight_uom            integer     NOT NULL, "
           "size_uom              integer     NOT NULL, "
           "distance_uom          integer     NOT NULL, "
           "weight_inc_dec_amount integer     NOT NULL, "
           "created_at            timestamptz NOT NULL, "
           "updated_at            timestamptz NOT NULL, "
           (format "%s            integer     NOT NULL, " col-updated-count)
           "deleted_at            timestamptz NULL)")
     tbl-user-settings]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                      serial      PRIMARY KEY, "
           (format "user_id         integer     NOT NULL REFERENCES %s (id), " uddl/tbl-user-account)
           (format "muscle_group_id integer     NOT NULL REFERENCES %s (id), " tbl-muscle-group)
           "logged_at               timestamptz NOT NULL, "
           "created_at              timestamptz NOT NULL, "
           "updated_at              timestamptz NOT NULL, "
           (format "%s              integer     NOT NULL, " col-updated-count)
           "deleted_at              timestamptz NULL)")
     tbl-soreness]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                  serial      PRIMARY KEY, "
           (format "user_id     integer     NOT NULL REFERENCES %s (id), " uddl/tbl-user-account)
           "logged_at           timestamptz NOT NULL, "
           "body_weight         numeric     NULL, "
           "body_weight_uom     integer     NULL, "
           "body_fat_percentage numeric     NULL, "
           "arm_size            numeric     NULL, "
           "calf_size           numeric     NULL, "
           "height              numeric     NULL, "
           "chest_size          numeric     NULL, "
           "size_uom            integer     NULL, "
           "created_at          timestamptz NOT NULL, "
           "updated_at          timestamptz NOT NULL, "
           (format "%s          integer     NOT NULL, " col-updated-count)
           "deleted_at          timestamptz NULL)")
     tbl-body-journal-log]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                  serial      PRIMARY KEY, "
           (format "user_id     integer     NOT NULL REFERENCES %s (id), " uddl/tbl-user-account)
           "created_at          timestamptz NOT NULL, "
           "updated_at          timestamptz NOT NULL, "
           (format "%s          integer     NOT NULL, " col-updated-count)
           "deleted_at          timestamptz NULL)")
     tbl-superset]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                          serial      PRIMARY KEY, "
           (format "user_id             integer     NOT NULL REFERENCES %s (id), " uddl/tbl-user-account)
           (format "movement_id         integer     NOT NULL REFERENCES %s (id), " tbl-movement)
           (format "movement_variant_id integer     REFERENCES %s (id), " tbl-movement-variant)
           (format "superset_id         integer     REFERENCES %s (id), " tbl-superset)
           "num_reps                    integer     NOT NULL, "
           "weight                      integer     NOT NULL, "
           "weight_uom                  integer     NOT NULL, "
           "negatives                   boolean     NOT NULL, "
           "to_failure                  boolean     NOT NULL, "
           "logged_at                   timestamptz NULL, "
           "ignore_time                 boolean     NOT NULL, "
           "created_at                  timestamptz NOT NULL, "
           "updated_at                  timestamptz NOT NULL, "
           (format "%s                  integer     NOT NULL, " col-updated-count)
           "deleted_at                  timestamptz NULL)")
     tbl-set]]
   [apply-ddl-updateable-table
    [#(str (format "CREATE TABLE IF NOT EXISTS %s (" %)
           "id                          serial      PRIMARY KEY, "
           (format "user_id             integer     NOT NULL REFERENCES %s (id), " uddl/tbl-user-account)
           (format "cardio_type_id      integer     NOT NULL REFERENCES %s (id), " tbl-cardio-type)
           (format "cardio_intensity_id integer              REFERENCES %s (id), " tbl-cardio-intensity)
           "duration                    numeric     NULL, "
           "started_at                  timestamptz NULL, "
           "ended_at                    timestamptz NULL, "
           "pace                        numeric     NULL, "
           "heart_rate                  integer     NULL, "
           "added_weight                integer     NULL, "
           "added_weight_uom            integer     NULL, "
           "distance                    numeric     NULL, "
           "distance_uom                integer     NULL, "
           "created_at                  timestamptz NOT NULL, "
           "updated_at                  timestamptz NOT NULL, "
           (format "%s                  integer     NOT NULL, " col-updated-count)
           "deleted_at                  timestamptz NULL)")
     tbl-cardio-session]]
   [apply-ddl-trigger
    ["make_user_settings"
     #(str (format "CREATE FUNCTION %s() RETURNS TRIGGER AS '" %)
           "BEGIN "
           (format "INSERT INTO %s (user_id, size_uom, distance_uom, weight_uom, weight_inc_dec_amount, created_at, updated_at, updated_count) values (NEW.ID, %s, %s, %s, %s, NEW.created_at, NEW.updated_at, NEW.updated_count);"
                   tbl-user-settings
                   config/r-default-user-setting-size-uom
                   config/r-default-user-setting-distance-uom
                   config/r-default-user-setting-weight-uom
                   config/r-default-user-setting-weight-inc-dec-amount)
           "RETURN NEW; "
           "END; "
           "' LANGUAGE 'plpgsql'")
     #(str (format "CREATE TRIGGER user_settings_trigger AFTER INSERT ON %s "
                   uddl/tbl-user-account)
           (format "FOR EACH ROW EXECUTE PROCEDURE %s();" %))]]])
