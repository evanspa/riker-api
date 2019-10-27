(ns riker-app.rest.resource.set.version.sets-import-res-v001
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
            [riker-app.rest.resource.set.set-utils :as setresutils]
            [riker-app.rest.resource.set.sets-import-res :refer [do-sets-import-fn
                                                                 body-data-in-transform-fn]]))

(def comma-re #",")

(def heading-logged-unix-time      "Logged Unix Time")
(def heading-movement-id           "Movement ID")
(def heading-variant-id            "Variant ID")
(def heading-weight                "Weight")
(def heading-weight-uom-id         "Weight UOM ID")
(def heading-reps                  "Reps")
(def heading-negatives             "Negatives?")
(def heading-to-failure            "To Failure?")
(def heading-ignore-time           "Ignore Time Component?")
(def heading-origination-device-id "Origination Device ID")

(defn to-dec
  [val-str]
  (if (or (= val-str "null")
          (nil? val-str)
          (= (count val-str) 0))
    nil
    (bigdec val-str)))

(defn to-long
  [val-str]
  (if (or (= val-str "null")
          (nil? val-str)
          (= (count val-str) 0))
    nil
    (Long. val-str)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   sets-csv-str]
  (identity sets-csv-str))

(defn validate-headings
  [headings]
  (let [headings-map (rikerutils/coll->map headings)
        c? (fn [k] (contains? headings-map k))]
    (and (c? heading-logged-unix-time)
         (c? heading-movement-id)
         (c? heading-variant-id)
         (c? heading-weight)
         (c? heading-weight-uom-id)
         (c? heading-reps)
         (c? heading-negatives)
         (c? heading-to-failure)
         (c? heading-ignore-time)
         (c? heading-origination-device-id))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Set importer function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod do-sets-import-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   base-url
   entity-uri-prefix
   send-verification-email-uri
   plaintext-auth-token
   sets-csv-str-post-as-do-body
   merge-embedded-fn
   merge-links-fn]
  (let [user (:user ctx)
        is-not-verified (nil? (:user/verified-at user))
        max-import-allowed (:user/max-allowed-set-import user)
        num-imported (dao/num-imported-sets db-spec user-id)]
    (if (> num-imported max-import-allowed)
      (throw (IllegalArgumentException. (str val/set-imported-limit-exceeded)))
      (if is-not-verified
        (throw (IllegalArgumentException. (str val/set-imported-unverified-email)))
        (try
          (let [br (java.io.BufferedReader. (java.io.StringReader. sets-csv-str-post-as-do-body))
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
                         _
                         movement-id-str
                         _
                         variant-id-str
                         weight-str
                         _
                         weight-uom-id-str
                         to-failure-str
                         negatives-str
                         ignore-time-str
                         reps-str
                         _
                         origination-device-id-str] csv-data
                        new-set-id (dao/next-set-id db-spec)
                        variant-id (to-long variant-id-str)]
                    (dao/save-new-set db-spec
                                      user-id
                                      new-set-id
                                      {:set/logged-at (Long. (clojure.string/replace logged-unix-time-str #"'" ""))
                                       :set/imported-at now
                                       :set/weight (to-dec weight-str)
                                       :set/weight-uom (Long. weight-uom-id-str)
                                       :set/num-reps (Long. reps-str)
                                       :set/origination-device-id (Long. origination-device-id-str)
                                       :set/movement-id (Long. movement-id-str)
                                       :set/to-failure (Boolean/parseBoolean to-failure-str)
                                       :set/negatives (Boolean/parseBoolean negatives-str)
                                       :set/ignore-time (Boolean/parseBoolean ignore-time-str)
                                       :set/movement-variant-id variant-id})))
                {:status 204})
              (do
                (log/error "set import file contained invalid heading(s)")
                {:status 500})))
          (catch Throwable t
            (rikerutils/log-e t "exception in do-sets-import-fn/v001")
            {:status 500}))))))
