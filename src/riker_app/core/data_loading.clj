(ns riker-app.core.data-loading
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as j]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.utils :as utils]
            [riker-app.core.jdbc :as jcore]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.ddl :as rddl]
            [riker-app.core.ref-data :refer :all]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn assoc-now
  [entity]
  (let [now-sql (c/to-timestamp (t/now))]
    (-> entity
        (assoc :updated_count 1)
        (assoc :created_at now-sql)
        (assoc :updated_at now-sql))))

(defn ins-origination-device
  [db-spec name icon-image-name]
  (j/insert! db-spec
             rddl/tbl-origination-device
             (assoc-now {:name name
                         :icon_image_name icon-image-name})))

(defn ins-body-segment
  [db-spec body-segment]
  (j/insert! db-spec
             rddl/tbl-body-segment
             (assoc-now body-segment)))

(defn ins-muscle-group
  [db-spec muscle-group]
  (j/insert! db-spec
             rddl/tbl-muscle-group
             (assoc-now muscle-group)))

(defn ins-movement-variant
  [db-spec movement-variant]
  (j/insert! db-spec
             rddl/tbl-movement-variant
             (assoc-now movement-variant)))

(defn ins-muscle
  ([db-spec muscle]
   (ins-muscle db-spec muscle false))
  ([db-spec muscle aliases-only]
   (when (not aliases-only)
     (j/insert! db-spec
                rddl/tbl-muscle
                (-> muscle
                    (assoc-now)
                    (dissoc :aliases))))
   (let [muscle-id (:id muscle)]
     (doseq [alias (:aliases muscle)]
       (j/insert! db-spec
                  rddl/tbl-muscle-alias
                  (-> alias
                      (dissoc :id) ; 'id' field should not be explicitly defined
                                   ; since we changed PK to be type 'serial'
                      (assoc-now)
                      (assoc :muscle_id muscle-id)))))))

(defn ins-movement
  ([db-spec movement]
   (ins-movement db-spec movement false))
  ([db-spec movement aliases-only]
   ;(log/debug "about to insert movement: " movement)
   (when (not aliases-only)
     (j/insert! db-spec
                rddl/tbl-movement
                (-> movement
                    (assoc-now)
                    (dissoc :aliases)
                    (dissoc :primary-muscles)
                    (dissoc :secondary-muscles))))
   (let [movement-id (:id movement)]
     (doseq [alias (:aliases movement)]
       (j/insert! db-spec
                  rddl/tbl-movement-alias
                  (-> alias
                      (assoc-now)
                      (dissoc :id) ; 'id' field should not be explicitly defined
                                   ; since we changed PK to be type 'serial'
                      (assoc :movement_id movement-id))))
     (when (not aliases-only)
       (doseq [primary-muscle (:primary-muscles movement)]
         (j/insert! db-spec
                    rddl/tbl-movement-primary-muscle
                    (-> primary-muscle
                        (assoc-now)
                        (assoc :movement_id movement-id))))
       (doseq [secondary-muscle (:secondary-muscles movement)]
         (j/insert! db-spec
                    rddl/tbl-movement-secondary-muscle
                    (-> secondary-muscle
                        (assoc-now)
                        (assoc :movement_id movement-id))))))))

(defn ins-cardio-type
  [db-spec cardio-type]
  (j/insert! db-spec
             rddl/tbl-cardio-type
             (assoc-now cardio-type)))

(defn ins-walking-pace
  [db-spec walking-pace]
  (j/insert! db-spec
             rddl/tbl-walking-pace
             (assoc-now walking-pace)))

(defn ins-movements
  ([db-spec movements]
   (ins-movements db-spec movements false))
  ([db-spec movements aliases-only]
   (doseq [movement movements]
     (ins-movement db-spec movement aliases-only))))

(defn touch-movement
  [db-spec id updated-at-sql]
  (j/update! db-spec rddl/tbl-movement {:updated_at updated-at-sql} ["id = ?" id]))

