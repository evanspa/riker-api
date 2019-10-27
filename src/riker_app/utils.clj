(ns riker-app.utils
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as j]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [clj-http.client :as client]
            [riker-app.app.config :as config]
            [riker-app.core.user-ddl :as uddl])
  (:import java.util.Base64))

(defn decode-base64Url [to-decode]
  (String. (.decode (Base64/getUrlDecoder) to-decode)))

(defn format-pennies-amount
  [amount]
  (format "%.2f" (* 0.01 (float amount))))

(defn coll->map
  [coll]
  (reduce (fn [m val]
            (assoc m val val))
          {}
          coll))

(defn log-e
  [e msg]
  (if (instance? java.sql.SQLException e)
    (let [next-exception (.getNextException e)]
      (if (not (nil? next-exception))
        (log/error next-exception msg)
        (log/error e msg)))
    (log/error e msg)))

(defn schedule-maintenance
  [conn in-days duration]
  (j/delete! conn uddl/tbl-maintenance-window [])
  (let [now (t/now)
        now-sql (c/to-timestamp now)]
    (j/insert! conn
               uddl/tbl-maintenance-window
               {:informed_of_maintenance_at now-sql
                :maintenance_starts_at (c/to-timestamp (t/plus now (t/days in-days)))
                :maintenance_duration duration})))

(defn schedule-maintenance-with-txn
  [db-spec in-days duration]
  (j/with-db-transaction [conn db-spec]
    (schedule-maintenance conn in-days duration)))

(defn set-current-plan-price
  [conn price-in-cents-per-year]
  (j/delete! conn uddl/tbl-current-subscription-plan-info [])
  (j/insert! conn
             uddl/tbl-current-subscription-plan-info
             {:current_plan_price price-in-cents-per-year}))

(defn iap-latest-receipt-info
  [receipt-data validation-url]
  (let [http-resp
        (client/post validation-url
                     {:accept :json
                      :content-type :json
                      :headers {"Accept-Language" "en-US"
                                "Accept-Charset" "UTF-8"}
                      :form-params {:password config/r-app-store-receipt-validation-shared-secret
                                    :receipt-data receipt-data}})]
    (when (= (:status http-resp) 200)
      (let [parsed-body (json/read-str (:body http-resp))]
        (get parsed-body "latest_receipt_info")))))

(def iap-receipt-date-formatter (f/formatter "yyyy-MM-dd HH:mm:ss ZZZ"))

(defn parse-iap-date
  [date-str]
  (f/parse iap-receipt-date-formatter date-str))
