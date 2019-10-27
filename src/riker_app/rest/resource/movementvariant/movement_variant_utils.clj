(ns riker-app.rest.resource.movementvariant.movement-variant-utils
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.user-utils :as userrestutil]
            [riker-app.rest.meta :as meta]))

(defn movement-variant-in-transform
  [movement-variant]
  (identity movement-variant))

(defn movement-variant-out-transform
  [movement-variant
   base-url
   entity-url-prefix]
  (-> movement-variant
      (userrestutil/user-out-transform)
      (ucore/transform-map-val :movementvariant/created-at #(c/to-long %))
      (ucore/transform-map-val :movementvariant/deleted-at #(c/to-long %))
      (ucore/transform-map-val :movementvariant/updated-at #(c/to-long %))
      (dissoc :movementvariant/updated-count)))
