(ns riker-app.core.ref-data
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.ddl :as rddl]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; START Version 0 (v0) Definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v0 Walking paces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def walking-pace-leisure {:id 0 :name "leisurely"      :pace_mph 3.0})
(def walking-pace-brisk   {:id 1 :name "brisk"          :pace_mph 4.0})
(def walking-pace-speed   {:id 2 :name "high intensity" :pace_mph 6.0})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v0 Cardio type attributes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def cardio-type-attr-distance (bit-shift-left 1 0)) ; has distance component to it

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v0 Cardio types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def cardio-type-other          {:id 0  :name "other" :attributes_mask 0})
(def cardio-type-dancing        {:id 1  :name "dancing" :attributes_mask 0})
(def cardio-type-elliptical     {:id 2  :name "elliptical" :attributes_mask 0})
(def cardio-type-group-exercise {:id 3  :name "group excercise" :attributes_mask 0})
(def cardio-type-jacobs-ladder  {:id 4  :name "Jacob's ladder" :attributes_mask 0})
(def cardio-type-jumping-rope   {:id 5  :name "jumping rope" :attributes_mask 0})
(def cardio-type-rowing         {:id 6
                                 :name "rowing"
                                 :attributes_mask (-> 0
                                                      (bit-or cardio-type-attr-distance))})
(def cardio-type-running        {:id 7
                                 :name "running"
                                 :attributes_mask (-> 0
                                                      (bit-or cardio-type-attr-distance))})
(def cardio-type-boxing         {:id 8 :name "boxing" :attributes_mask 0})
(def cardio-type-spinning       {:id 9
                                 :name "spinning"
                                 :attributes_mask (-> 0
                                                      (bit-or cardio-type-attr-distance))})
(def cardio-type-sports         {:id 10 :name "sports activity" :attributes_mask 0})
(def cardio-type-stair-climber  {:id 11 :name "stair climber" :attributes_mask 0})
(def cardio-type-swimming       {:id 12
                                 :name "swimming"
                                 :attributes_mask (-> 0
                                                      (bit-or cardio-type-attr-distance))})
(def cardio-type-cycling        {:id 13
                                 :name "cycling"
                                 :attributes_mask (-> 0
                                                      (bit-or cardio-type-attr-distance))})
(def cardio-type-walking        {:id 14
                                 :name "walking"
                                 :attributes_mask (-> 0
                                                      (bit-or cardio-type-attr-distance))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Constants
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def push-up-percent-of-body-weight 0.64)
(def dip-percent-of-body-weight 0.95)
(def pull-up-percent-of-body-weight 1.0)
(def sit-up-percent-of-body-weight 0.5)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v0 Body Segment Identifiers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def bs-upper-body {:id 0 :name "upper body"})
(def bs-lower-body {:id 1 :name "lower body"})
(def bs-core       {:id 2 :name "core"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v0 Muscles Groups
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def mg-shoulders  {:id 0  :name "shoulders"  :body_segment_id (:id bs-upper-body)})
(def mg-back       {:id 1  :name "back"       :body_segment_id (:id bs-upper-body)})
(def mg-chest      {:id 2  :name "chest"      :body_segment_id (:id bs-upper-body)})
(def mg-abs        {:id 3  :name "abs"        :body_segment_id (:id bs-core)})
(def mg-serratus   {:id 4  :name "serratus"   :body_segment_id (:id bs-core)})
(def mg-quadriceps {:id 5  :name "quadriceps" :body_segment_id (:id bs-lower-body)})
(def mg-hamstrings {:id 6  :name "hamstrings" :body_segment_id (:id bs-lower-body)})
(def mg-calfs     {:id 7  :name "calves"     :body_segment_id (:id bs-lower-body)})
(def mg-triceps    {:id 8  :name "triceps"    :body_segment_id (:id bs-upper-body)})
(def mg-biceps     {:id 9  :name "biceps"     :body_segment_id (:id bs-upper-body)})
(def mg-forearms   {:id 10 :name "forearms"   :body_segment_id (:id bs-upper-body)})
(def mg-glutes     {:id 11 :name "glutes"     :body_segment_id (:id bs-lower-body)})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v0 Muscles
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *v0-muscle-id* -1)

(def m-deltoids       {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "deltoids"
                       :muscle_group_id (:id mg-shoulders)
                       :aliases [{:id 0 :alias "delts"}
                                 {:id 1 :alias "shoulders"}]})
(def m-deltoids-rear  {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "rear deltoids"
                       :muscle_group_id (:id mg-shoulders)
                       :aliases [{:id 0 :alias "rear delts"}
                                 {:id 1 :alias "posterior deltoids"}]})
(def m-deltoids-front {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "front deltoids"
                       :muscle_group_id (:id mg-shoulders)
                       :aliases [{:id 0 :alias "front delts"}
                                 {:id 1 :alias "anterior deltoids"}]})
(def m-deltoids-side  {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "side deltoids"
                       :muscle_group_id (:id mg-shoulders)
                       :aliases [{:id 0 :alias "side delts"}
                                 {:id 1 :alias "middle delts"}
                                 {:id 2 :alias "outer delts"}
                                 {:id 3 :alias "lateral deltoids"}]})
(def m-back           {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "back"
                       :muscle_group_id (:id mg-back)
                       :aliases [{:id 0 :alias "lats"}
                                 {:id 1 :alias "latissimus dorsi"}]})
(def m-back-upper     {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "upper back"
                       :muscle_group_id (:id mg-back)
                       :aliases [{:id 0 :alias "upper lats"}]})
(def m-back-lower     {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "lower back"
                       :muscle_group_id (:id mg-back)
                       :aliases [{:id 0 :alias "lower lats"}]})
(def m-chest          {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "chest"
                       :muscle_group_id (:id mg-chest)
                       :aliases [{:id 0 :alias "pecs"}
                                 {:id 1 :alias "pectorals"}]})
(def m-chest-upper    {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "upper chest"
                       :muscle_group_id (:id mg-chest)
                       :aliases [{:id 0 :alias "upper pecs"}]})
(def m-chest-lower    {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "lower chest"
                       :muscle_group_id (:id mg-chest)
                       :aliases [{:id 0 :alias "lower pecs"}]})
(def m-abs            {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "abs"
                       :muscle_group_id (:id mg-abs)
                       :aliases [{:id 0 :alias "abdominals"}]})
(def m-abs-upper      {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "upper abs"
                       :muscle_group_id (:id mg-abs)
                       :aliases [{:id 0 :alias "upper abdominals"}]})
(def m-abs-lower      {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "lower abs"
                       :muscle_group_id (:id mg-abs)
                       :aliases [{:id 0 :alias "lower abdominals"}]})
