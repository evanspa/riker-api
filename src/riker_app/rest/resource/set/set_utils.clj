(ns riker-app.rest.resource.set.set-utils
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.user-utils :as userrestutil]
            [riker-app.rest.meta :as meta]))

(defn set-in-transform
  [set]
  (-> set
      (ucore/assoc-if-contains set :set/movement :set/movement-id rucore/entity-id-from-uri)
      (ucore/transform-map-val :set/imported-at #(c/from-long (Long. %)))
      (ucore/transform-map-val :set/logged-at #(c/from-long (Long. %)))))

(defn set-out-transform
  [{user-id :set/user-id :as set}
   base-url
   entity-url-prefix]
  (-> set
      (ucore/assoc-if-contains set :set/movement-id :set/movement #(userrestutil/make-user-subentity-url base-url
                                                                                                         entity-url-prefix
                                                                                                         user-id
                                                                                                         meta/pathcomp-movements
                                                                                                         %))
      (ucore/assoc-if-contains set :set/movement-variant-id :set/movement-variant #(userrestutil/make-user-subentity-url base-url
                                                                                                                         entity-url-prefix
                                                                                                                         user-id
                                                                                                                         meta/pathcomp-movement-variants
                                                                                                                         %))
      (ucore/assoc-if-contains set :set/origination-device-id :set/origination-device #(userrestutil/make-user-subentity-url base-url
                                                                                                                             entity-url-prefix
                                                                                                                             user-id
                                                                                                                             meta/pathcomp-origination-devices
                                                                                                                             %))
      (userrestutil/user-out-transform)
      (ucore/transform-map-val :set/created-at #(c/to-long %))
      (ucore/transform-map-val :set/deleted-at #(c/to-long %))
      (ucore/transform-map-val :set/updated-at #(c/to-long %))
      (ucore/transform-map-val :set/imported-at #(c/to-long %))
      (ucore/transform-map-val :set/logged-at  #(c/to-long %))
      (dissoc :set/updated-count)
      (dissoc :set/user-id)))
