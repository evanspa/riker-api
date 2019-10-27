(ns riker-app.rest.resource.bodyjournallog.version.journals-import-res-v001
  (:require [clojure.tools.logging :as log]
            [clojure.string :refer [split]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.utils :as rikerutils]
            [riker-app.core.dao :as dao]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.bodyjournallog.journals-import-res :refer [do-body-journal-logs-import-fn
                                                                                body-data-in-transform-fn]]))

(def comma-re #",")

(def heading-logged-unix-time      "Logged Unix Time")
(def heading-body-weight           "Body Weight")
(def heading-body-weight-uom-id    "Body Weight UOM ID")
(def heading-calf-size             "Calf Size")
(def heading-chest-size            "Chest Size")
(def heading-arm-size              "Arm Size")
(def heading-neck-size             "Neck Size")
(def heading-waist-size            "Waist Size")
(def heading-thigh-size            "Thigh Size")
(def heading-forearm-size          "Forearm Size")
(def heading-size-uom-id           "Size UOM ID")
(def heading-origination-device-id "Origination Device ID")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   body-journal-logs-csv-str]
  (identity body-journal-logs-csv-str))

(defn validate-headings
  [headings]
  (let [headings-map (rikerutils/coll->map headings)
        c? (fn [k] (contains? headings-map k))]
    (and (c? heading-logged-unix-time)
         (c? heading-body-weight)
         (c? heading-body-weight-uom-id)
         (c? heading-calf-size)
         (c? heading-chest-size)
         (c? heading-arm-size)
         (c? heading-neck-size)
         (c? heading-waist-size)
         (c? heading-thigh-size)
         (c? heading-forearm-size)
         (c? heading-size-uom-id)
         (c? heading-origination-device-id))))

(defn to-dec
  [val-str]
  (if (or (= val-str "null")
          (nil? val-str)
          (= (count val-str) 0))
    nil
    (bigdec val-str)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Body Journal Log importer function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod do-body-journal-logs-import-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   base-url
   entity-uri-prefix
   send-verification-email-uri
   plaintext-auth-token
   body-journal-logs-csv-str-post-as-do-body
   merge-embedded-fn
   merge-links-fn]
  (let [user (:user ctx)
        is-not-verified (nil? (:user/verified-at user))
        max-import-allowed (:user/max-allowed-bml-import user)
        num-imported (dao/num-imported-body-journal-logs db-spec user-id)]
    (if (> num-imported max-import-allowed)
      (throw (IllegalArgumentException. (str val/body-journal-log-imported-limit-exceeded)))
      (if is-not-verified
        (throw (IllegalArgumentException. (str val/body-journal-log-imported-unverified-email)))
        (try
          (let [br (java.io.BufferedReader. (java.io.StringReader. body-journal-logs-csv-str-post-as-do-body))
                all-lines (line-seq br)
                headings-csv-line (first all-lines)
                csv-data-lines (rest all-lines)
                now (t/now)
                headings (split headings-csv-line comma-re)]
            (if (validate-headings headings)
              (do
                (doseq [csv-data-line csv-data-lines]
                  (let [csv-data (split csv-data-line comma-re)
                        [_
                         logged-unix-time-str
                         body-weight-str
                         _
                         body-weight-uom-id-str
                         calf-size-str
                         chest-size-str
                         arm-size-str
                         neck-size-str
                         waist-size-str
                         thigh-size-str
                         forearm-size-str
                         _
                         size-uom-id-str
                         _
                         origination-device-id-str] csv-data
                        new-body-journal-log-id (dao/next-body-journal-log-id db-spec)]
                    (dao/save-new-body-journal-log db-spec
                                                   user-id
                                                   new-body-journal-log-id
                                                   {:bodyjournallog/logged-at (Long. (clojure.string/replace logged-unix-time-str #"'" ""))
                                                    :bodyjournallog/imported-at now
                                                    :bodyjournallog/body-weight (to-dec body-weight-str)
                                                    :bodyjournallog/body-weight-uom (Long. body-weight-uom-id-str)
                                                    :bodyjournallog/calf-size (to-dec calf-size-str)
                                                    :bodyjournallog/chest-size (to-dec chest-size-str)
                                                    :bodyjournallog/arm-size (to-dec arm-size-str)
                                                    :bodyjournallog/neck-size (to-dec neck-size-str)
                                                    :bodyjournallog/waist-size (to-dec waist-size-str)
                                                    :bodyjournallog/thigh-size (to-dec thigh-size-str)
                                                    :bodyjournallog/forearm-size (to-dec forearm-size-str)
                                                    :bodyjournallog/size-uom (Long. size-uom-id-str)
                                                    :bodyjournallog/origination-device-id (Long. origination-device-id-str)})))
                {:status 204})
              (do
                (log/error "body journal log import file contained invalid heading(s)")
                {:status 500})))
          (catch Throwable t
            (rikerutils/log-e t "exception in do-body-journal-logs-import-fn/v001")
            {:status 500}))))))