(def m-serratus       {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "serratus"
                       :muscle_group_id (:id mg-serratus)
                       :aliases []})
(def m-quadriceps     {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "quadriceps"
                       :muscle_group_id (:id mg-quadriceps)
                       :aliases [{:id 0 :alias "quads"}]})
(def m-hamstrings     {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "hamstrings"
                       :muscle_group_id (:id mg-hamstrings)
                       :aliases [{:id 0 :alias "hams"}]})
(def m-calfs          {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "calves"
                       :muscle_group_id (:id mg-calfs)
                       :aliases []})
(def m-triceps        {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "triceps"
                       :muscle_group_id (:id mg-triceps)
                       :aliases []})
(def m-biceps         {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "biceps"
                       :muscle_group_id (:id mg-biceps)
                       :aliases []})
(def m-forearms       {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "forearms"
                       :muscle_group_id (:id mg-forearms)
                       :aliases []})
(def m-traps          {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "traps"
                       :muscle_group_id (:id mg-shoulders)
                       :aliases [{:id 0 :alias "trapezius"}]})
(def m-glutes         {:id (alter-var-root (var *v0-muscle-id*) inc)
                       :canonical_name "glutes"
                       :muscle_group_id (:id mg-glutes)
                       :aliases [{:id 0 :alias "butt"}
                                 {:id 1 :alias "buttocks"}
                                 {:id 2 :alias "gluteus maximus"}]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Movement variants
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def variant-barbell       {:id (bit-shift-left 1 0) :name "barbell"})
(def variant-dumbbell      {:id (bit-shift-left 1 1) :name "dumbbell"})
(def variant-machine       {:id (bit-shift-left 1 2) :name "machine"})
(def variant-smith-machine {:id (bit-shift-left 1 3) :name "smith machine"})
(def variant-cable         {:id (bit-shift-left 1 4) :name "cable"})
(def variant-curl-bar      {:id (bit-shift-left 1 5) :name "curl bar"})
(def variant-sled          {:id (bit-shift-left 1 6) :name "sled"})
(def variant-body          {:id (bit-shift-left 1 7)
                            :name "body"
                            :description "No equipment or machine is used, just the weight of your body."})
; fyi, the above variants get their sort_order populated in v8-data-loads
(def variant-kettlebell    {:id (bit-shift-left 1 8)
                            :name "kettlebell"
                            :sort_order 8})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Movement helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn assoc-all-variants
  [movement]
  (assoc movement
         :variant_mask
         (-> 0
             (bit-or (:id variant-barbell)
                     (:id variant-dumbbell)
                     (:id variant-machine)
                     (:id variant-smith-machine)
                     (:id variant-cable)))))

(defn assoc-body
  [movement percentage]
  (-> movement
      (assoc :is_body_lift true)
      (assoc :percentage_of_body_weight percentage)
      (assoc :variant_mask 0)))

(defn assoc-bench-press-muscles ; deprecated
  [movement]
  (-> movement
      (assoc :primary-muscles [{:muscle_id (:id m-chest)}])
      (assoc :secondary-muscles [{:muscle_id (:id m-chest-upper)}
                                 {:muscle_id (:id m-chest-lower)}
                                 {:muscle_id (:id m-deltoids-front)}
                                 {:muscle_id (:id m-serratus)}
                                 {:muscle_id (:id m-traps)}
                                 {:muscle_id (:id m-triceps)}])))

(defn assoc-shoulder-press-muscles ; deprecated
  [movement]
  (-> movement
      (assoc :primary-muscles [{:muscle_id (:id m-deltoids-front)}
                               {:muscle_id (:id m-deltoids-side)}])
      (assoc :secondary-muscles [{:muscle_id (:id m-triceps)}
                                 {:muscle_id (:id m-traps)}])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Movement IDs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *v0-movement-id* -1)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sort Order IDs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *chest-sort-order* -1)
