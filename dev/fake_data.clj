(ns fake-data
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer (pprint)]
            [clojure.repl :refer :all]
            [clojure.java.jdbc :as j]
            [clojure.stacktrace :refer (e)]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [pe-core-utils.core :as ucore]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.dao :as dao]
            [riker-app.utils :as utils]
            [riker-app.app.config :as config]
            [riker-app.core.data-loading :as dl]
            [riker-app.core.ref-data :as rd]))

(def monday 1)
(def tuesday 2)
(def wednesday 3)
(def thursday 4)
(def friday 5)
(def saturday 6)
(def sunday 7)

(def avg-male-body-weight 200)

(defn variant-weight-config
  [barbell-value body-weight]
  {(:id rd/variant-barbell) barbell-value ; lbs
   (:id rd/variant-curl-bar) (+ barbell-value 25)
   (:id rd/variant-dumbbell) (- barbell-value 20)
   (:id rd/variant-smith-machine) (+ barbell-value 20)
   (:id rd/variant-machine) (+ barbell-value 35)
   (:id rd/variant-cable) (+ barbell-value 20)
   (:id rd/variant-sled) (+ barbell-value 100)
   (:id rd/variant-kettlebell) (- barbell-value 25)
   (:id rd/variant-body) body-weight})

(defn variant-rest-config
  [barbell-value]
  {(:id rd/variant-barbell) barbell-value ; lbs
   (:id rd/variant-curl-bar) (- barbell-value 5)
   (:id rd/variant-dumbbell) (- barbell-value 5)
   (:id rd/variant-smith-machine) (- barbell-value 10)
   (:id rd/variant-machine) (- barbell-value 15)
   (:id rd/variant-cable) (- barbell-value 15)
   (:id rd/variant-sled) (- barbell-value 5)
   (:id rd/variant-kettlebell) (- barbell-value 0)
   (:id rd/variant-body) (- barbell-value 0)})

(def avg-male-fake-data-config
  {:starting-weight {(:id rd/mg-shoulders)  (variant-weight-config 45 avg-male-body-weight)
                     (:id rd/mg-chest)      (variant-weight-config 65 avg-male-body-weight)
                     (:id rd/mg-back)       (variant-weight-config 65 avg-male-body-weight)
                     (:id rd/mg-quadriceps) (variant-weight-config 95 avg-male-body-weight)
                     (:id rd/mg-hamstrings) (variant-weight-config 45 avg-male-body-weight)
                     (:id rd/mg-glutes)     (variant-weight-config 45 avg-male-body-weight)
                     (:id rd/mg-calfs)      (variant-weight-config 95 avg-male-body-weight)
                     (:id rd/mg-abs)        (variant-weight-config 25 avg-male-body-weight)
                     (:id rd/mg-triceps)    (variant-weight-config 25 avg-male-body-weight)
                     (:id rd/mg-biceps)     (variant-weight-config 25 avg-male-body-weight)
                     (:id rd/mg-forearms)   (variant-weight-config 15 avg-male-body-weight)}
   :starting-rest {(:id rd/mg-shoulders)  (variant-rest-config 180)
                   (:id rd/mg-chest)      (variant-rest-config 200)
                   (:id rd/mg-back)       (variant-rest-config 200)
                   (:id rd/mg-quadriceps) (variant-rest-config 200)
                   (:id rd/mg-hamstrings) (variant-rest-config 180)
                   (:id rd/mg-glutes)     (variant-rest-config 180)
                   (:id rd/mg-calfs)      (variant-rest-config 130)
                   (:id rd/mg-abs)        (variant-rest-config 150)
                   (:id rd/mg-triceps)    (variant-rest-config 130)
                   (:id rd/mg-biceps)     (variant-rest-config 130)
                   (:id rd/mg-forearms)   (variant-rest-config 120)}
   :starting-body-weight avg-male-body-weight
   :starting-arm-size 14.0
   :starting-calf-size 20.0
   :starting-chest-size 50.0
   :starting-thigh-size 26.0
   :starting-waist-size 34.0
   :starting-forearm-size 10.0
   :starting-neck-size 15.0
   :starting-num-movements-per-day 5
   :weight-multipliers {"deadlift" 1.25}
   :rep-multipliers {"deadlift" 0.75}
   :training-schedule {monday [(:id rd/mg-shoulders) (:id rd/mg-triceps)]
                       tuesday [(:id rd/mg-quadriceps) (:id rd/mg-calfs) (:id rd/mg-hamstrings) (:id rd/mg-glutes)]
                       wednesday [] ; rest day (along with saturday and sunday)
                       thursday [(:id rd/mg-back) (:id rd/mg-biceps)]
                       friday [(:id rd/mg-chest) (:id rd/mg-forearms) (:id rd/mg-abs)]}})

