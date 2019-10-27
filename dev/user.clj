(ns user
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.java.jdbc :as j]
            [clojure.data.json :as json]
            [clojure.test :as test]
            [clojure.stacktrace :refer (e)]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [clj-http.client :as client]
            [ring.server.standalone :refer (serve)]
            [clj-stripe.common :as stripecommon]
            [clj-stripe.customers :as customers]
            [clj-stripe.invoices :as invoices]
            [clj-stripe.charges :as charges]
            [pe-core-utils.core :as ucore]
            [riker-app.core.jdbc :as jcore]
            [riker-app.rest.utils.core :refer [*retry-after*]]
            [riker-app.core.user-ddl :as uddl]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.validation :as val]
            [riker-app.core.dao :as dao]
            [riker-app.core.ddl :as rddl]
            [riker-app.utils :as utils]
            [riker-app.app.endpoint :as endpoint]
            [riker-app.app.config :as config]
            [riker-app.app.jobs :as jobs]
            [riker-app.app.lifecycle :as lifecycle]
            [riker-app.core.data-loading :as dl]
            [riker-app.core.ref-data :as rd]
            [fake-data :as fd]))

(def server (atom nil))

(defn- create-and-start-server
  []
  (serve endpoint/riker-app {:port 3006 :open-browser? false :auto-reload? true}))

(defn- go-with-db-init []
  (println "Proceeding to init the database")
  (lifecycle/init-database)
  (lifecycle/init)
  (reset! server (create-and-start-server))
  (println "Jetty server restarted."))

(defn- go []
  (reset! server (create-and-start-server))
  (println "Jetty server restarted."))

(defn reset
  ([] (reset nil))
  ([init-db]
   (let [go-fn-name (if (nil? init-db) 'user/go 'user/go-with-db-init)]
     (when (not (nil? @server))
       (println "Proceeding to stop server")
       (.stop @server))
     (refresh-all :after go-fn-name))))

(defn busy []
  (alter-var-root (var *retry-after*) (fn [_] (t/now))))

(defn not-busy []
  (alter-var-root (var *retry-after*) (fn [_] nil)))

(defn start []
  (.start @server))

(defn stop []
  (.stop @server))

(defn logout
  [user-id token]
  (usercore/invalidate-user-token config/db-spec user-id token usercore/invalrsn-logout))

(defn now->psql
  ""
  []
  (int (/ (c/to-long (t/now)) 1000)))

(defn maintenance-now [] (utils/schedule-maintenance-with-txn config/db-spec 0 1))