(def ^:dynamic *back-sort-order* -1)
(def ^:dynamic *shoulder-sort-order* -1)
(def ^:dynamic *traps-sort-order* -1)
(def ^:dynamic *biceps-sort-order* -1)
(def ^:dynamic *triceps-sort-order* -1)
(def ^:dynamic *forearms-sort-order* -1)
(def ^:dynamic *calfs-sort-order* -1)
(def ^:dynamic *glutes-sort-order* -1)
(def ^:dynamic *hamstrings-sort-order* -1)
(def ^:dynamic *quadriceps-sort-order* -1)
(def ^:dynamic *abs-sort-order* -1)
(def ^:dynamic *serratus-sort-order* -1)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v0 Movements
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v0-movements
  [
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Chest movements
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "bench press"
        :sort_order (alter-var-root (var *chest-sort-order*) inc)
        :aliases []}
       (assoc :is_body_lift false)
       (assoc-all-variants)
       (assoc-bench-press-muscles))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "incline bench press"
        :sort_order (alter-var-root (var *chest-sort-order*) inc)
        :aliases []}
       (assoc :is_body_lift false)
       (assoc-all-variants)
       (assoc :primary-muscles [{:muscle_id (:id m-chest-upper)}
                                {:muscle_id (:id m-deltoids-front)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-chest)}
                                  {:muscle_id (:id m-serratus)}
                                  {:muscle_id (:id m-traps)}
                                  {:muscle_id (:id m-triceps)}]))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "decline bench press"
        :sort_order (alter-var-root (var *chest-sort-order*) inc)
        :aliases []}
       (assoc :is_body_lift false)
       (assoc-all-variants)
       (assoc :primary-muscles [{:muscle_id (:id m-chest-lower)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-chest)}
                                  {:muscle_id (:id m-deltoids-front)}
                                  {:muscle_id (:id m-serratus)}
                                  {:muscle_id (:id m-traps)}
                                  {:muscle_id (:id m-triceps)}]))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "flys"
        :sort_order (alter-var-root (var *chest-sort-order*) inc)
        :aliases [{:id 0 :alias "chest fly"}
                  {:id 1 :alias "pectoral fly"}
                  {:id 2 :alias "pec fly"}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell))
                                (bit-or (:id variant-cable))
                                (bit-or (:id variant-machine))))
       (assoc :primary-muscles [{:muscle_id (:id m-chest)}])
       (assoc :secondary-muscles []))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "incline flys"
        :sort_order (alter-var-root (var *chest-sort-order*) inc)
        :aliases [{:id 0 :alias "incline chest fly"}
                  {:id 1 :alias "incline pectoral fly"}
                  {:id 2 :alias "incline pec fly"}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell))
                                (bit-or (:id variant-cable))
                                (bit-or (:id variant-machine))))
       (assoc :primary-muscles [{:muscle_id (:id m-chest-upper)}])
       (assoc :secondary-muscles []))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "decline flys"
        :sort_order (alter-var-root (var *chest-sort-order*) inc)
        :aliases [{:id 0 :alias "decline chest fly"}
                  {:id 1 :alias "decline pectoral fly"}
                  {:id 2 :alias "decline pec fly"}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell))
                                (bit-or (:id variant-cable))
                                (bit-or (:id variant-machine))))
       (assoc :primary-muscles [{:muscle_id (:id m-chest-lower)}])
       (assoc :secondary-muscles []))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "push-up"
        :sort_order (alter-var-root (var *chest-sort-order*) inc)
        :aliases [{:id 0 :alias "press-up"}
                  {:id 1 :alias "floor dip"}]}
       (assoc-body push-up-percent-of-body-weight)
       (assoc-bench-press-muscles))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "one arm push-up"
        :sort_order (alter-var-root (var *chest-sort-order*) inc)
        :aliases []}
       (assoc-body push-up-percent-of-body-weight)
       (assoc-bench-press-muscles))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "wide grip dips"
        :sort_order (alter-var-root (var *chest-sort-order*) inc)
        :aliases [{:id 0 :alias "chest dips"}]
        :primary-muscles [{:muscle_id (:id m-chest)}]
        :secondary-muscles [{:muscle_id (:id m-deltoids-front)}
                            {:muscle_id (:id m-triceps)}]}
       (assoc-body dip-percent-of-body-weight))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Back movements
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "pulldowns"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases [{:id 0 :alias "lat pulldowns"}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-cable))))
       (assoc :primary-muscles [{:muscle_id (:id m-back)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-biceps)}]))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "rows"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-cable))
                                (bit-or (:id variant-machine))))
       (assoc :primary-muscles [{:muscle_id (:id m-back)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-biceps)}]))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "t-bar rows"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))))
       (assoc :primary-muscles [{:muscle_id (:id m-back)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-biceps)}]))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "bent-over rows"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases [{:id 0 :alias "barbell rows"}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-smith-machine))))
       (assoc :primary-muscles [{:muscle_id (:id m-back)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-biceps)}]))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "one arm bent-over rows"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell))))
       (assoc :primary-muscles [{:muscle_id (:id m-back)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-biceps)}]))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "good-mornings"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))))
       (assoc :primary-muscles [{:muscle_id (:id m-back-lower)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-hamstrings)}
                                  {:muscle_id (:id m-glutes)}]))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Shoulder movements
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "shoulder press"
        :sort_order (alter-var-root (var *shoulder-sort-order*) inc)
        :aliases [{:id 2 :alias "overhead press"}
                  {:id 3 :alias "press behind the neck"}]}
       (assoc :is_body_lift false)
       (assoc-all-variants)
       (assoc-shoulder-press-muscles))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "Arnold press"
        :sort_order (alter-var-root (var *shoulder-sort-order*) inc)
        :aliases []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell))))
       (assoc-shoulder-press-muscles))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "military press"
        :sort_order (alter-var-root (var *shoulder-sort-order*) inc)
        :aliases [{:id 0 :alias "front shoulder press"}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-smith-machine))))
       (assoc-shoulder-press-muscles))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "clean and press"
        :sort_order (alter-var-root (var *shoulder-sort-order*) inc)
        :aliases []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))))
       (assoc-shoulder-press-muscles))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "push press"
        :sort_order (alter-var-root (var *shoulder-sort-order*) inc)
        :aliases []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))))
       (assoc-shoulder-press-muscles))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "lateral raises"
        :sort_order (alter-var-root (var *shoulder-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-deltoids-side)}]
        :secondary-muscles [{:muscle_id (:id m-deltoids-rear)}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell))
                                (bit-or (:id variant-cable)))))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "front raises"
        :sort_order (alter-var-root (var *shoulder-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-deltoids-front)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell))
                                (bit-or (:id variant-cable)))))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "cross cable laterals"
        :sort_order (alter-var-root (var *shoulder-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-deltoids-rear)}
                          {:muscle_id (:id m-deltoids-side)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-cable)))))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "overhead laterals"
        :sort_order (alter-var-root (var *shoulder-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-deltoids-front)}]
        :secondary-muscles [{:muscle_id (:id m-traps)}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell)))))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "bent-over laterals"
        :sort_order (alter-var-root (var *shoulder-sort-order*) inc)
        :aliases [{:id 0 :alias "reverse fly"}
                  {:id 1 :alias "rear delt fly"}
                  {:id 2 :alias "inverted fly"}]
        :primary-muscles [{:muscle_id (:id m-deltoids-rear)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell))
                                (bit-or (:id variant-cable)))))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Traps movements
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "upright rows"
        :sort_order (alter-var-root (var *traps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-traps)}
                          {:muscle_id (:id m-deltoids-front)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-smith-machine)))))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "shrugs"
        :sort_order (alter-var-root (var *traps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-traps)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-dumbbell))
                                (bit-or (:id variant-smith-machine)))))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Triceps movements
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "dips"
        :sort_order (alter-var-root (var *triceps-sort-order*) inc)
        :aliases [{:id 0 :alias "shoulder width dips"}
                  {:id 1 :alias "tricep dips"}]
        :primary-muscles [{:muscle_id (:id m-triceps)}]
        :secondary-muscles [{:muscle_id (:id m-deltoids-front)}
                            {:muscle_id (:id m-chest)}]}
       (assoc-body dip-percent-of-body-weight))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "close-grip bench press"
        :sort_order (alter-var-root (var *triceps-sort-order*) inc)
        :is_body_lift false
        :aliases []
        :primary-muscles [{:muscle_id (:id m-triceps)}]
        :secondary-muscles [{:muscle_id (:id m-chest)}]}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-smith-machine)))))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "pushdowns"
        :sort_order (alter-var-root (var *triceps-sort-order*) inc)
        :is_body_lift false
        :aliases []
        :primary-muscles [{:muscle_id (:id m-triceps)}]
        :secondary-muscles []}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-cable)))))
   (-> {:id (alter-var-root (var *v0-movement-id*) inc)
        :canonical_name "tricep extensions"
        :is_body_lift false
        :sort_order (alter-var-root (var *triceps-sort-order*) inc)
        :aliases [{:id 0 :alias "skull crushers"}
                  {:id 1 :alias "french press"}
                  {:id 2 :alias "french extensions"}]
        :primary-muscles [{:muscle_id (:id m-triceps)}]
        :secondary-muscles []}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-curl-bar)))))
   ]
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v1 Movements
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *v1-movement-id* *v0-movement-id*)