(defn insert-mov-alias
  [db-spec mov-id alias updated-at-sql]
  (j/insert! db-spec
             rddl/tbl-movement-alias
             {:updated_at updated-at-sql
              :created_at updated-at-sql
              :updated_count 1
              :movement_id mov-id
              :alias alias}))

(defn update-variants-mask
  [db-spec mov-id variants-mask updated-at-sql]
  (j/update! db-spec
             rddl/tbl-movement
             {:updated_at updated-at-sql
              :variant_mask variants-mask}
             ["id = ?" mov-id]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v0 Data Loads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v0-data-loads
  [db-spec]

  ; Walking paces
  (ins-walking-pace db-spec walking-pace-leisure)
  (ins-walking-pace db-spec walking-pace-brisk)
  (ins-walking-pace db-spec walking-pace-speed)

  ; cardio types
  (ins-cardio-type db-spec cardio-type-other)
  (ins-cardio-type db-spec cardio-type-dancing)
  (ins-cardio-type db-spec cardio-type-elliptical)
  (ins-cardio-type db-spec cardio-type-group-exercise)
  (ins-cardio-type db-spec cardio-type-jacobs-ladder)
  (ins-cardio-type db-spec cardio-type-jumping-rope)
  (ins-cardio-type db-spec cardio-type-rowing)
  (ins-cardio-type db-spec cardio-type-running)
  (ins-cardio-type db-spec cardio-type-boxing)
  (ins-cardio-type db-spec cardio-type-spinning)
  (ins-cardio-type db-spec cardio-type-sports)
  (ins-cardio-type db-spec cardio-type-stair-climber)
  (ins-cardio-type db-spec cardio-type-swimming)
  (ins-cardio-type db-spec cardio-type-cycling)
  (ins-cardio-type db-spec cardio-type-walking)

  ; insert body segments
  (ins-body-segment db-spec bs-upper-body)
  (ins-body-segment db-spec bs-lower-body)
  (ins-body-segment db-spec bs-core)

  ; insert muscle groups
  (ins-muscle-group db-spec mg-shoulders)
  (ins-muscle-group db-spec mg-back)
  (ins-muscle-group db-spec mg-chest)
  (ins-muscle-group db-spec mg-abs)
  (ins-muscle-group db-spec mg-serratus)
  (ins-muscle-group db-spec mg-quadriceps)
  (ins-muscle-group db-spec mg-hamstrings)
  (ins-muscle-group db-spec mg-calfs)
  (ins-muscle-group db-spec mg-triceps)
  (ins-muscle-group db-spec mg-biceps)
  (ins-muscle-group db-spec mg-forearms)
  (ins-muscle-group db-spec mg-glutes)

  ; insert muscles
  (ins-muscle db-spec m-deltoids)
  (ins-muscle db-spec m-deltoids-rear)
  (ins-muscle db-spec m-deltoids-front)
  (ins-muscle db-spec m-deltoids-side)
  (ins-muscle db-spec m-back)
  (ins-muscle db-spec m-back-upper)
  (ins-muscle db-spec m-back-lower)
  (ins-muscle db-spec m-chest)
  (ins-muscle db-spec m-chest-upper)
  (ins-muscle db-spec m-chest-lower)
  (ins-muscle db-spec m-abs)
  (ins-muscle db-spec m-abs-upper)
  (ins-muscle db-spec m-abs-lower)
  (ins-muscle db-spec m-quadriceps)
  (ins-muscle db-spec m-hamstrings)
  (ins-muscle db-spec m-calfs)
  (ins-muscle db-spec m-triceps)
  (ins-muscle db-spec m-biceps)
  (ins-muscle db-spec m-forearms)
  (ins-muscle db-spec m-traps)
  (ins-muscle db-spec m-serratus)
  (ins-muscle db-spec m-glutes)

  ; insert movement variants
  (ins-movement-variant db-spec variant-barbell)
  (ins-movement-variant db-spec variant-dumbbell)
  (ins-movement-variant db-spec variant-machine)
  (ins-movement-variant db-spec variant-smith-machine)
  (ins-movement-variant db-spec variant-cable)
  (ins-movement-variant db-spec variant-curl-bar)
  (ins-movement-variant db-spec variant-sled)

  ; insert v0 movements
  (ins-movements db-spec v0-movements)
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v1 Data Loads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v1-data-loads
  [db-spec]
  (ins-movements db-spec v1-movements))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v2 Data Loads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v2-data-loads
  [db-spec]
  (ins-origination-device db-spec "Browser" "web")    ;; id=1
  (ins-origination-device db-spec "Pebble" "pebble")) ;; id=2

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v3 Data Loads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v3-data-loads
  [db-spec]
  (j/update! db-spec rddl/tbl-set {:origination_device_id 0} [])
  (j/update! db-spec rddl/tbl-body-journal-log {:origination_device_id 0} []))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v4 Data Loads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v4-data-loads
  [db-spec]
  (ins-movements db-spec v2-movements)
  (ins-movement-variant db-spec variant-body))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v5 Data Loads (forgot to seed maintenance_window table in previous deploy)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v5-data-loads
  [db-spec]
  ; seed with in-past maintenance window
  (utils/schedule-maintenance db-spec -1 10))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v6 Data Loads - need to re-populate alias tables since you dropped them,
