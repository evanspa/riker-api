(ns riker-app.rest.meta
  (:require [riker-app.rest.utils.meta :as rumeta]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- make-mt-subtype-fn
  [mt]
  (fn [mt-subtype-prefix]
    (str mt-subtype-prefix mt)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Media type vars
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; ref data media types
(def mt-subtype-body-segment              (make-mt-subtype-fn "bodysegment"))
(def mt-subtype-muscle-group              (make-mt-subtype-fn "musclegroup"))
(def mt-subtype-muscle                    (make-mt-subtype-fn "muscle"))
(def mt-subtype-muscle-alias              (make-mt-subtype-fn "musclealias"))
(def mt-subtype-movement                  (make-mt-subtype-fn "movement"))
(def mt-subtype-movement-variant          (make-mt-subtype-fn "movementvariant"))
(def mt-subtype-movement-alias            (make-mt-subtype-fn "movementalias"))
(def mt-subtype-refdata-changelog         (make-mt-subtype-fn "refdatachangelog"))
(def mt-subtype-origination-device        (make-mt-subtype-fn "originationdevice"))
(def mt-subtype-plan                      (make-mt-subtype-fn "plan"))
; user data media types
(def mt-subtype-usersettings       (make-mt-subtype-fn "usersettings"))
(def mt-subtype-set                (make-mt-subtype-fn "set"))
(def mt-subtype-stripe-token       (make-mt-subtype-fn "stripetoken"))
(def mt-subtype-body-journal-log   (make-mt-subtype-fn "bodyjournallog"))
(def mt-subtype-soreness           (make-mt-subtype-fn "sorenesslog"))
(def mt-subtype-userdata-changelog (make-mt-subtype-fn "userdatachangelog"))
(def mt-subtype-stripe-token       (make-mt-subtype-fn "stripetoken"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The versions of this REST API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v001 "0.0.1")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Link relations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; ref data relations
(def r-body-segment-relation      :bodysegment)
(def r-muscle-group-relation      :musclegroup)
(def r-muscle-relation            :muscle)
(def r-muscle-alias-relation      :musclealias)
(def r-movement-relation          :movement)
(def r-movement-variant-relation  :movement-variant)
(def r-movement-alias-relation    :movement-alias)
(def r-refdata-changelog-relation :refdatachangelog)
(def r-origination-device-relation :origination-device)
(def r-plan-relation               :plan)
; user data relations
(def r-usersettings-relation       :usersettings)
(def r-sets-relation               :sets)
(def r-sets-file-import-relation   :setsfileimport)
(def r-stripe-tokens-relation      :stripetokens)
(def r-body-journal-logs-relation  :bodyjournallogs)
(def r-body-journal-logs-file-import-relation :bodyjournallogsfileimport)
(def r-sorenesses-relation         :sorenesses)
(def r-userdata-changelog-relation :userdatachangelog)
(def r-stripe-tokens-relation      :stripetokens)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; URL path components
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; ref data path components
(def pathcomp-body-segments       "bodysegments")
(def pathcomp-muscle-groups       "musclegroups")
(def pathcomp-muscles             "muscles")
(def pathcomp-muscle-aliases      "musclealiases")
(def pathcomp-movements           "movements")
(def pathcomp-movement-variants   "movementvariants")
(def pathcomp-movement-aliases    "movementaliases")
(def pathcomp-refdata-changelog   "refdatachangelog")
(def pathcomp-origination-devices "originationdevices")
(def pathcomp-plan                "plan")
; user data path components
(def pathcomp-usersettings       "usersettings")
(def pathcomp-sets               "sets")
(def pathcomp-sets-file-import   "setsfileimport")
(def pathcomp-stripe-tokens      "stripetokens")
(def pathcomp-body-journal-logs  "bodyjournallogs")
(def pathcomp-body-journal-logs-file-import "bodyjournallogsfileimport")
(def pathcomp-sorenesses         "sorenesses")
(def pathcomp-userdata-changelog "userdatachangelog")
(def pathcomp-stripe-tokens      "stripetokens")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Information about this REST API, including supported content
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn supported-media-types
  [mt-subtype-prefix]
  "Convenient data structure that succinctly captures the set of media types
   (including version and format indicators) supported by this REST API."
  (let [versions {:versions {v001 {:format-inds #{"edn" "json"}}}}
        assoc-mt (fn [m mt-subtype-fn] (assoc m (mt-subtype-fn mt-subtype-prefix) versions))]
    {rumeta/mt-type
     {:subtypes
      (-> {}
          (assoc-mt mt-subtype-origination-device)
          (assoc-mt mt-subtype-plan)
          (assoc-mt mt-subtype-body-segment)
          (assoc-mt mt-subtype-muscle-group)
          (assoc-mt mt-subtype-muscle)
          (assoc-mt mt-subtype-muscle-alias)
          (assoc-mt mt-subtype-movement)
          (assoc-mt mt-subtype-movement-variant)
          (assoc-mt mt-subtype-movement-alias)
          (assoc-mt mt-subtype-usersettings)
          (assoc-mt mt-subtype-set)
          (assoc-mt mt-subtype-stripe-token)
          (assoc-mt mt-subtype-body-journal-log)
          (assoc-mt mt-subtype-soreness)
          (assoc-mt mt-subtype-stripe-token))}}))
