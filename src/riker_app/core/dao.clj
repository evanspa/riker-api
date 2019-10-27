(ns riker-app.core.dao
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as j]
            [clojure.string :as str]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.jdbc :as jcore]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.ddl :as rddl]
            [riker-app.core.user-ddl :as uddl]
            [riker-app.core.validation :as val]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Next ID helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn next-id-maker
  [tbl]
  (fn [db-spec]
    (jcore/seq-next-val db-spec (format "%s_id_seq" tbl))))

(def next-superset-id         (next-id-maker rddl/tbl-superset))
(def next-set-id              (next-id-maker rddl/tbl-set))
(def next-body-journal-log-id (next-id-maker rddl/tbl-body-journal-log))
(def next-cardio-session-id   (next-id-maker rddl/tbl-cardio-session))
(def next-soreness-id         (next-id-maker rddl/tbl-soreness))
(def next-stripe-token-id     (next-id-maker rddl/tbl-stripe-token))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; clj-time formatters
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def excel-format (f/formatter "yyyy-MM-dd hh:mm:ss"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; General helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def from-sql-time-fn #(c/from-sql-time %))

(defn concat-common-fields
  [fields]
  (vec (concat fields [{:field :id}
                       {:field :updated_count}
                       {:field :updated_at
                        :to-model-conv-fn from-sql-time-fn
                        :to-db-conv-fn c/to-timestamp}
                       {:field :deleted_at
                        :to-model-conv-fn from-sql-time-fn
                        :to-db-conv-fn c/to-timestamp}
                       {:field :created_at
                        :to-model-conv-fn from-sql-time-fn
                        :to-db-conv-fn c/to-timestamp}])))

(defn make-keyword->hypenated-ns-keyword-fn
  [target-ns]
  (fn [key]
    (keyword target-ns (str/replace (name key) "_" "-"))))

(defn make-rs->model-fn
  [db-fields id-field default-to-model-key-transformer-fn]
  (fn [rs]
    [(get rs id-field)
     (reduce (fn [target {db-field :field
                          conv-fn :to-model-conv-fn
                          key-transformer-fn :to-model-key-transformer}]
               (let [key-transformer-fn (if (not (nil? key-transformer-fn))
                                          key-transformer-fn
                                          default-to-model-key-transformer-fn)
                     args [target rs db-field (key-transformer-fn db-field)]
                     args (if (not (nil? conv-fn)) (conj args conv-fn) args)]
                 (merge target
                        (apply ucore/assoc-if-contains args))))
             {}
             db-fields)]))

(defn model-db-keypairs
  [db-fields default-to-db-key-transformer-fn]
  (reduce (fn [target {db-field :field
                       conv-fn :to-db-conv-fn
                       key-transformer-fn :to-db-key-transformer}]
            (let [key-transformer-fn (if (not (nil? key-transformer-fn))
                                       key-transformer-fn
                                       default-to-db-key-transformer-fn)
                  pair-vec [(key-transformer-fn db-field) db-field]
                  pair-vec (if (not (nil? conv-fn))
                             (conj pair-vec conv-fn)
                             pair-vec)]
              (conj target pair-vec)))
          []
          db-fields))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Aggregate helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn tbl-count
  [db-spec tbl]
  (:num (first (j/query db-spec [(format "select count(id) as num from %s" tbl)]))))

(defn total-set-count [db-spec] (tbl-count db-spec rddl/tbl-set))
(defn total-superset-count [db-spec] (tbl-count db-spec rddl/tbl-superset))
(defn total-soreness-count [db-spec] (tbl-count db-spec rddl/tbl-soreness))
(defn total-body-journal-log-count [db-spec] (tbl-count db-spec rddl/tbl-body-journal-log))
(defn total-cardio-session-count [db-spec] (tbl-count db-spec rddl/tbl-cardio-session))

