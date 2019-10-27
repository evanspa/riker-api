(ns riker-app.rest.resource.movement.movement-utils
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.user-utils :as userrestutil]
            [riker-app.rest.meta :as meta]))

(defn movement-in-transform
  [movement]
  (identity movement))

(defn movement-out-transform
  [movement
   base-url
   entity-url-prefix]
  (-> movement
      (userrestutil/user-out-transform)
      (ucore/transform-map-val :movement/created-at #(c/to-long %))
      (ucore/transform-map-val :movement/deleted-at #(c/to-long %))
      (ucore/transform-map-val :movement/updated-at #(c/to-long %))
      (dissoc :movement/updated-count)))