(def v1-movements
  [
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Quadricep movements
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v1-movement-id*) inc)
        :canonical_name "leg press"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}
                          {:muscle_id (:id m-hamstrings)}
                          {:muscle_id (:id m-glutes)}]
        :secondary-muscles [{:muscle_id (:id m-calfs)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-sled)))))

   (-> {:id (alter-var-root (var *v1-movement-id*) inc)
        :canonical_name "deadlifts"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}
                          {:muscle_id (:id m-hamstrings)}
                          {:muscle_id (:id m-glutes)}
                          {:muscle_id (:id m-back)}]
        :secondary-muscles [{:muscle_id (:id m-forearms)}
                            {:muscle_id (:id m-abs)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   (-> {:id (alter-var-root (var *v1-movement-id*) inc)
        :canonical_name "squats"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles []
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-smith-machine)))))

   (-> {:id (alter-var-root (var *v1-movement-id*) inc)
        :canonical_name "leg extensions"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles []
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-machine)))))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Forearms movements
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v1-movement-id*) inc)
        :canonical_name "wrist curls"
        :sort_order (alter-var-root (var *forearms-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-forearms)}]
        :secondary-muscles []
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-curl-bar))
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-dumbbell))
                                (bit-or (:id variant-cable)))))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Calfs movements
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v1-movement-id*) inc)
        :canonical_name "calve raises"
        :sort_order (alter-var-root (var *calfs-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-calfs)}]
        :secondary-muscles []
        :is_body_lift false
        :percentage_of_body_weight 1.0}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-machine))
                                (bit-or (:id variant-body)))))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Biceps movements
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v1-movement-id*) inc)
        :canonical_name "curls"
        :sort_order (alter-var-root (var *biceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-biceps)}]
        :secondary-muscles []
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-curl-bar))
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-dumbbell))
                                (bit-or (:id variant-cable))
                                (bit-or (:id variant-machine)))))

   ]
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v2 Movements
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *v2-movement-id* *v1-movement-id*)

(def v2-movements
  [
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Core
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v2-movement-id*) inc)
        :canonical_name "sit ups"
        :sort_order (alter-var-root (var *abs-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-abs)}
                          {:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}]
        :secondary-muscles []}
       (assoc-body sit-up-percent-of-body-weight)
       (assoc :variant_mask 0))
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Back
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v2-movement-id*) inc)
        :canonical_name "pull-ups"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases [{:id 0 :alias "chins"}]}
       (assoc-body pull-up-percent-of-body-weight)
       (assoc :primary-muscles [{:muscle_id (:id m-back)}
                                {:muscle_id (:id m-back-upper)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-biceps)}]))
   (-> {:id (alter-var-root (var *v2-movement-id*) inc)
        :canonical_name "wide grip pull-ups"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases [{:id 0 :alias "wide grip chins"}]}
       (assoc-body pull-up-percent-of-body-weight)
       (assoc :primary-muscles [{:muscle_id (:id m-back)}
                                {:muscle_id (:id m-back-upper)}]))
   ]
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v3 Movements
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def m-tricep-lateral-head {:id 22})
(def m-tricep-long-head    {:id 23})
(def m-tricep-medial-head  {:id 24})

(def ^:dynamic *v3-movement-id* *v2-movement-id*)