(defn num-imported
  [db-spec tbl user-id]
  (:num (first (j/query db-spec
                        [(format "select count(id) as num from %s where user_id = ? and imported_at is not null and deleted_at is null"
                                 tbl)
                         user-id]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Origination device-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def origination-device-keyword-namespace "originationdevice")
(def origination-device-domain-fields [{:field :name}
                                       {:field :icon_image_name}])
(def rs->origination-device (make-rs->model-fn (concat-common-fields origination-device-domain-fields)
                                               :id
                                               (make-keyword->hypenated-ns-keyword-fn origination-device-keyword-namespace)))

(defn origination-devices [db-spec] (jcore/load-entities db-spec rddl/tbl-origination-device rs->origination-device true))

(defn origination-devices-modified-since
  [db-spec modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-origination-device
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :originationdevice/id
                                 :originationdevice/deleted-at
                                 :originationdevice/updated-at
                                 rs->origination-device))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Body Segment-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def body-segment-keyword-namespace "bodysegment")
(def body-segment-domain-fields [{:field :name}])
(def rs->body-segment (make-rs->model-fn (concat-common-fields body-segment-domain-fields)
                                         :id
                                         (make-keyword->hypenated-ns-keyword-fn body-segment-keyword-namespace)))

(defn body-segments [db-spec] (jcore/load-entities db-spec rddl/tbl-body-segment rs->body-segment true))

(defn body-segments-modified-since
  [db-spec modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-body-segment
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :bodysegment/id
                                 :bodysegment/deleted-at
                                 :bodysegment/updated-at
                                 rs->body-segment))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Muscle Group-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def muscle-group-keyword-namespace "musclegroup")
(def muscle-group-domain-fields [{:field :body_segment_id}
                                 {:field :name}
                                 {:field :abbrev_name}])
(def rs->muscle-group (make-rs->model-fn (concat-common-fields muscle-group-domain-fields)
                                         :id
                                         (make-keyword->hypenated-ns-keyword-fn muscle-group-keyword-namespace)))

(defn muscle-groups [db-spec] (jcore/load-entities db-spec rddl/tbl-muscle-group rs->muscle-group true))

(defn muscle-groups-modified-since
  [db-spec modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-muscle-group
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :musclegroup/id
                                 :musclegroup/deleted-at
                                 :musclegroup/updated-at
                                 rs->muscle-group))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Muscle-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def muscle-keyword-namespace "muscle")
(def muscle-domain-fields [{:field :muscle_group_id}
                           {:field :canonical_name}
                           {:field :abbrev_canonical_name}])
(def rs->muscle (make-rs->model-fn (concat-common-fields muscle-domain-fields)
                                   :id
                                   (make-keyword->hypenated-ns-keyword-fn muscle-keyword-namespace)))

