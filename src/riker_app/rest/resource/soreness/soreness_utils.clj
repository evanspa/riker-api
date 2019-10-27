(ns riker-app.rest.resource.soreness.soreness-utils
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.user-utils :as userrestutil]
            [riker-app.rest.meta :as meta]))

(defn soreness-in-transform
  [soreness]
  (-> soreness
      (ucore/assoc-if-contains soreness :soreness/muscle-group :soreness/muscle-group-id rucore/entity-id-from-uri)
      (ucore/transform-map-val :soreness/logged-at #(c/from-long (Long. %)))))

(defn soreness-out-transform
  [{user-id :soreness/user-id :as soreness}
   base-url
   entity-url-prefix]
  (-> soreness
      (ucore/assoc-if-contains soreness :soreness/muscle-group-id :soreness/muscle-group #(userrestutil/make-user-subentity-url base-url
                                                                                                                                entity-url-prefix
                                                                                                                                user-id
                                                                                                                                meta/pathcomp-muscle-groups
                                                                                                                                %))
      (ucore/assoc-if-contains soreness :soreness/origination-device-id :soreness/origination-device #(userrestutil/make-user-subentity-url base-url
                                                                                                                                            entity-url-prefix
                                                                                                                                            user-id
                                                                                                                                            meta/pathcomp-origination-devices
                                                                                                                                            %))
      (userrestutil/user-out-transform)
      (ucore/transform-map-val :soreness/created-at #(c/to-long %))
      (ucore/transform-map-val :soreness/deleted-at #(c/to-long %))
      (ucore/transform-map-val :soreness/updated-at #(c/to-long %))
      (ucore/transform-map-val :soreness/logged-at  #(c/to-long %))
      (dissoc :soreness/updated-count)
      (dissoc :soreness/user-id)))
