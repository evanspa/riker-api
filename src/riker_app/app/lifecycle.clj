(ns riker-app.app.lifecycle
  (:require [clojure.tools.logging :as log]
            [clojure.tools.nrepl.server :as nrepl]
            [clojure.java.jdbc :as j]
            [environ.core :refer [env]]
            [riker-app.core.jdbc :as jcore]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as qt]
            [clojurewerkz.quartzite.jobs :as qj]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojurewerkz.quartzite.schedule.daily-interval :refer [schedule
                                                                    with-interval-in-days
                                                                    on-every-day
                                                                    starting-daily-at
                                                                    time-of-day]]
            [riker-app.core.user-ddl :as uddl]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.ddl :as ddl]
            [riker-app.core.data-loading :as rdata]
            [riker-app.core.ddl :as rddl]
            [riker-app.app.jobs :as jobs]
            [riker-app.app.config :as config]))

(def nrepl-server)

; "12" was deployed to production on 03/22/2017
; "13" was deployed to production on 04/08/2017
; "14" was deployed to production on 05/30/2017
; "15" was deployed to production on 06/04/2017
; "16" was deployed to production on 06/08/2017
; "17" was deployed to production on 06/26/2017
; "18" was deployed to production on 07/06/2017
; "19" was deployed to production on 08/28/2017
; "20" was deployed to production on 09/23/2017
; "21" was deployed to production on 10/07/2017
; "22" was deployed to production on 04/08/2018
; "23" was deployed to production on 07/14/2018
; "24" was NOT deployed to production yet
(def target-schema-version 24)

