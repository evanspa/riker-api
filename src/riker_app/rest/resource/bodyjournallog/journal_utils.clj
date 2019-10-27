(ns riker-app.rest.resource.bodyjournallog.journal-utils
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.user-utils :as userrestutil]
            [riker-app.rest.meta :as meta]))

(defn body-journal-log-in-transform
  [body-journal-log]
  (-> body-journal-log
      (ucore/transform-map-val :bodyjournallog/imported-at #(c/from-long (Long. %)))
      (ucore/transform-map-val :bodyjournallog/logged-at #(c/from-long (Long. %)))))

(defn body-journal-log-out-transform
  [{user-id :bodyjournallog/user-id :as body-journal-log}
   base-url
   entity-url-prefix]
  (-> body-journal-log
      (ucore/assoc-if-contains body-journal-log :bodyjournallog/origination-device-id :bodyjournallog/origination-device #(userrestutil/make-user-subentity-url base-url
                                                                                                                                                                entity-url-prefix
                                                                                                                                                                user-id
                                                                                                                                                                meta/pathcomp-origination-devices
                                                                                                                                                                %))
      (userrestutil/user-out-transform)
      (ucore/transform-map-val :bodyjournallog/created-at #(c/to-long %))
      (ucore/transform-map-val :bodyjournallog/deleted-at #(c/to-long %))
      (ucore/transform-map-val :bodyjournallog/updated-at #(c/to-long %))
      (ucore/transform-map-val :bodyjournallog/logged-at  #(c/to-long %))
      (ucore/transform-map-val :bodyjournallog/imported-at  #(c/to-long %))
      (dissoc :bodyjournallog/updated-count)
      (dissoc :bodyjournallog/user-id)))
