(ns riker-app.rest.resource.plan.plan-utils
  (:require [clojure.tools.logging :as log]))

(defn plan-out-transform
  [plan
   base-url
   entity-url-prefix]
  {:user/current-plan-price (:plan/current-plan-price plan)})