(def v3-movements
  [
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Abs
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "ab roller"
        :sort_order (alter-var-root (var *abs-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}]
        :secondary-muscles [{:muscle_id (:id m-back-upper)}
                            {:muscle_id (:id m-back-lower)}
                            {:muscle_id (:id m-tricep-lateral-head)}
                            {:muscle_id (:id m-tricep-long-head)}
                            {:muscle_id (:id m-tricep-medial-head)}
                            {:muscle_id (:id m-traps)}]}
       (assoc-body 0.50))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "crunches"
        :sort_order (alter-var-root (var *abs-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}]
        :secondary-muscles []}
       (assoc-body 0.20))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "bicycle crunches"
        :sort_order (alter-var-root (var *abs-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}]
        :secondary-muscles []}
       (assoc-body 0.20))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "leg raises"
        :sort_order (alter-var-root (var *abs-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}]
        :secondary-muscles []}
       (assoc-body 0.40))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "Russian twists"
        :sort_order (alter-var-root (var *abs-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}]
        :secondary-muscles []}
       (assoc-body 0.30))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "pelvic lifts"
        :sort_order (alter-var-root (var *abs-sort-order*) inc)
        :aliases [{:id 0 :alias "pelvic tilts"}]
        :primary-muscles [{:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}
                          {:muscle_id (:id m-back-lower)}
                          {:muscle_id (:id m-glutes)}]
        :secondary-muscles []}
       (assoc-body 0.20))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "cable crunches"
        :sort_order (alter-var-root (var *abs-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}]
        :secondary-muscles []
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-cable)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "jackknifes"
        :sort_order (alter-var-root (var *abs-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}]
        :secondary-muscles []}
       (assoc-body 0.40))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "knee raises"
        :sort_order (alter-var-root (var *abs-sort-order*) inc)
        :aliases [{:id 0 :alias "hip raises"}]
        :primary-muscles [{:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}]
        :secondary-muscles []}
       (assoc-body 0.40))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "V-ups"
        :sort_order (alter-var-root (var *abs-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}]
        :secondary-muscles []}
       (assoc-body 0.75))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "woodchoppers"
        :sort_order (alter-var-root (var *abs-sort-order*) inc)
        :aliases [{:id 0 :alias "standing cable wood chop"}]
        :primary-muscles [{:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-cable)))))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Glutes
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "dirty dog"
        :sort_order (alter-var-root (var *glutes-sort-order*) inc)
        :aliases [{:id 0 :alias "hip side lifts"}
                  {:id 1 :alias "fire hydrant exercise"}]
        :primary-muscles [{:muscle_id (:id m-glutes)}]
        :secondary-muscles []}
       (assoc-body 0.2))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "hip thrust"
        :sort_order (alter-var-root (var *glutes-sort-order*) inc)
        :aliases [{:id 0 :alias "bridge"}
                  {:id 1 :alias "weighted hip extension"}]
        :primary-muscles [{:muscle_id (:id m-glutes)}]
        :secondary-muscles [{:muscle_id (:id m-quadriceps)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "kettlebell swing"
        :sort_order (alter-var-root (var *glutes-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-glutes)}
                          {:muscle_id (:id m-hamstrings)}
                          {:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}
                          {:muscle_id (:id m-deltoids-front)}
                          {:muscle_id (:id m-deltoids-side)}
                          {:muscle_id (:id m-deltoids-rear)}]
        :secondary-muscles [{:muscle_id (:id m-quadriceps)}
                            {:muscle_id (:id m-chest-upper)}
                            {:muscle_id (:id m-chest-lower)}
                            {:muscle_id (:id m-forearms)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-kettlebell)))))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Quads
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "lunges"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}
                          {:muscle_id (:id m-glutes)}]
        :secondary-muscles [{:muscle_id (:id m-hamstrings)}]
        :percentage_of_body_weight 0.75
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-body))
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-dumbbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "box squats"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles []
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-smith-machine)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "front squats"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles []
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-smith-machine)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "hack squats"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles [{:muscle_id (:id m-glutes)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-sled)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "split squats"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases [{:id 0 :alias "bulgarian split squats"}]
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles []
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "overhead squats"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles [{:muscle_id (:id m-back-upper)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "press unders"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles [{:muscle_id (:id m-back-upper)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "clean"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles [{:muscle_id (:id m-back-upper)}
                            {:muscle_id (:id m-back-lower)}
                            {:muscle_id (:id m-deltoids-front)}
                            {:muscle_id (:id m-deltoids-rear)}
                            {:muscle_id (:id m-deltoids-side)}
                            {:muscle_id (:id m-calfs)}
                            {:muscle_id (:id m-glutes)}
                            {:muscle_id (:id m-hamstrings)}
                            {:muscle_id (:id m-traps)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "clean and jerk"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles [{:muscle_id (:id m-tricep-lateral-head)}
                            {:muscle_id (:id m-tricep-long-head)}
                            {:muscle_id (:id m-tricep-medial-head)}
                            {:muscle_id (:id m-abs-upper)}
                            {:muscle_id (:id m-abs-lower)}
                            {:muscle_id (:id m-forearms)}
                            {:muscle_id (:id m-back-upper)}
                            {:muscle_id (:id m-back-lower)}
                            {:muscle_id (:id m-deltoids-front)}
                            {:muscle_id (:id m-deltoids-rear)}
                            {:muscle_id (:id m-deltoids-side)}
                            {:muscle_id (:id m-calfs)}
                            {:muscle_id (:id m-glutes)}
                            {:muscle_id (:id m-hamstrings)}
                            {:muscle_id (:id m-traps)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "split jerk"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles [{:muscle_id (:id m-tricep-lateral-head)}
                            {:muscle_id (:id m-tricep-long-head)}
                            {:muscle_id (:id m-tricep-medial-head)}
                            {:muscle_id (:id m-abs-upper)}
                            {:muscle_id (:id m-abs-lower)}
                            {:muscle_id (:id m-forearms)}
                            {:muscle_id (:id m-back-upper)}
                            {:muscle_id (:id m-back-lower)}
                            {:muscle_id (:id m-deltoids-front)}
                            {:muscle_id (:id m-deltoids-rear)}
                            {:muscle_id (:id m-deltoids-side)}
                            {:muscle_id (:id m-calfs)}
                            {:muscle_id (:id m-glutes)}
                            {:muscle_id (:id m-hamstrings)}
                            {:muscle_id (:id m-traps)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "hang clean"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles [{:muscle_id (:id m-back-upper)}
                            {:muscle_id (:id m-back-lower)}
                            {:muscle_id (:id m-deltoids-front)}
                            {:muscle_id (:id m-deltoids-rear)}
                            {:muscle_id (:id m-deltoids-side)}
                            {:muscle_id (:id m-calfs)}
                            {:muscle_id (:id m-glutes)}
                            {:muscle_id (:id m-hamstrings)}
                            {:muscle_id (:id m-traps)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "jump squat"
        :sort_order (alter-var-root (var *quadriceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles [{:muscle_id (:id m-calfs)}]}
       (assoc-body 0.50))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Hamstrings
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "leg curl"
        :sort_order (alter-var-root (var *hamstrings-sort-order*) inc)
        :aliases [{:id 0 :alias "hamstring curl"}]
        :primary-muscles [{:muscle_id (:id m-hamstrings)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-machine)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "hang snatch"
        :sort_order (alter-var-root (var *hamstrings-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-hamstrings)}]
        :secondary-muscles [{:muscle_id (:id m-quadriceps)}
                            {:muscle_id (:id m-back-upper)}
                            {:muscle_id (:id m-back-lower)}
                            {:muscle_id (:id m-deltoids-front)}
                            {:muscle_id (:id m-deltoids-rear)}
                            {:muscle_id (:id m-deltoids-side)}
                            {:muscle_id (:id m-calfs)}
                            {:muscle_id (:id m-glutes)}
                            {:muscle_id (:id m-traps)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "stiff legged deadlift"
        :sort_order (alter-var-root (var *hamstrings-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-hamstrings)}
                          {:muscle_id (:id m-glutes)}]
        :secondary-muscles []
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "Romanian deadlift"
        :sort_order (alter-var-root (var *hamstrings-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-hamstrings)}
                          {:muscle_id (:id m-glutes)}]
        :secondary-muscles []
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "sumo deadlift"
        :sort_order (alter-var-root (var *hamstrings-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-hamstrings)}]
        :secondary-muscles [{:muscle_id (:id m-glutes)}
                            {:muscle_id (:id m-quadriceps)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Back
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "back extension"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases [{:id 0 :alias "hyperextension"}]
        :primary-muscles [{:muscle_id (:id m-back-lower)}]
        :secondary-muscles [{:muscle_id (:id m-back-upper)}]
        :is_body_lift false
        :percentage_of_body_weight 0.5}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-machine))
                                (bit-or (:id variant-body)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "chin-up"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases [{:id 0 :alias "chin"}]
        :primary-muscles [{:muscle_id (:id m-back-upper)}
                          {:muscle_id (:id m-biceps)}]
        :secondary-muscles [{:muscle_id (:id m-back-lower)}]}
       (assoc-body 1.0))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "muscle-up"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-back-upper)}
                          {:muscle_id (:id m-biceps)}
                          {:muscle_id (:id m-tricep-lateral-head)}
                          {:muscle_id (:id m-tricep-long-head)}
                          {:muscle_id (:id m-tricep-medial-head)}]
        :secondary-muscles [{:muscle_id (:id m-back-lower)}]}
       (assoc-body 1.0))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "inverted row"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases [{:id 0 :alias "supine row"}]
        :primary-muscles [{:muscle_id (:id m-back-upper)}
                          {:muscle_id (:id m-traps)}]
        :secondary-muscles [{:muscle_id (:id m-biceps)}]}
       (assoc-body 0.8))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "jump shrug"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-back-upper)}
                          {:muscle_id (:id m-quadriceps)}
                          {:muscle_id (:id m-traps)}]
        :secondary-muscles [{:muscle_id (:id m-back-lower)}
                            {:muscle_id (:id m-calfs)}
                            {:muscle_id (:id m-glutes)}
                            {:muscle_id (:id m-hamstrings)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "kipping pull-ups"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases [{:id 0 :alias "pull-up with kip"}]}
       (assoc-body pull-up-percent-of-body-weight)
       (assoc :primary-muscles [{:muscle_id (:id m-back-lower)}
                                {:muscle_id (:id m-back-upper)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-biceps)}]))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "Pendlay rows"
        :sort_order (alter-var-root (var *back-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-back-lower)}
                          {:muscle_id (:id m-back-upper)}]
        :secondary-muscles []
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Forearms
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "hammer curls"
        :sort_order (alter-var-root (var *forearms-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-forearms)}]
        :secondary-muscles [{:muscle_id (:id m-biceps)}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "reverse barbell curls"
        :sort_order (alter-var-root (var *forearms-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-forearms)}]
        :secondary-muscles [{:muscle_id (:id m-biceps)}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Biceps
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "concentration curls"
        :sort_order (alter-var-root (var *biceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-biceps)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "preacher curls"
        :sort_order (alter-var-root (var *biceps-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-biceps)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell))
                                (bit-or (:id variant-curl-bar))
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-cable))
                                (bit-or (:id variant-machine)))))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Chest
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "around the worlds"
        :sort_order (alter-var-root (var *chest-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-chest-upper)}
                          {:muscle_id (:id m-chest-lower)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "cable crossovers"
        :sort_order (alter-var-root (var *chest-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-chest-upper)}
                          {:muscle_id (:id m-chest-lower)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-cable)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "kneeling push-up"
        :sort_order (alter-var-root (var *chest-sort-order*) inc)
        :aliases [{:id 0 :alias "kneeling press-up"}
                  {:id 1 :alias "kneeling floor dip"}]}
       (assoc-body 0.4)
       (assoc :primary-muscles [{:muscle_id (:id m-chest-upper)}
                                {:muscle_id (:id m-chest-lower)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-deltoids-front)}
                                  {:muscle_id (:id m-serratus)}
                                  {:muscle_id (:id m-traps)}
                                  {:muscle_id (:id m-tricep-lateral-head)}
                                  {:muscle_id (:id m-tricep-long-head)}
                                  {:muscle_id (:id m-tricep-medial-head)}]))

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Shoulder movements
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "face pull"
        :sort_order (alter-var-root (var *shoulder-sort-order*) inc)
        :aliases [{:id 0 :alias "rear delt row"}]
        :primary-muscles [{:muscle_id (:id m-deltoids-rear)}]
        :secondary-muscles [{:muscle_id (:id m-deltoids-side)}
                            {:muscle_id (:id m-traps)}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-cable)))))

   (-> {:id (alter-var-root (var *v3-movement-id*) inc)
        :canonical_name "high pull"
        :sort_order (alter-var-root (var *shoulder-sort-order*) inc)
        :aliases []
        :primary-muscles [{:muscle_id (:id m-deltoids-rear)}
                          {:muscle_id (:id m-traps)}]
        :secondary-muscles [{:muscle_id (:id m-hamstrings)}
                            {:muscle_id (:id m-glutes)}
                            {:muscle_id (:id m-back-lower)}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))
   ]
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v18 Movements
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *v18-movement-id* 87) ; fyi, high pull is ID 87

