(ns riker-app.rest.resource.usersettings.usersettings-utils
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [pe-core-utils.core :as ucore]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.user-utils :as userrestutil]
            [riker-app.rest.meta :as meta]))

(defn usersettings-in-transform
  [usersettings]
  usersettings)

(defn usersettings-out-transform
  [{user-id :usersettings/user-id :as usersettings}
   base-url
   entity-url-prefix]
  (-> usersettings
      (userrestutil/user-out-transform)
      (ucore/transform-map-val :usersettings/created-at #(c/to-long %))
      (ucore/transform-map-val :usersettings/deleted-at #(c/to-long %))
      (ucore/transform-map-val :usersettings/updated-at #(c/to-long %))
      (dissoc :usersettings/updated-count)
      (dissoc :usersettings/user-id)))