(defn movements-by-mg-id
  [mg-id]
  (j/query config/db-spec
           ["select distinct mov.id, mov.variant_mask, mov.canonical_name from movement mov, movement_primary_muscle mpm, muscle m, muscle_group mg where m.muscle_group_id = mg.id and mpm.muscle_id = m.id and mov.id = mpm.movement_id and mg.id = ?"
            mg-id]))

(defn variants-for-mask
  [variant-mask]
  (letfn [(do-concat [variants variant]
            (concat variants (when (> (bit-and variant-mask (:id variant)) 0)
                               [variant])))]
    (-> []
        (do-concat rd/variant-barbell)
        (do-concat rd/variant-dumbbell)
        (do-concat rd/variant-cable)
        (do-concat rd/variant-machine)
        (do-concat rd/variant-smith-machine)
        (do-concat rd/variant-curl-bar)
        (do-concat rd/variant-sled)
        (do-concat rd/variant-kettlebell)
        (do-concat rd/variant-body))))

(def ^:dynamic *base-logged-at* nil)
(def ^:dynamic *body-weight* nil)
(def ^:dynamic *arm-size* nil)
(def ^:dynamic *forearm-size* nil)
(def ^:dynamic *neck-size* nil)
(def ^:dynamic *chest-size* nil)
(def ^:dynamic *thigh-size* nil)
(def ^:dynamic *waist-size* nil)
(def ^:dynamic *calf-size* nil)