(def v18-movements
  [
   (-> {:id (alter-var-root (var *v18-movement-id*) inc) ; will be ID 88
        :canonical_name "reverse pushdowns"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-tricep-long-head)}
                          {:muscle_id (:id m-tricep-lateral-head)}
                          {:muscle_id (:id m-tricep-medial-head)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-cable)))))
   (-> {:id (alter-var-root (var *v18-movement-id*) inc)
        :canonical_name "rope pushdowns"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-tricep-long-head)}
                          {:muscle_id (:id m-tricep-lateral-head)}
                          {:muscle_id (:id m-tricep-medial-head)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-cable)))))
   (-> {:id (alter-var-root (var *v18-movement-id*) inc)
        :canonical_name "overhead extensions"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-tricep-long-head)}
                          {:muscle_id (:id m-tricep-lateral-head)}
                          {:muscle_id (:id m-tricep-medial-head)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell)))))
   (-> {:id (alter-var-root (var *v18-movement-id*) inc)
        :canonical_name "overhead rope extensions"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-tricep-long-head)}
                          {:muscle_id (:id m-tricep-lateral-head)}
                          {:muscle_id (:id m-tricep-medial-head)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-cable)))))
   (-> {:id (alter-var-root (var *v18-movement-id*) inc)
        :canonical_name "bench dips"
        :sort_order 0
        :aliases [{:id 45 :alias "tricep bench dips"}]
        :primary-muscles [{:muscle_id (:id m-tricep-long-head)}
                          {:muscle_id (:id m-tricep-lateral-head)}
                          {:muscle_id (:id m-tricep-medial-head)}]
        :secondary-muscles []}
       (assoc :is_body_lift true)
       (assoc :percentage_of_body_weight 0.75)
       (assoc :variant_mask 0))
   (-> {:id (alter-var-root (var *v18-movement-id*) inc)
        :canonical_name "grippers"
        :sort_order 0
        :aliases [{:id 46 :alias "Captains of Crush Grippers"}
                  {:id 47 :alias "hand grippers"}]
        :primary-muscles [{:muscle_id (:id m-forearms)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask 0))
   ]
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; more (new) Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn assoc-all
  [movement key muscles]
  ; gives us deep merge (https://clojuredocs.org/clojure.core/merge-with)
  (merge-with into
              movement
              (assoc {} key muscles)))

(defn assoc-all-triceps
  [movement key]
  (assoc-all movement key [{:muscle_id (:id m-tricep-long-head)}
                           {:muscle_id (:id m-tricep-lateral-head)}
                           {:muscle_id (:id m-tricep-medial-head)}]))

(defn assoc-all-abs
  [movement key]
  (assoc-all movement key [{:muscle_id (:id m-abs-upper)}
                           {:muscle_id (:id m-abs-lower)}]))

(defn assoc-all-delts
  [movement key]
  (assoc-all movement key [{:muscle_id (:id m-deltoids-rear)}
                           {:muscle_id (:id m-deltoids-side)}
                           {:muscle_id (:id m-deltoids-front)}]))

(defn assoc-all-chest
  [movement key]
  (assoc-all movement key [{:muscle_id (:id m-chest-upper)}
                           {:muscle_id (:id m-chest-lower)}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v19 Movements
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *v19-movement-id* 93) ; fyi, grippers is ID 93
(def ^:dynamic *v19-movement-alias-id* 49) ; fyi, 'barbell full squats' is ID 49

(def v19-movements
  [
   (-> {:id (alter-var-root (var *v19-movement-id*) inc) ; will be ID 94
        :canonical_name "power cleans"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-hamstrings)}]
        :secondary-muscles [{:muscle_id (:id m-glutes)}
                            {:muscle_id (:id m-back-upper)}
                            {:muscle_id (:id m-back-lower)}
                            {:muscle_id (:id m-deltoids-rear)}
                            {:muscle_id (:id m-deltoids-side)}
                            {:muscle_id (:id m-deltoids-front)}
                            {:muscle_id (:id m-calfs)}
                            {:muscle_id (:id m-quadriceps)}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))
   (-> {:id (alter-var-root (var *v19-movement-id*) inc)
        :canonical_name "bent-over two-dumbbell rows"
        :sort_order 0
        :aliases []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell))))
       (assoc :primary-muscles [{:muscle_id (:id m-back-upper)}
                                {:muscle_id (:id m-back-lower)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-biceps)}]))
   (-> {:id (alter-var-root (var *v19-movement-id*) inc)
        :canonical_name "air squats"
        :sort_order 0
        :aliases [{:id (alter-var-root (var *v19-movement-alias-id*) inc) ; will be ID 50
                   :alias "body weight squats"}]}
       (assoc :is_body_lift true)
       (assoc :percentage_of_body_weight 0.75)
       (assoc :variant_mask 0)
       (assoc :primary-muscles [{:muscle_id (:id m-quadriceps)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-glutes)}
                                  {:muscle_id (:id m-hamstrings)}]))
   (-> {:id (alter-var-root (var *v19-movement-id*) inc)
        :canonical_name "Zercher squats"
        :sort_order 0
        :aliases []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-smith-machine))))
       (assoc :primary-muscles [{:muscle_id (:id m-quadriceps)}])
       (assoc :secondary-muscles [{:muscle_id (:id m-glutes)}
                                  {:muscle_id (:id m-hamstrings)}]))
   (-> {:id (alter-var-root (var *v19-movement-id*) inc)
        :canonical_name "dead-stop push-up"
        :sort_order 0
        :aliases []}
       (assoc-body push-up-percent-of-body-weight)
       (assoc-all-chest :primary-muscles)
       (assoc-all-triceps :primary-muscles)
       (assoc-all :primary-muscles [{:muscle_id (:id m-deltoids-front)}
                                    {:muscle_id (:id m-serratus)}
                                    {:muscle_id (:id m-traps)}]))
   (-> {:id (alter-var-root (var *v19-movement-id*) inc)
        :canonical_name "plank push-up"
        :sort_order 0
        :aliases []}
       (assoc :is_body_lift true)
       (assoc-body push-up-percent-of-body-weight)
       (assoc-all-chest :primary-muscles)
       (assoc-all-triceps :primary-muscles)
       (assoc-all-abs :primary-muscles)
       (assoc-all-delts :primary-muscles))
   (-> {:id (alter-var-root (var *v19-movement-id*) inc)
        :canonical_name "pullovers"
        :sort_order 0
        :aliases []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell))
                                (bit-or (:id variant-cable))))
       (assoc-all-chest :primary-muscles)
       (assoc-all :primary-muscles [{:muscle_id (:id m-serratus)}]))
   (-> {:id (alter-var-root (var *v19-movement-id*) inc)
        :canonical_name "rope pulls"
        :sort_order 0
        :aliases []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-cable))))
       (assoc-all :primary-muscles [{:muscle_id (:id m-serratus)}]))
   (-> {:id (alter-var-root (var *v19-movement-id*) inc)
        :canonical_name "hanging serratus crunches"
        :sort_order 0
        :aliases []}
       (assoc :is_body_lift true)
       (assoc-body 0.50)
       (assoc-all :primary-muscles [{:muscle_id (:id m-serratus)}]))
   ]
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v20 Movements
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *v20-movement-id* 102) ; fyi, hanging serratus crunches is ID 102
(def ^:dynamic *v20-movement-alias-id* 50) ; fyi, 'body weight squats' is ID 50