(def ddl-operations
  {0 (fn []
       ;; User / auth-token setup
       (j/db-do-commands config/db-spec
                         true
                         [uddl/schema-version-ddl
                          uddl/v0-create-user-account-ddl
                          uddl/v0-add-unique-constraint-user-account-email
                          uddl/v0-add-unique-constraint-user-account-username
                          uddl/v0-create-authentication-token-ddl
                          uddl/v1-user-add-deleted-reason-col
                          uddl/v1-user-add-suspended-at-col
                          uddl/v1-user-add-suspended-reason-col
                          uddl/v1-user-add-suspended-count-col
                          uddl/v2-create-email-verification-token-ddl
                          uddl/v3-create-password-reset-token-ddl
                          uddl/v4-password-reset-token-add-used-at-col])
       (jcore/with-try-catch-exec-as-query config/db-spec
         (uddl/v0-create-updated-count-inc-trigger-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (uddl/v0-create-user-account-updated-count-trigger-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (uddl/v1-create-suspended-count-inc-trigger-fn config/db-spec))
       (jcore/with-try-catch-exec-as-query config/db-spec
         (uddl/v1-create-user-account-suspended-count-trigger-fn config/db-spec))

       ;; v0 DDL
       (ddl/apply-ddl-collection config/db-spec ddl/v0-ddl)
       (rdata/v0-data-loads config/db-spec))

   1 (fn [] (rdata/v1-data-loads config/db-spec))
   2 (fn []
       (ddl/apply-ddl-collection config/db-spec ddl/v2-ddl)
       (rdata/v2-data-loads config/db-spec))
   3 (fn []
       (ddl/apply-ddl-collection config/db-spec ddl/v3-ddl)
       (rdata/v3-data-loads config/db-spec))
   4 (fn []
       (ddl/apply-ddl-collection config/db-spec ddl/v4-ddl)
       (rdata/v4-data-loads config/db-spec))
   5 (fn []
       (rdata/v5-data-loads config/db-spec))
   6 (fn []
       (ddl/apply-ddl-collection config/db-spec ddl/v6-ddl)
       (rdata/v6-data-loads config/db-spec))
   7 (fn []
       (rdata/v7-data-loads config/db-spec))
   8 (fn []
       (ddl/apply-ddl-collection config/db-spec ddl/v8-ddl)
       (rdata/v8-data-loads config/db-spec))
   9 (fn []
       (ddl/apply-ddl-collection config/db-spec ddl/v9-ddl))
   10 (fn []
        (ddl/apply-ddl-collection config/db-spec ddl/v10-ddl))
   11 (fn []
        (ddl/apply-ddl-collection config/db-spec ddl/v11-ddl)
        (rdata/v11-data-loads config/db-spec))
   12 (fn []
        (ddl/apply-ddl-collection config/db-spec ddl/v12-ddl)
        (rdata/v12-data-loads config/db-spec))
   13 (fn []
        (ddl/apply-ddl-collection config/db-spec ddl/v13-ddl))
   14 (fn []
        (rdata/v14-data-loads config/db-spec))
   15 (fn []
        (ddl/apply-ddl-collection config/db-spec ddl/v15-ddl))
   16 (fn []
        (ddl/apply-ddl-collection config/db-spec ddl/v16-ddl))
   17 (fn []
        (rdata/v17-data-loads config/db-spec))
   18 (fn []
        (rdata/v18-data-loads config/db-spec))
   19 (fn []
        (rdata/v19-data-loads config/db-spec))
   20 (fn []
        (rdata/v20-data-loads config/db-spec))
   21 (fn []
        (rdata/v21-data-loads config/db-spec))
   22 (fn []
        (rdata/v22-data-loads config/db-spec))
   23 (fn []
        (ddl/apply-ddl-collection config/db-spec ddl/v23-ddl))
   24 (fn []
        (ddl/apply-ddl-collection config/db-spec ddl/v24-ddl))
   })

(defn init-database
  []
  ;; Database setup
  (log/info (format "Proceeding to setup database (app version=[%s])" config/r-app-version))

  ;; Create schema version table
  (j/db-do-commands config/db-spec
                    true
                    [uddl/schema-version-ddl])

  ;; Apply DDL operations
  (let [current-schema-version (usercore/get-schema-version config/db-spec)]
    (if (nil? current-schema-version)
      (let [do-upper-bound (inc target-schema-version)]
        (log/info (format "Current schema version installed is nil.  Proceeding to apply DDL operations through target schema version: [%d]"
                          target-schema-version))
        (dotimes [version do-upper-bound]
          (let [ddl-fn (get ddl-operations version)]
            (log/info (format "Proceeding to apply version [%d] DDL updates." version))
            (ddl-fn))))
      (let [do-upper-bound (- target-schema-version current-schema-version)]
        (log/info (format "Current schema version installed: [%d].  Proceeding to apply DDL operations through target schema version: [%d]."
                          current-schema-version
                          target-schema-version))
        (dotimes [version do-upper-bound]
          (let [version-key (+ version (inc current-schema-version))
                ddl-fn (get ddl-operations version-key)]
            (log/info (format "Proceeding to apply version [%d] DDL updates." version-key))
            (ddl-fn)
            (log/info (format  "Version [%d] DDL updates applied." version-key))))))
    (usercore/set-schema-version config/db-spec target-schema-version)
    (log/info (format  "Schema version table updated to value: [%d]." target-schema-version))))

(defn init []
  (log/info (format "Proceeding to start Riker App server (version=[%s])." config/r-app-version))
  (init-database)
  (log/info (format "Proceeding to start nrepl-server at port: [%s]" config/r-nrepl-server-port))
  (defonce nrepl-server
    (nrepl/start-server :port (Integer/valueOf config/r-nrepl-server-port)))
  (log/info "Proceeding to schedule quartz jobs")
  (let [s (-> (qs/initialize) qs/start)]
    (qs/schedule s
                 jobs/trial-almost-expired-notices-job
                 (jobs/make-daily-trigger "triggers.trial.almost.expired.notices" 1 3)) ; 3am
    (qs/schedule s
                 jobs/validate-iap-subscriptions-job
                 (jobs/make-daily-trigger "validate-iap-subscription.notices" 1 4)) ; 4am
    (qs/schedule s
                 jobs/aggregate-stats-job
                 (jobs/make-daily-trigger "triggers.aggregate.stats" 1 12)) ; Noon
    )
)

(defn stop []
  (log/info (format "Proceeding to stop Riker App server (version=[%s])." config/r-app-version))
  (nrepl/stop-server nrepl-server))