(defn create-fake-user
  [email password num-days-of-history fd-config]
  (let [db config/db-spec
        now (t/now)
        start-date (t/plus now (t/days (* num-days-of-history -1)))
        num-weeks (quot num-days-of-history 7)
        days-leftover (rem num-days-of-history 7)
        user-id (usercore/next-user-account-id db)
        starting-num-movements (:starting-num-movements-per-day fd-config)
        num-movements-per-day-increment (double (/ (- (* 2 starting-num-movements) starting-num-movements)
                                                   num-days-of-history))]
    (let [[user-id user] (usercore/save-new-user db
                                                 user-id
                                                 {:user/email email
                                                  :user/password password
                                                  :user/is-payment-past-due false})]
      (alter-var-root (var *body-weight*) (fn [_] (:starting-body-weight fd-config)))
      (alter-var-root (var *arm-size*) (fn [_] (:starting-arm-size fd-config)))
      (alter-var-root (var *forearm-size*) (fn [_] (:starting-forearm-size fd-config)))
      (alter-var-root (var *neck-size*) (fn [_] (:starting-neck-size fd-config)))
      (alter-var-root (var *chest-size*) (fn [_] (:starting-chest-size fd-config)))
      (alter-var-root (var *thigh-size*) (fn [_] (:starting-thigh-size fd-config)))
      (alter-var-root (var *waist-size*) (fn [_] (:starting-waist-size fd-config)))
      (alter-var-root (var *calf-size*) (fn [_] (:starting-calf-size fd-config)))
      (dotimes [d num-days-of-history]
        (let [date (t/plus now (t/days (* (- num-days-of-history d) -1)))
              num-movements-to-work (Math/round (+ (:starting-num-movements-per-day fd-config)
                                                   (* d num-movements-per-day-increment)))
              day-of-week (t/day-of-week date)
              mg-ids (get (:training-schedule fd-config) day-of-week)
              orig-device-id (rand-int 7)
              orig-device-id (if (= orig-device-id 2) 3 orig-device-id) ; check for Pebble
              orig-device-id (if (= orig-device-id 0) 1 orig-device-id)
              arm-size (+ *arm-size* (* 0.05 (rand-int 10)))
              forearm-size (+ *forearm-size* (* 0.01 (rand-int 10)))
              chest-size (+ *chest-size* (* 0.1 (rand-int 10)))
              thigh-size (+ *thigh-size* (* 0.1 (rand-int 10)))
              waist-size (- *waist-size* (* 0.05 (rand-int 10)))
              neck-size (+ *neck-size* (* 0.01 (rand-int 10)))
              calf-size (+ *calf-size* (* 0.025 (rand-int 10)))
              body-weight(+ *body-weight* (* 0.1 (rand-int 10)))]
          (alter-var-root (var *base-logged-at*) (fn[_] date))
          (alter-var-root (var *body-weight*) (fn [_] body-weight))
          (alter-var-root (var *arm-size*) (fn [_] arm-size))
          (alter-var-root (var *forearm-size*) (fn [_] forearm-size))
          (alter-var-root (var *neck-size*) (fn [_] neck-size))
          (alter-var-root (var *chest-size*) (fn [_] chest-size))
          (alter-var-root (var *thigh-size*) (fn [_] thigh-size))
          (alter-var-root (var *waist-size*) (fn [_] waist-size))
          (alter-var-root (var *calf-size*) (fn [_] calf-size))
          (dao/save-new-body-journal-log db
                                         user-id
                                         (dao/next-body-journal-log-id db)
                                         {:bodyjournallog/arm-size *arm-size*
                                          :bodyjournallog/forearm-size *forearm-size*
                                          :bodyjournallog/chest-size *chest-size*
                                          :bodyjournallog/neck-size *neck-size*
                                          :bodyjournallog/waist-size *waist-size*
                                          :bodyjournallog/thigh-size *thigh-size*
                                          :bodyjournallog/calf-size *calf-size*
                                          :bodyjournallog/body-weight *body-weight*
                                          :bodyjournallog/body-weight-uom 0
                                          :bodyjournallog/size-uom 0
                                          :bodyjournallog/origination-device-id orig-device-id
                                          :bodyjournallog/logged-at date})
          (when mg-ids
            (let [num-mg-ids (count mg-ids)
                  num-movements-per-mg (if (> num-mg-ids 0) (quot num-movements-to-work num-mg-ids) 0)]
              (dotimes [mg-idx num-mg-ids]
                (let [mg-id (nth mg-ids mg-idx)
                      starting-weight-config (-> fd-config (get :starting-weight) (get mg-id))
                      starting-rest-config (-> fd-config (get :starting-rest) (get mg-id))
                      movements (movements-by-mg-id mg-id)
                      num-movements (count movements)]
                  (dotimes [m num-movements-per-mg]
                    (let [rand-mov-idx (rand-int num-movements)
                          num-sets (+ (rand-int 3) 3)
                          mov (nth movements rand-mov-idx)
                          mov-id (:id mov)
                          mov-variant-mask (:variant_mask mov)
                          mov-name (:canonical_name mov)
                          variants (variants-for-mask mov-variant-mask)
                          num-variants (count variants)
                          rand-variant-idx (rand-int num-variants)
                          variant (if (> num-variants 0) (nth variants rand-variant-idx) rd/variant-body)
                          variant-id (:id variant)
                          starting-rest (get starting-rest-config variant-id)
                          ending-rest (quot starting-rest 3.5)
                          rest-decrement (double (/ (- starting-rest ending-rest) num-days-of-history))
                          starting-weight (get starting-weight-config variant-id)
                          ending-weight (* starting-weight 4)
                          weight-increment (double (/ (- ending-weight starting-weight)
                                                      num-days-of-history))
                          variant-id (when (> num-variants 0) variant-id)
                          reps 10]
                      (dotimes [s num-sets]
                        (let [rand-duration-of-set (+ (rand-int 5) 25)
                              set-logged-at (t/plus *base-logged-at* (t/seconds (+ rand-duration-of-set
                                                                                   (if (> s 0) (- starting-rest (* d rest-decrement)) 0))))
                              reps (- reps (rand-int s))
                              weight (+ starting-weight (Math/round (* weight-increment d)))
                              new-set-id (dao/next-set-id db)]
                          (dao/save-new-set db
                                            user-id
                                            new-set-id
                                            {:set/logged-at set-logged-at
                                             :set/movement-id mov-id
                                             :set/movement-variant-id variant-id
                                             :set/ignore-time false
                                             :set/to-failure (= (rand-int 2) 0)
                                             :set/negatives false
                                             :set/weight weight
                                             :set/num-reps reps
                                             :set/weight-uom 0
                                             :set/origination-device-id orig-device-id})
                          (alter-var-root (var *base-logged-at*) (fn[_] set-logged-at)))))))))))))))