(def v20-movements
  [
   (-> {:id (alter-var-root (var *v20-movement-id*) inc) ; will be ID 103
        :canonical_name "landmine press"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-chest-upper)}
                          {:muscle_id (:id m-chest-lower)}]
        :secondary-muscles [{:muscle_id (:id m-deltoids-front)}
                            {:muscle_id (:id m-serratus)}
                            {:muscle_id (:id m-tricep-long-head)}
                            {:muscle_id (:id m-tricep-lateral-head)}
                            {:muscle_id (:id m-tricep-medial-head)}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))
   (-> {:id (alter-var-root (var *v20-movement-id*) inc) ; will be ID 103
        :canonical_name "landmine squats"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles [{:muscle_id (:id m-hamstrings)}
                            {:muscle_id (:id m-glutes)}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))
   (-> {:id (alter-var-root (var *v20-movement-id*) inc)
        :canonical_name "barbell thruster"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}
                          {:muscle_id (:id m-hamstrings)}
                          {:muscle_id (:id m-glutes)}
                          {:muscle_id (:id m-deltoids-front)}
                          {:muscle_id (:id m-deltoids-side)}
                          {:muscle_id (:id m-back-upper)}]
        :secondary-muscles [{:muscle_id (:id m-traps)}]}
       (assoc-all-triceps :secondary-muscles)
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))
   (-> {:id (alter-var-root (var *v20-movement-id*) inc)
        :canonical_name "landmine thruster"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}
                          {:muscle_id (:id m-hamstrings)}
                          {:muscle_id (:id m-glutes)}
                          {:muscle_id (:id m-deltoids-front)}
                          {:muscle_id (:id m-deltoids-side)}
                          {:muscle_id (:id m-back-upper)}]
        :secondary-muscles [{:muscle_id (:id m-traps)}]}
       (assoc-all-triceps :secondary-muscles)
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))
   (-> {:id (alter-var-root (var *v20-movement-id*) inc)
        :canonical_name "rotational single-arm press"
        :sort_order 0
        :aliases [{:id (alter-var-root (var *v20-movement-alias-id*) inc) ; will be ID 51
                   :alias "landmine rotational single-arm press"}]
        :primary-muscles [{:muscle_id (:id m-glutes)}]
        :secondary-muscles []}
       (assoc-all-delts :primary-muscles)
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))
   (-> {:id (alter-var-root (var *v20-movement-id*) inc)
        :canonical_name "landmine anti-rotations"
        :sort_order 0
        :aliases [{:id (alter-var-root (var *v20-movement-alias-id*) inc)
                   :alias "landmine 180s"}
                  {:id (alter-var-root (var *v20-movement-alias-id*) inc)
                   :alias "landmine twists"}
                  {:id (alter-var-root (var *v20-movement-alias-id*) inc)
                   :alias "landmine rotations"}]
        :primary-muscles [{:muscle_id (:id m-abs-upper)}
                          {:muscle_id (:id m-abs-lower)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))
   (-> {:id (alter-var-root (var *v20-movement-id*) inc)
        :canonical_name "single-arm landmine row"
        :sort_order 0
        :aliases [{:id (alter-var-root (var *v20-movement-alias-id*) inc)
                   :alias "Meadows row"}]
        :primary-muscles [{:muscle_id (:id m-back-upper)}
                          {:muscle_id (:id m-back-lower)}]
        :secondary-muscles [{:muscle_id (:id m-biceps)}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))
   (-> {:id (alter-var-root (var *v20-movement-id*) inc)
        :canonical_name "typewriter push-up"
        :sort_order 0
        :aliases []}
       (assoc :is_body_lift true)
       (assoc-body push-up-percent-of-body-weight)
       (assoc-all-chest :primary-muscles)
       (assoc-all-triceps :secondary-muscles)
       (assoc-all :secondary-muscles [{:muscle_id (:id m-deltoids-front)}
                                      {:muscle_id (:id m-serratus)}
                                      {:muscle_id (:id m-traps)}]))
   (-> {:id (alter-var-root (var *v20-movement-id*) inc)
        :canonical_name "high rows"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-back-upper)}
                          {:muscle_id (:id m-back-lower)}]
        :secondary-muscles [{:muscle_id (:id m-biceps)}]}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-machine)))))
   ]
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v21 Movements
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *v21-movement-id* 111) ; fyi, high rows is ID 111