;; and re-created them with correct primary keys.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v6-data-loads
  [db-spec]
  ; insert muscle aliases (again)
  (ins-muscle db-spec m-deltoids true)
  (ins-muscle db-spec m-deltoids-rear true)
  (ins-muscle db-spec m-deltoids-front true)
  (ins-muscle db-spec m-deltoids-side true)
  (ins-muscle db-spec m-back true)
  (ins-muscle db-spec m-back-upper true)
  (ins-muscle db-spec m-back-lower true)
  (ins-muscle db-spec m-chest true)
  (ins-muscle db-spec m-chest-upper true)
  (ins-muscle db-spec m-chest-lower true)
  (ins-muscle db-spec m-abs true)
  (ins-muscle db-spec m-abs-upper true)
  (ins-muscle db-spec m-abs-lower true)
  (ins-muscle db-spec m-quadriceps true)
  (ins-muscle db-spec m-hamstrings true)
  (ins-muscle db-spec m-calfs true)
  (ins-muscle db-spec m-triceps true)
  (ins-muscle db-spec m-biceps true)
  (ins-muscle db-spec m-forearms true)
  (ins-muscle db-spec m-traps true)
  (ins-muscle db-spec m-serratus true)
  (ins-muscle db-spec m-glutes true)
  ; insert movement aliases (again)
  (ins-movements db-spec v0-movements true)
  (ins-movements db-spec v1-movements true)
  (ins-movements db-spec v2-movements true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v7 Data Loads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v7-data-loads
  [db-spec]
  (j/update! db-spec
             rddl/tbl-origination-device
             {:icon_image_name "orig-device-www"}
             ["id = ?" 1])
  (j/update! db-spec
             rddl/tbl-origination-device
             {:icon_image_name "orig-device-pebble"}
             ["id = ?" 2])
  (ins-origination-device db-spec "Phone" "orig-device-phone")                  ;; id=3
  (ins-origination-device db-spec "Tablet" "orig-device-tablet")                ;; id=4
  (ins-origination-device db-spec "Apple Watch" "orig-device-apple-watch")      ;; id=5
  (ins-origination-device db-spec "Android Watch" "orig-device-android-watch")) ;; id=6

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v8 Data Loads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v8-data-loads
  [db-spec]
  (let [updated-at-sql (c/to-timestamp (t/now))]
    (j/update! db-spec rddl/tbl-movement-variant {:updated_at updated-at-sql :sort_order 0} ["id = ?" 1]) ;; barbell
    (j/update! db-spec rddl/tbl-movement-variant {:updated_at updated-at-sql :sort_order 1} ["id = ?" 2]) ;; dumbell
    (j/update! db-spec rddl/tbl-movement-variant {:updated_at updated-at-sql :sort_order 2} ["id = ?" 4]) ;; machine
    (j/update! db-spec rddl/tbl-movement-variant {:updated_at updated-at-sql :sort_order 3} ["id = ?" 8]) ;; smith machine
    (j/update! db-spec rddl/tbl-movement-variant {:updated_at updated-at-sql :sort_order 4} ["id = ?" 16]) ;; cable
    (j/update! db-spec rddl/tbl-movement-variant {:updated_at updated-at-sql :sort_order 5} ["id = ?" 32]) ;; curl bar
    (j/update! db-spec rddl/tbl-movement-variant {:updated_at updated-at-sql :sort_order 6} ["id = ?" 64]) ;; sled
    (j/update! db-spec rddl/tbl-movement-variant {:updated_at updated-at-sql :sort_order 7} ["id = ?" 128]) ;; body
    )
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v9, v10 Data Loads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; none

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v11 Data Loads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v11-data-loads
  [db-spec]
  (utils/set-current-plan-price db-spec 1149))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v12 Data Loads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v12-data-loads
  [db-spec]
  (let [updated-at-sql (c/to-timestamp (t/now))]
    (letfn [(abbrev [tbl col abbrev-name id]
              (j/update! db-spec
                         tbl
                         {:updated_at updated-at-sql col abbrev-name}
                         ["id = ?" id]))
            (mv [abbrev-name id]
              (abbrev rddl/tbl-movement-variant :abbrev_name abbrev-name id))
            (mg [abbrev-name id]
              (abbrev rddl/tbl-muscle-group :abbrev_name abbrev-name id))
            (m [abbrev-name id]
              (abbrev rddl/tbl-muscle :abbrev_canonical_name abbrev-name id))]
      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; Update Movement Variants
      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      (mv "smith m." 8)
      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; Update Muscle Groups
      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      (mg "delts" 0)
      (mg "quads" 5)
      (mg "hams" 6)
      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; Update Muscles
      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      (m "delts" 0)
      (m "rear delts" 1)
      (m "front delts" 2)
      (m "side delts" 3)
      (m "quads" 14)
      (m "hams" 15))

    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    ;; Delete "generic" muscle objects
    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    (letfn [(delete-m [id]
              (j/delete! db-spec rddl/tbl-muscle-alias ["muscle_id = ?" id])
              (j/delete! db-spec rddl/tbl-movement-primary-muscle ["muscle_id = ?" id])
              (j/delete! db-spec rddl/tbl-movement-secondary-muscle ["muscle_id = ?" id])
              (j/delete! db-spec rddl/tbl-muscle ["id = ?" id]))
            (ins-muscle [mg-id id name abbrev-name]
              (j/insert! db-spec
                         rddl/tbl-muscle
                         {:updated_at updated-at-sql
                          :created_at updated-at-sql
                          :updated_count 1
                          :muscle_group_id mg-id
                          :id id
                          :canonical_name name
                          :abbrev_canonical_name abbrev-name}))
            (touch-mov [id] (touch-movement db-spec id updated-at-sql))
            (ins-mov-alias [mov-id alias]
              (insert-mov-alias db-spec mov-id alias updated-at-sql))
            (ins-pri-second-m [tbl mov-id m-id]
              (j/insert! db-spec
                         tbl
                         {:updated_at updated-at-sql
                          :created_at updated-at-sql
                          :updated_count 0
                          :movement_id mov-id
                          :muscle_id m-id}))
            (ins-primary-m [mov-id m-id]
              (ins-pri-second-m rddl/tbl-movement-primary-muscle mov-id m-id))
            (ins-secondary-m [mov-id m-id]
              (ins-pri-second-m rddl/tbl-movement-secondary-muscle mov-id m-id))]

      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; Get rid of "core" body segment and get rid of serratus as a muscle
      ;; group, and instead have it just be a muscle within the chest muscle
      ;; group.
      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      (j/update! db-spec
                 rddl/tbl-muscle-group
                 {:updated_at updated-at-sql
                  :body_segment_id 0}
                 ["id = ?" 3]) ; move abs muscle group to upper body segment
      (j/update! db-spec
                 rddl/tbl-muscle
                 {:updated_at updated-at-sql
                  :muscle_group_id 2} ; chest muscle group
                 ["id = ?" 13]) ; serratus muscle
      (j/delete! db-spec rddl/tbl-muscle-group ["id = ?" 4]) ; serratus muscle group
      (j/delete! db-spec rddl/tbl-body-segment ["id = ?" 2]) ; core body segment

      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; For each musclegroup that we want to allow drill-down in, we have to
      ;; blow away the corresponding generic muscle.
      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      (delete-m 0)  ; deltoids (just so happen that it wasn't referenced in any movements)
      (delete-m 4)  ; back
      (delete-m 10) ; abs
      (delete-m 17) ; triceps
      (delete-m 7)  ; chest
      (ins-muscle 8 (:id m-tricep-lateral-head) "lateral head" "lat. head")
      (ins-muscle 8 (:id m-tricep-long-head)    "long head"    "long head")
      (ins-muscle 8 (:id m-tricep-medial-head)  "medial head"  "med. head")

      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; chest
      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      (let [mov-id 0] ; bench press
        (ins-primary-m mov-id 8)  ; upper chest
        (ins-primary-m mov-id 9)  ; lower chest
        (touch-mov mov-id))
      (let [mov-id 6] ; push-up
        (ins-primary-m mov-id 8)  ; upper chest
        (ins-primary-m mov-id 9)  ; lower chest
        (touch-mov mov-id))
      (let [mov-id 7] ; one-arm push-up
        (ins-primary-m mov-id 8)  ; upper chest
        (ins-primary-m mov-id 9)  ; lower chest
        (touch-mov mov-id))
      (let [mov-id 1] ; incline bench
        (ins-secondary-m mov-id 9) ; lower chest
        (touch-mov mov-id))
      (let [mov-id 2] ; decline bench
        (ins-secondary-m mov-id 8) ; upper chest
        (touch-mov mov-id))
      (let [mov-id 3] ; flys
        (ins-primary-m mov-id 8)  ; upper chest
        (ins-primary-m mov-id 9)  ; lower chest
        (touch-mov mov-id))
      (let [mov-id 8] ; wide grip dips
        (ins-primary-m mov-id 8)  ; upper chest
        (ins-primary-m mov-id 9)  ; lower chest
        (touch-mov mov-id))
      (let [mov-id 27] ; dips
        (ins-secondary-m mov-id 8)  ; upper chest
        (ins-secondary-m mov-id 9)  ; lower chest
        (touch-mov mov-id))
      (let [mov-id 28] ; close grip bench
        (ins-secondary-m mov-id 8)  ; upper chest
        (ins-secondary-m mov-id 9)  ; lower chest
        (touch-mov mov-id))

      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; triceps
      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      (let [mov-id 0] ; bench press
        (ins-secondary-m mov-id 22)  ; tri. lateral head
        (ins-secondary-m mov-id 23)  ; tri. long head
        (ins-secondary-m mov-id 24)  ; tri. medial head
        (touch-mov mov-id))
      (let [mov-id 1] ; incline bench press
        (ins-secondary-m mov-id 22)  ; tri. lateral head
        (ins-secondary-m mov-id 23)  ; tri. long head
        (ins-secondary-m mov-id 24)  ; tri. medial head
        (touch-mov mov-id))
      (let [mov-id 2] ; decline bench press
        (ins-secondary-m mov-id 22)  ; tri. lateral head
        (ins-secondary-m mov-id 23)  ; tri. long head
        (ins-secondary-m mov-id 24)  ; tri. medial head
        (touch-mov mov-id))
      (let [mov-id 15] ; shoulder press
        (ins-secondary-m mov-id 22)  ; tri. lateral head
        (ins-secondary-m mov-id 23)  ; tri. long head
        (ins-secondary-m mov-id 24)  ; tri. medial head
        (touch-mov mov-id))
      (let [mov-id 8] ; wide grip dips
        (ins-secondary-m mov-id 22)  ; tri. lateral head
        (ins-secondary-m mov-id 23)  ; tri. long head
        (ins-secondary-m mov-id 24)  ; tri. medial head
        (touch-mov mov-id))
      (let [mov-id 27] ; dips
        (ins-primary-m mov-id 22)  ; tri. lateral head
        (ins-primary-m mov-id 23)  ; tri. long head
        (ins-primary-m mov-id 24)  ; tri. medial head
        (touch-mov mov-id))
      (let [mov-id 28] ; close grip bench press
        (ins-primary-m mov-id 22)  ; tri. lateral head
        (ins-primary-m mov-id 23)  ; tri. long head
        (ins-primary-m mov-id 24)  ; tri. medial head
        (touch-mov mov-id))
      (let [mov-id 29] ; push downs
        (ins-primary-m mov-id 22)   ; tri. lateral head
        (ins-secondary-m mov-id 23) ; tri. long head
        (ins-secondary-m mov-id 24) ; tri. medial head
        (ins-mov-alias mov-id "press-downs")
        (touch-mov mov-id))
      (let [mov-id 30] ; tricep extensions
        (ins-secondary-m mov-id 22) ; tri. lateral head
        (ins-primary-m mov-id 23)   ; tri. long head
        (ins-secondary-m mov-id 24) ; tri. medial head
        (touch-mov mov-id))
      (let [mov-id 6] ; push-up
        (ins-secondary-m mov-id 22)  ; tri. lateral head
        (ins-secondary-m mov-id 23)  ; tri. long head
        (ins-secondary-m mov-id 24)  ; tri. medial head
        (touch-mov mov-id))
      (let [mov-id 7] ; one arm push-up
        (ins-secondary-m mov-id 22)  ; tri. lateral head
        (ins-secondary-m mov-id 23)  ; tri. long head
        (ins-secondary-m mov-id 24)  ; tri. medial head
        (touch-mov mov-id))
      (let [mov-id 16] ; Arnold press
        (ins-secondary-m mov-id 22)  ; tri. lateral head
        (ins-secondary-m mov-id 23)  ; tri. long head
        (ins-secondary-m mov-id 24)  ; tri. medial head
        (touch-mov mov-id))
      (let [mov-id 17] ; military press
        (ins-secondary-m mov-id 22)  ; tri. lateral head
        (ins-secondary-m mov-id 23)  ; tri. long head
        (ins-secondary-m mov-id 24)  ; tri. medial head
        (touch-mov mov-id))
      (let [mov-id 18] ; clean and press
        (ins-secondary-m mov-id 22)  ; tri. lateral head
        (ins-secondary-m mov-id 23)  ; tri. long head
        (ins-secondary-m mov-id 24)  ; tri. medial head
        (touch-mov mov-id))
      (let [mov-id 19] ; push press
        (ins-secondary-m mov-id 22)  ; tri. lateral head
        (ins-secondary-m mov-id 23)  ; tri. long head
        (ins-secondary-m mov-id 24)  ; tri. medial head
        (touch-mov mov-id))

      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; Abs
      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      (let [mov-id 32] ; deadlifts
        (ins-secondary-m mov-id 11) ; upper abs
        (ins-secondary-m mov-id 12) ; lower abs
        (touch-mov mov-id))

      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      ;; Back
      ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
      (let [mov-id 9] ; pulldown
        (ins-primary-m mov-id 5) ; upper back
        (ins-primary-m mov-id 6) ; lower back
        (touch-mov mov-id))
      (let [mov-id 10] ; rows
        (ins-primary-m mov-id 5) ; upper back
        (ins-primary-m mov-id 6) ; lower back
        (touch-mov mov-id))
      (let [mov-id 11] ; t-bar rows
        (ins-primary-m mov-id 5) ; upper back
        (ins-primary-m mov-id 6) ; lower back
        (touch-mov mov-id))
      (let [mov-id 12] ; bent-over rows
        (ins-primary-m mov-id 5) ; upper back
        (ins-primary-m mov-id 6) ; lower back
        (touch-mov mov-id))
      (let [mov-id 13] ; one arm bent-over rows
        (ins-primary-m mov-id 5) ; upper back
        (ins-primary-m mov-id 6) ; lower back
        (touch-mov mov-id))
      (let [mov-id 32] ; deadlifts
        (ins-primary-m mov-id 5) ; upper back
        (ins-primary-m mov-id 6) ; lower back
        (touch-mov mov-id))
      (let [mov-id 39] ; pull ups
        (touch-mov mov-id))
      (let [mov-id 40] ; wide grip pull ups
        (touch-mov mov-id)))

    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    ;; Update Origination Devices
    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    (j/update! db-spec
               rddl/tbl-origination-device
               {:updated_at updated-at-sql :name "iPhone"}
               ["id = ?" 3])
    (j/update! db-spec
               rddl/tbl-origination-device
               {:updated_at updated-at-sql
                :name "iPhone"
                :icon_image_name "orig-device-iphone"}
               ["id = ?" 3])
    (j/update! db-spec
               rddl/tbl-origination-device
               {:updated_at updated-at-sql
                :name "iPad"
                :icon_image_name "orig-device-ipad"}
               ["id = ?" 4])
    (j/update! db-spec
               rddl/tbl-origination-device
               {:updated_at updated-at-sql
                :name "Web"
                :icon_image_name "orig-device-web"}
               ["id = ?" 1])
    )
  (utils/set-current-plan-price db-spec 1149))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v13 Data Loads (was deployed to production on 04/08/2017)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; none

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v14 Data Loads (was deployed to production on 05/30/2017)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v14-data-loads
  [db-spec]
  (ins-movement-variant db-spec variant-kettlebell)
  (ins-movements db-spec v3-movements))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v15 and v16 Data Loads
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; none

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v17 Data Loads (was deployed to production on 06/26/2017)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v17-data-loads
  [db-spec]
  (let [updated-at-sql (c/to-timestamp (t/now))]
    ; Giving calf raises the 'smith machine' variant
    (j/update! db-spec
               rddl/tbl-movement
               {:updated_at updated-at-sql
                :variant_mask 140}
               ["id = ?" 36])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v18 Data Loads (was deployed to production on 07/06/2017)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v18-data-loads
  [db-spec]
  (let [updated-at-sql (c/to-timestamp (t/now))]
    (letfn [(touch-mov [id] (touch-movement db-spec id updated-at-sql))
            (fix-chest-mov [mov-id]
              (j/delete! db-spec
                         rddl/tbl-movement-secondary-muscle
                         ["movement_id = ? and muscle_id = ?" mov-id 8]) ; upper chest
              (j/delete! db-spec
                         rddl/tbl-movement-secondary-muscle
                         ["movement_id = ? and muscle_id = ?" mov-id 9]) ; lower chest
              (touch-mov mov-id))]
      ; Delete erroneous 'upper/lower chest' secondary muscles from bench press
      ; movement and other chest movements.  They were added as primary muscles
      ; way back in v12 updates, but I forgot to remove them from the secondary table.
      (fix-chest-mov 0) ; bench press
      (fix-chest-mov 6) ; push-up
      (fix-chest-mov 7) ; one-arm push-up
      (ins-movements db-spec v18-movements)))) ; some new movements

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v19 Data Loads (was deployed to production on 08/28/2017)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v19-data-loads
  [db-spec]
  ;; updates, fixes, etc
  (let [updated-at-sql (c/to-timestamp (t/now))]
    (letfn [(ins-mov-alias [mov-id alias]
              (insert-mov-alias db-spec mov-id alias updated-at-sql))
            (ins-pri-second-m [tbl mov-id m-id]
              (j/insert! db-spec
                         tbl
                         {:updated_at updated-at-sql
                          :created_at updated-at-sql
                          :updated_count 0
                          :movement_id mov-id
                          :muscle_id m-id}))
            (ins-secondary-m [mov-id m-id]
              (ins-pri-second-m rddl/tbl-movement-secondary-muscle mov-id m-id))
            (touch-mov [id] (touch-movement db-spec id updated-at-sql))]
      (ins-mov-alias 33 "back squats") ; squats (auto incs...will be ID 48 in movement_alias table)
      (ins-mov-alias 33 "barbell full squats") ; will be ID 49
      (ins-secondary-m 33 (:id m-glutes))
      (ins-secondary-m 33 (:id m-hamstrings))
      (touch-mov 33)
      (ins-secondary-m 56 (:id m-glutes)) ; box squats
      (ins-secondary-m 56 (:id m-hamstrings))
      (touch-mov 56)
      (ins-secondary-m 58 (:id m-hamstrings)) ; hack squats
      (touch-mov 58)
      (ins-secondary-m 59 (:id m-glutes)) ; split squats
      (ins-secondary-m 59 (:id m-hamstrings))
      (touch-mov 59)
      (ins-secondary-m 60 (:id m-glutes)) ; overhead squats
      (ins-secondary-m 60 (:id m-hamstrings))
      (touch-mov 60)
      (ins-secondary-m 66 (:id m-glutes)) ; jump squats
      (ins-secondary-m 66 (:id m-hamstrings))
      (touch-mov 66)))
  ;; new movements
  (ins-movements db-spec v19-movements))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v20 Data Loads (was deployed to production on 09/23/2017)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v20-data-loads
  [db-spec]
  ;; new movements
  (ins-movements db-spec v20-movements)
  ;; updates, fixes, etc
  (let [updated-at-sql (c/to-timestamp (t/now))]
    (letfn [(ins-mov-alias [mov-id alias]
              (insert-mov-alias db-spec mov-id alias updated-at-sql))
            (touch-mov [id] (touch-movement db-spec id updated-at-sql))
            (update-var-mask [mov-id mask]
              (update-variants-mask db-spec mov-id mask updated-at-sql))]
      (ins-mov-alias 0 "chest press") ; bench press (will be ID 56 in movement_alias table)
      (touch-mov 0)
      ; add 'machine' variant to pulldowns movement (id = 9)
      (update-var-mask 9 (-> 0
                             (bit-or (:id variant-cable)
                                     (:id variant-machine))))
      (touch-mov 9)
      ; add 'dumbbell' variant to skull crushers movement (id = 30)
      (update-var-mask 30 (-> 0
                              (bit-or (:id variant-curl-bar)
                                      (:id variant-dumbbell))))
      (touch-mov 30)
      ; add 'machine' variant to various dip movements
      (update-var-mask 8 (-> 0 ; wide grip dips
                             (bit-or (:id variant-body)
                                     (:id variant-machine))))
      (touch-mov 8)
      (update-var-mask 27 (-> 0 ; dips
                              (bit-or (:id variant-body)
                                      (:id variant-machine))))
      (touch-mov 27))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v21 Data Loads (was deployed to production on 10/07/2017)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v21-data-loads
  [db-spec]
  ;; new movements
  (ins-movements db-spec v21-movements)
  ;; updates, fixes, etc
  (let [updated-at-sql (c/to-timestamp (t/now))]
    (j/update! db-spec
               rddl/tbl-movement
               {:updated_at updated-at-sql :canonical_name "calf raises"}
               ["id = ?" 36])
    (j/update! db-spec
               rddl/tbl-muscle-group
               {:updated_at updated-at-sql :name "calfs"}
               ["id = ?" 7])
    (j/update! db-spec
               rddl/tbl-muscle
               {:updated_at updated-at-sql :canonical_name "calfs"}
               ["id = ?" 16])
    (letfn [(ins-mov-alias [mov-id alias]
              (insert-mov-alias db-spec mov-id alias updated-at-sql))
            (touch-mov [id] (touch-movement db-spec id updated-at-sql))]
      (ins-mov-alias 37 "bicep curls") ; new alias for 'curl' movement (mov ID = 37); highest movement alias ID is now 57 (auto-incs)
      (touch-mov 37)
      (ins-mov-alias 62 "squat clean") ; new alias for 'clean' movement
      (touch-mov 62)
      (ins-mov-alias 70 "RDL") ; new alias for 'Romanian deadlift' movement
      (touch-mov 70)
      (ins-mov-alias 24 "rear lateral raises") ; new alias for 'bent-over laterals' movement
      (touch-mov 24))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v22 Data Loads (was deployed to production on 04/08/2018)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn v22-data-loads
  [db-spec]
  ;; new movements
  (ins-movements db-spec v22-movements))