(defn assoc-muscle-aliases
  [muscles db-spec]
  (let [aliases-fn (fn [muscle-id tbl] (vec (j/query db-spec
                                                     [(format "select id from %s where muscle_id = ?" tbl) muscle-id]
                                                     {:row-fn #(get % :id)})))]
    (reduce (fn [results [muscle-id muscle]]
              (conj results
                    [muscle-id (-> muscle
                                   (assoc :muscle/alias-ids
                                          (aliases-fn muscle-id rddl/tbl-muscle-alias)))]))
            []
            muscles)))

(defn muscles
  [db-spec]
  (assoc-muscle-aliases
   (jcore/load-entities db-spec rddl/tbl-muscle rs->muscle true)
   db-spec))

(defn muscles-modified-since
  [db-spec modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-muscle
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :muscle/id
                                 :muscle/deleted-at
                                 :muscle/updated-at
                                 rs->muscle))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Muscle Alias-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def muscle-alias-keyword-namespace "musclealias")
(def muscle-alias-domain-fields [{:field :muscle_id}
                                 {:field :alias}])
(def rs->muscle-alias (make-rs->model-fn (concat-common-fields muscle-alias-domain-fields)
                                         :id
                                         (make-keyword->hypenated-ns-keyword-fn muscle-alias-keyword-namespace)))

(defn muscle-aliases [db-spec] (jcore/load-entities db-spec rddl/tbl-muscle-alias rs->muscle-alias true))

(defn muscle-aliases-modified-since
  [db-spec modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-muscle-alias
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :musclealias/id
                                 :musclealias/deleted-at
                                 :musclealias/updated-at
                                 rs->muscle-alias))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Movement Variant-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def movement-variant-keyword-namespace "movementvariant")
(def movement-variant-domain-fields [{:field :name}
                                     {:field :abbrev_name}
                                     {:field :description}
                                     {:field :sort_order}])
(def rs->movement-variant (make-rs->model-fn (concat-common-fields movement-variant-domain-fields)
                                             :id
                                             (make-keyword->hypenated-ns-keyword-fn movement-variant-keyword-namespace)))

(defn movement-variants [db-spec] (jcore/load-entities db-spec rddl/tbl-movement-variant rs->movement-variant true))

(defn movement-variants-modified-since
  [db-spec modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-movement-variant
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :movementvariant/id
                                 :movementvariant/deleted-at
                                 :movementvariant/updated-at
                                 rs->movement-variant))

(defn movement-variant-by-id
  ([db-spec movement-variant-id]
   (movement-variant-by-id db-spec movement-variant-id true))
  ([db-spec movement-variant-id active-only]
   (jcore/load-entity-by-col db-spec
                             rddl/tbl-movement-variant
                             "id"
                             "="
                             movement-variant-id
                             rs->movement-variant
                             active-only)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Movement-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def movement-keyword-namespace "movement")
(def movement-domain-fields [{:field :canonical_name}
                             {:field :sort_order}
                             {:field :is_body_lift}
                             {:field :percentage_of_body_weight}
                             {:field :variant_mask}])
(def rs->movement (make-rs->model-fn (concat-common-fields movement-domain-fields)
                                     :id
                                     (make-keyword->hypenated-ns-keyword-fn movement-keyword-namespace)))

(defn assoc-movement-muscles-and-aliases
  [movements db-spec]
  (let [muscles-fn (fn [mov-id tbl] (vec (j/query db-spec
                                                  [(format "select muscle_id from %s where movement_id = ?" tbl) mov-id]
                                                  {:row-fn #(get % :muscle_id)})))
        aliases-fn (fn [mov-id] (vec (j/query db-spec
                                              [(format "select alias from %s where movement_id = ?" rddl/tbl-movement-alias) mov-id]
                                              {:row-fn #(get % :alias)})))]
    (reduce (fn [results [mov-id mov]]
              (conj results
                    [mov-id (-> mov
                                (assoc :movement/primary-muscle-ids
                                       (muscles-fn mov-id rddl/tbl-movement-primary-muscle))
                                (assoc :movement/secondary-muscle-ids
                                       (muscles-fn mov-id rddl/tbl-movement-secondary-muscle))
                                (assoc :movement/aliases
                                       (aliases-fn mov-id)))]))
            []
            movements)))

(defn movements
  [db-spec]
  (assoc-movement-muscles-and-aliases
   (jcore/load-entities db-spec rddl/tbl-movement rs->movement true)
   db-spec))

(defn movements-modified-since
  [db-spec modified-since]
  (let [results (jcore/entities-modified-since db-spec
                                               rddl/tbl-movement
                                               "updated_at"
                                               "deleted_at"
                                               modified-since
                                               :movement/id
                                               :movement/deleted-at
                                               :movement/updated-at
                                               rs->movement)
        aug-results (assoc-movement-muscles-and-aliases (get results :entities) db-spec)]
    (assoc results :entities aug-results)))

(defn movement-by-id
  ([db-spec movement-id]
   (movement-by-id db-spec movement-id true))
  ([db-spec movement-id active-only]
   (let [movement (jcore/load-entity-by-col db-spec
                                            rddl/tbl-movement
                                            "id"
                                            "="
                                            movement-id
                                            rs->movement
                                            active-only)]
     (when (not (nil? movement))
       (first (assoc-movement-muscles-and-aliases [movement] db-spec))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Movement Alias-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def movement-alias-keyword-namespace "movementalias")
(def movement-alias-domain-fields [{:field :movement_id}
                                   {:field :alias}])
(def rs->movement-alias (make-rs->model-fn (concat-common-fields movement-alias-domain-fields)
                                           :id
                                           (make-keyword->hypenated-ns-keyword-fn movement-alias-keyword-namespace)))

(defn movement-aliases [db-spec] (jcore/load-entities db-spec rddl/tbl-movement-alias rs->movement-alias true))

(defn movement-aliases-modified-since
  [db-spec modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-movement-alias
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :movementalias/id
                                 :movementalias/deleted-at
                                 :movementalias/updated-at
                                 rs->movement-alias))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cardio Type-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def cardio-type-keyword-namespace "cardiotype")
(def cardio-type-domain-fields [{:field :name}
                                {:field :attributes_mask}])
(def rs->cardio-type (make-rs->model-fn (concat-common-fields cardio-type-domain-fields)
                                        :id
                                        (make-keyword->hypenated-ns-keyword-fn cardio-type-keyword-namespace)))

(defn cardio-types [db-spec]
  (jcore/load-entities db-spec rddl/tbl-cardio-type rs->cardio-type true))

(defn cardio-types-modified-since
  [db-spec modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-cardio-type
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :cardiotype/id
                                 :cardiotype/deleted-at
                                 :cardiotype/updated-at
                                 rs->cardio-type))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Walking Pace-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def walking-pace-keyword-namespace "walkingpace")
(def walking-pace-domain-fields [{:field :name}
                                 {:field :pace_mph}])
(def rs->walking-pace (make-rs->model-fn (concat-common-fields walking-pace-domain-fields)
                                         :id
                                         (make-keyword->hypenated-ns-keyword-fn walking-pace-keyword-namespace)))

(defn walking-paces [db-spec] (jcore/load-entities db-spec rddl/tbl-walking-pace rs->walking-pace true))

(defn walking-paces-modified-since
  [db-spec modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-walking-pace
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :walkingpace/id
                                 :walkingpace/deleted-at
                                 :walkingpace/updated-at
                                 rs->walking-pace))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Superset-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def superset-keyword-namespace "superset")
(def superset-domain-fields [{:field :user_id}
                             {:field :origination_device_id}])
(def rs->superset (make-rs->model-fn (concat-common-fields superset-domain-fields)
                                     :id
                                     (make-keyword->hypenated-ns-keyword-fn superset-keyword-namespace)))
(def superset-key-pairs (model-db-keypairs superset-domain-fields
                                           (make-keyword->hypenated-ns-keyword-fn superset-keyword-namespace)))

(defn superset-by-id
  ([db-spec superset-id]
   (superset-by-id db-spec superset-id true))
  ([db-spec superset-id active-only]
   (jcore/load-entity-by-col db-spec rddl/tbl-superset "id" "=" superset-id rs->superset active-only)))

(defn save-new-superset
  [db-spec user-id new-superset-id superset]
  (jcore/save-new-entity db-spec
                         new-superset-id
                         superset
                         nil
                         val/sset-any-issues
                         superset-by-id
                         :superset
                         superset-key-pairs
                         {:user_id user-id}
                         :superset/created-at
                         :superset/updated-at
                         nil
                         nil))

(defn save-superset
  ([db-spec superset-id superset]
   (save-superset db-spec superset-id superset nil))
  ([db-spec superset-id superset if-unmodified-since]
   (jcore/save-entity db-spec
                      superset-id
                      superset
                      nil
                      val/sset-any-issues
                      superset-by-id
                      :superset
                      superset-key-pairs
                      :superset/updated-at
                      nil
                      nil
                      if-unmodified-since)))

(defn supersets-for-user
  ([db-spec user-id]
   (supersets-for-user db-spec user-id true))
  ([db-spec user-id active-only]
   (jcore/load-entities-by-col db-spec
                               rddl/tbl-superset
                               "user_id"
                               "="
                               user-id
                               "created_at"
                               "desc"
                               rs->superset
                               active-only)))

(defn supersets-modified-since
  [db-spec user-id modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-superset
                                 "user_id"
                                 "="
                                 user-id
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :superset/id
                                 :superset/deleted-at
                                 :superset/updated-at
                                 rs->superset))

(defn superset-by-id
  ([db-spec superset-id]
   (superset-by-id db-spec superset-id true))
  ([db-spec superset-id active-only]
   (jcore/load-entity-by-col db-spec
                             rddl/tbl-superset
                             "id"
                             "="
                             superset-id
                             rs->superset
                             active-only)))

(defn mark-superset-as-deleted
  [db-spec superset-id if-unmodified-since]
  (jcore/mark-entity-as-deleted db-spec
                                superset-id
                                superset-by-id
                                :superset
                                :superset/updated-at
                                if-unmodified-since))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Body journal log-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def body-journal-log-keyword-namespace "bodyjournallog")
(def body-journal-log-domain-fields [{:field :user_id}
                                     {:field :origination_device_id}
                                     {:field :body_weight}
                                     {:field :body_weight_uom}
                                     {:field :arm_size}
                                     {:field :chest_size}
                                     {:field :calf_size}
                                     {:field :neck_size}
                                     {:field :waist_size}
                                     {:field :thigh_size}
                                     {:field :forearm_size}
                                     {:field :size_uom}
                                     {:field :imported_at
                                      :to-model-conv-fn from-sql-time-fn
                                      :to-db-conv-fn c/to-timestamp}
                                     {:field :logged_at
                                      :to-model-conv-fn from-sql-time-fn
                                      :to-db-conv-fn c/to-timestamp}])
(def rs->body-journal-log (make-rs->model-fn (concat-common-fields body-journal-log-domain-fields)
                                         :id
                                         (make-keyword->hypenated-ns-keyword-fn body-journal-log-keyword-namespace)))
(def body-journal-log-key-pairs (model-db-keypairs body-journal-log-domain-fields
                                               (make-keyword->hypenated-ns-keyword-fn body-journal-log-keyword-namespace)))

(defn body-journal-log-by-id
  ([db-spec body-journal-log-id]
   (body-journal-log-by-id db-spec body-journal-log-id true))
  ([db-spec body-journal-log-id active-only]
   (jcore/load-entity-by-col db-spec
                             rddl/tbl-body-journal-log
                             "id"
                             "="
                             body-journal-log-id
                             rs->body-journal-log
                             active-only)))

(defn num-imported-body-journal-logs
  [db-spec user-id]
  (num-imported db-spec rddl/tbl-body-journal-log user-id))

(defn save-new-body-journal-log
  [db-spec user-id new-body-journal-log-id body-journal-log]
  (jcore/save-new-entity db-spec
                         new-body-journal-log-id
                         body-journal-log
                         nil
                         val/body-journal-log-any-issues
                         body-journal-log-by-id
                         :body_journal_log
                         body-journal-log-key-pairs
                         {:user_id user-id}
                         :bodyjournallog/created-at
                         :bodyjournallog/updated-at
                         nil
                         nil))

(defn save-body-journal-log
  ([db-spec body-journal-log-id body-journal-log]
   (save-body-journal-log db-spec body-journal-log-id body-journal-log nil))
  ([db-spec body-journal-log-id body-journal-log if-unmodified-since]
   (jcore/save-entity db-spec
                      body-journal-log-id
                      body-journal-log
                      nil
                      val/body-journal-log-any-issues
                      body-journal-log-by-id
                      :body_journal_log
                      body-journal-log-key-pairs
                      :bodyjournallog/updated-at
                      nil
                      nil
                      if-unmodified-since)))

(defn body-journal-logs-for-user
  ([db-spec user-id]
   (body-journal-logs-for-user db-spec user-id true))
  ([db-spec user-id active-only]
   (jcore/load-entities-by-col db-spec
                               rddl/tbl-body-journal-log
                               "user_id"
                               "="
                               user-id
                               "logged_at"
                               "desc"
                               rs->body-journal-log
                               active-only)))

(defn body-journal-logs-modified-since
  [db-spec user-id modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-body-journal-log
                                 "user_id"
                                 "="
                                 user-id
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :bodyjournallog/id
                                 :bodyjournallog/deleted-at
                                 :bodyjournallog/updated-at
                                 rs->body-journal-log))


(defn mark-body-journal-log-as-deleted
  [db-spec body-journal-log-id if-unmodified-since]
  (jcore/mark-entity-as-deleted db-spec
                                body-journal-log-id
                                body-journal-log-by-id
                                :body_journal_log
                                :bodyjournallog/updated-at
                                if-unmodified-since))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cardio session-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def cardio-session-keyword-namespace "cardio")
(def cardio-session-domain-fields [{:field :user_id}
                                   {:field :cardio_type_id}
                                   {:field :origination_device_id}
                                   {:field :duration}
                                   {:field :pace}
                                   {:field :heart_rate}
                                   {:field :distance}
                                   {:field :distance_uom}
                                   {:field :added_weight}
                                   {:field :added_weight_uom}
                                   {:field :started_at
                                    :to-model-conv-fn from-sql-time-fn
                                    :to-db-conv-fn c/to-timestamp}
                                   {:field :ended_at
                                    :to-model-conv-fn from-sql-time-fn
                                    :to-db-conv-fn c/to-timestamp}])
(def rs->cardio-session (make-rs->model-fn (concat-common-fields cardio-session-domain-fields)
                                           :id
                                           (make-keyword->hypenated-ns-keyword-fn cardio-session-keyword-namespace)))
(def cardio-session-key-pairs (model-db-keypairs cardio-session-domain-fields
                                                 (make-keyword->hypenated-ns-keyword-fn cardio-session-keyword-namespace)))

(defn cardio-session-by-id
  ([db-spec cardio-session-id]
   (cardio-session-by-id db-spec cardio-session-id true))
  ([db-spec cardio-session-id active-only]
   (jcore/load-entity-by-col db-spec
                             rddl/tbl-cardio-session
                             "id"
                             "="
                             cardio-session-id
                             rs->cardio-session
                             active-only)))

(defn save-new-cardio-session
  [db-spec user-id new-cardio-session-id cardio-session]
  (jcore/save-new-entity db-spec
                         new-cardio-session-id
                         cardio-session
                         nil
                         val/cardio-any-issues
                         cardio-session-by-id
                         :cardio_session
                         cardio-session-key-pairs
                         {:user_id user-id}
                         :cardio/created-at
                         :cardio/updated-at
                         nil
                         nil))

(defn save-cardio-session
  ([db-spec cardio-session-id cardio-session]
   (save-cardio-session db-spec cardio-session-id cardio-session nil))
  ([db-spec cardio-session-id cardio-session if-unmodified-since]
   (jcore/save-entity db-spec
                      cardio-session-id
                      cardio-session
                      nil
                      val/cardio-any-issues
                      cardio-session-by-id
                      :cardio_session
                      cardio-session-key-pairs
                      :cardio/updated-at
                      nil
                      nil
                      if-unmodified-since)))

(defn cardio-sessions-for-user
  ([db-spec user-id]
   (cardio-sessions-for-user db-spec user-id true))
  ([db-spec user-id active-only]
   (jcore/load-entities-by-col db-spec
                               rddl/tbl-cardio-session
                               "user_id"
                               "="
                               user-id
                               "started_at"
                               "desc"
                               rs->cardio-session
                               active-only)))

(defn cardio-sessions-modified-since
  [db-spec user-id modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-cardio-session
                                 "user_id"
                                 "="
                                 user-id
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :cardio/id
                                 :cardio/deleted-at
                                 :cardio/updated-at
                                 rs->cardio-session))

(defn mark-cardio-session-as-deleted
  [db-spec cardio-session-id if-unmodified-since]
  (jcore/mark-entity-as-deleted db-spec
                                cardio-session-id
                                cardio-session-by-id
                                :cardio_session
                                :cardio/updated-at
                                if-unmodified-since))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Settings-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def usersettings-keyword-namespace "usersettings")
(def usersettings-domain-fields [{:field :user_id}
                                 {:field :distance_uom}
                                 {:field :size_uom}
                                 {:field :weight_uom}
                                 {:field :weight_inc_dec_amount}])
(def rs->usersettings (make-rs->model-fn (concat-common-fields usersettings-domain-fields)
                                         :id
                                         (make-keyword->hypenated-ns-keyword-fn usersettings-keyword-namespace)))
(def usersettings-key-pairs (model-db-keypairs usersettings-domain-fields
                                               (make-keyword->hypenated-ns-keyword-fn usersettings-keyword-namespace)))

(defn usersettings-by-id
  ([db-spec usersettings-id]
   (usersettings-by-id db-spec usersettings-id true))
  ([db-spec usersettings-id active-only]
   (jcore/load-entity-by-col db-spec
                             rddl/tbl-user-settings
                             "id"
                             "="
                             usersettings-id
                             rs->usersettings
                             active-only)))

(defn save-usersettings
  ([db-spec usersettings-id usersettings]
   (save-usersettings db-spec usersettings-id usersettings nil))
  ([db-spec usersettings-id usersettings if-unmodified-since]
   (jcore/save-entity db-spec
                      usersettings-id
                      usersettings
                      nil
                      val/usersettings-any-issues
                      usersettings-by-id
                      :user_settings
                      usersettings-key-pairs
                      :usersettings/updated-at
                      nil
                      nil
                      if-unmodified-since)))

(defn usersettings-for-user
  ([db-spec user-id]
   (usersettings-for-user db-spec user-id true))
  ([db-spec user-id active-only]
   (jcore/load-entities-by-col db-spec
                               rddl/tbl-user-settings
                               "user_id"
                               "="
                               user-id
                               nil ; order by col
                               nil ; order by direction
                               rs->usersettings
                               active-only)))

(defn usersettings-modified-since
  [db-spec user-id modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-user-settings
                                 "user_id"
                                 "="
                                 user-id
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :usersettings/id
                                 :usersettings/deleted-at
                                 :usersettings/updated-at
                                 rs->usersettings))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plan-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def plan-keyword-namespace "plan")
(def plan-domain-fields [{:field :current_plan_price}])
(def rs->plan (make-rs->model-fn (concat-common-fields plan-domain-fields)
                                 nil
                                 (make-keyword->hypenated-ns-keyword-fn plan-keyword-namespace)))
(def plan-key-pairs (model-db-keypairs plan-domain-fields
                                       (make-keyword->hypenated-ns-keyword-fn plan-keyword-namespace)))

(defn plan
  [db-spec]
  (jcore/load-entity db-spec
                     uddl/tbl-current-subscription-plan-info
                     rs->plan))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Set-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def set-keyword-namespace "set")
(def set-domain-fields [{:field :user_id}
                        {:field :movement_id}
                        {:field :movement_variant_id}
                        {:field :superset_id}
                        {:field :origination_device_id}
                        {:field :num_reps}
                        {:field :weight}
                        {:field :weight_uom}
                        {:field :negatives}
                        {:field :to_failure}
                        {:field :ignore_time}
                        {:field :imported_at
                         :to-model-conv-fn from-sql-time-fn
                         :to-db-conv-fn c/to-timestamp}
                        {:field :logged_at
                         :to-model-conv-fn from-sql-time-fn
                         :to-db-conv-fn c/to-timestamp}])
(def rs->set (make-rs->model-fn (concat-common-fields set-domain-fields)
                                :id
                                (make-keyword->hypenated-ns-keyword-fn set-keyword-namespace)))
(def set-key-pairs (model-db-keypairs set-domain-fields
                                      (make-keyword->hypenated-ns-keyword-fn set-keyword-namespace)))

(defn set-deps
  ([db-spec]
   (set-deps db-spec :set/superset-id))
  ([db-spec superset-id-or-key]
   [[#(superset-by-id db-spec %) superset-id-or-key val/set-superset-does-not-exist true]]))

(defn set-by-id
  ([db-spec set-id]
   (set-by-id db-spec set-id true))
  ([db-spec set-id active-only]
   (jcore/load-entity-by-col db-spec
                             rddl/tbl-set
                             "id"
                             "="
                             set-id
                             rs->set
                             active-only)))

(defn num-imported-sets
  [db-spec user-id]
  (num-imported db-spec rddl/tbl-set user-id))

(defn save-new-set
  [db-spec user-id new-set-id set]
  (jcore/save-new-entity db-spec
                         new-set-id
                         set
                         nil
                         val/set-any-issues
                         set-by-id
                         :set
                         set-key-pairs
                         {:user_id user-id}
                         :set/created-at
                         :set/updated-at
                         nil
                         (set-deps db-spec
                                   (:set/superset-id set))))

(defn save-set
  ([db-spec set-id set]
   (save-set db-spec set-id set nil))
  ([db-spec set-id set if-unmodified-since]
   (jcore/save-entity db-spec
                      set-id
                      set
                      nil
                      val/set-any-issues
                      set-by-id
                      :set
                      set-key-pairs
                      :set/updated-at
                      nil
                      (set-deps db-spec)
                      if-unmodified-since)))

(defn sets-for-user
  ([db-spec user-id]
   (sets-for-user db-spec user-id true))
  ([db-spec user-id active-only]
   (jcore/load-entities-by-col db-spec
                               rddl/tbl-set
                               "user_id"
                               "="
                               user-id
                               "logged_at"
                               "desc"
                               rs->set
                               active-only)))

(defn sets-modified-since
  [db-spec user-id modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-set
                                 "user_id"
                                 "="
                                 user-id
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :set/id
                                 :set/deleted-at
                                 :set/updated-at
                                 rs->set))

(defn mark-set-as-deleted
  [db-spec set-id if-unmodified-since]
  (jcore/mark-entity-as-deleted db-spec
                                set-id
                                set-by-id
                                :set
                                :set/updated-at
                                if-unmodified-since))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Soreness-related definitions.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def soreness-keyword-namespace "soreness")
(def soreness-domain-fields [{:field :user_id}
                             {:field :origination_device_id}
                             {:field :muscle_group_id}
                             {:field :logged_at
                              :to-model-conv-fn from-sql-time-fn
                              :to-db-conv-fn c/to-timestamp}])
(def rs->soreness (make-rs->model-fn (concat-common-fields soreness-domain-fields)
                                     :id
                                     (make-keyword->hypenated-ns-keyword-fn soreness-keyword-namespace)))
(def soreness-key-pairs (model-db-keypairs soreness-domain-fields
                                           (make-keyword->hypenated-ns-keyword-fn soreness-keyword-namespace)))

(defn soreness-by-id
  ([db-spec soreness-id]
   (soreness-by-id db-spec soreness-id true))
  ([db-spec soreness-id active-only]
   (jcore/load-entity-by-col db-spec
                             rddl/tbl-soreness
                             "id"
                             "="
                             soreness-id
                             rs->soreness
                             active-only)))

(defn save-new-soreness
  [db-spec user-id new-soreness-id soreness]
  (jcore/save-new-entity db-spec
                         new-soreness-id
                         soreness
                         nil
                         val/sore-any-issues
                         soreness-by-id
                         :soreness_log
                         soreness-key-pairs
                         {:user_id user-id}
                         :soreness/created-at
                         :soreness/updated-at
                         nil
                         nil))

(defn save-soreness
  ([db-spec soreness-id soreness]
   (save-soreness db-spec soreness-id soreness nil))
  ([db-spec soreness-id soreness if-unmodified-since]
   (jcore/save-entity db-spec
                      soreness-id
                      soreness
                      nil
                      val/sore-any-issues
                      soreness-by-id
                      :soreness_log
                      soreness-key-pairs
                      :soreness/updated-at
                      nil
                      nil
                      if-unmodified-since)))

(defn sorenesses-for-user
  ([db-spec user-id]
   (sorenesses-for-user db-spec user-id true))
  ([db-spec user-id active-only]
   (jcore/load-entities-by-col db-spec
                               rddl/tbl-soreness
                               "user_id"
                               "="
                               user-id
                               "logged_at"
                               "desc"
                               rs->soreness
                               active-only)))

(defn sorenesses-modified-since
  [db-spec user-id modified-since]
  (jcore/entities-modified-since db-spec
                                 rddl/tbl-soreness
                                 "user_id"
                                 "="
                                 user-id
                                 "updated_at"
                                 "deleted_at"
                                 modified-since
                                 :soreness/id
                                 :soreness/deleted-at
                                 :soreness/updated-at
                                 rs->soreness))

(defn mark-soreness-as-deleted
  [db-spec soreness-id if-unmodified-since]
  (jcore/mark-entity-as-deleted db-spec
                                soreness-id
                                soreness-by-id
                                :soreness_log
                                :soreness/updated-at
                                if-unmodified-since))