(def v21-movements
  [
   (-> {:id (alter-var-root (var *v21-movement-id*) inc) ; will be ID 112
        :canonical_name "rear delt machine"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-deltoids-rear)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-machine)))))
   (-> {:id (alter-var-root (var *v21-movement-id*) inc)
        :canonical_name "calf press"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-calfs)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-machine))
                                (bit-or (:id variant-sled)))))
   (-> {:id (alter-var-root (var *v21-movement-id*) inc)
        :canonical_name "burpee"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles [{:muscle_id (:id m-hamstrings)}
                            {:muscle_id (:id m-glutes)}
                            {:muscle_id (:id m-calfs)}]}
       (assoc :is_body_lift true)
       (assoc :percentage_of_body_weight 0.85)
       (assoc :variant_mask 0))
   (-> {:id (alter-var-root (var *v21-movement-id*) inc)
        :canonical_name "close-grip push-up"
        :sort_order 0
        :aliases []}
       (assoc :is_body_lift true)
       (assoc-body push-up-percent-of-body-weight)
       (assoc-all-chest :secondary-muscles)
       (assoc-all-triceps :primary-muscles))
   (-> {:id (alter-var-root (var *v21-movement-id*) inc)
        :canonical_name "hang power clean"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}]
        :secondary-muscles [{:muscle_id (:id m-back-upper)}
                            {:muscle_id (:id m-back-lower)}
                            {:muscle_id (:id m-deltoids-front)}
                            {:muscle_id (:id m-deltoids-rear)}
                            {:muscle_id (:id m-deltoids-side)}
                            {:muscle_id (:id m-calfs)}
                            {:muscle_id (:id m-glutes)}
                            {:muscle_id (:id m-hamstrings)}
                            {:muscle_id (:id m-traps)}]
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-barbell)))))
   (-> {:id (alter-var-root (var *v21-movement-id*) inc)
        :canonical_name "JM press"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-tricep-long-head)}
                          {:muscle_id (:id m-tricep-lateral-head)}
                          {:muscle_id (:id m-tricep-medial-head)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-curl-bar))
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-smith-machine)))))
   (-> {:id (alter-var-root (var *v21-movement-id*) inc)
        :canonical_name "pike press"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-deltoids-front)}]
        :secondary-muscles [{:muscle_id (:id m-tricep-long-head)}
                            {:muscle_id (:id m-tricep-lateral-head)}
                            {:muscle_id (:id m-tricep-medial-head)}
                            {:muscle_id (:id m-chest-upper)}]}
       (assoc-body push-up-percent-of-body-weight))
   (-> {:id (alter-var-root (var *v21-movement-id*) inc)
        :canonical_name "pike push-up"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-chest-upper)}]
        :secondary-muscles [{:muscle_id (:id m-tricep-long-head)}
                            {:muscle_id (:id m-tricep-lateral-head)}
                            {:muscle_id (:id m-tricep-medial-head)}
                            {:muscle_id (:id m-deltoids-front)}]}
       (assoc-body push-up-percent-of-body-weight))
   (-> {:id (alter-var-root (var *v21-movement-id*) inc)
        :canonical_name "rear lunges"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}
                          {:muscle_id (:id m-glutes)}]
        :secondary-muscles [{:muscle_id (:id m-hamstrings)}]
        :percentage_of_body_weight 0.75
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-body))
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-dumbbell)))))
   (-> {:id (alter-var-root (var *v21-movement-id*) inc)
        :canonical_name "seated calf raises"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-calfs)}]
        :secondary-muscles []
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-machine)))))
   (-> {:id (alter-var-root (var *v21-movement-id*) inc)
        :canonical_name "side lunges"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}
                          {:muscle_id (:id m-glutes)}]
        :secondary-muscles [{:muscle_id (:id m-hamstrings)}]
        :percentage_of_body_weight 0.75
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-body))
                                (bit-or (:id variant-dumbbell)))))
   (-> {:id (alter-var-root (var *v21-movement-id*) inc)
        :canonical_name "step-up"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-quadriceps)}
                          {:muscle_id (:id m-glutes)}]
        :secondary-muscles [{:muscle_id (:id m-hamstrings)}]
        :percentage_of_body_weight 0.75
        :is_body_lift false}
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-body))
                                (bit-or (:id variant-barbell))
                                (bit-or (:id variant-dumbbell)))))
   ]
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; v22 Movements
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ^:dynamic *v22-movement-id* 123) ; fyi, step-up is ID 123

(def v22-movements
  [
   (-> {:id (alter-var-root (var *v22-movement-id*) inc) ; will be ID 124
        :canonical_name "dumbbell rotational punch"
        :sort_order 0
        :aliases []
        :primary-muscles [{:muscle_id (:id m-deltoids-rear)}
                          {:muscle_id (:id m-deltoids-front)}
                          {:muscle_id (:id m-deltoids-side)}
                          {:muscle_id (:id m-serratus)}]
        :secondary-muscles []}
       (assoc :is_body_lift false)
       (assoc :variant_mask (-> 0
                                (bit-or (:id variant-dumbbell)))))
   ]
  )
