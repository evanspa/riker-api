(ns riker-app.rest.resource.set.version.sets-res-v001
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.set.set-utils :as setresutils]
            [riker-app.rest.resource.set.sets-res :refer [new-set-validator-fn
                                                          body-data-in-transform-fn
                                                          body-data-out-transform-fn
                                                          save-new-set-fn
                                                          next-set-id-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod new-set-validator-fn meta/v001
  [version set]
  0)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   new-set]
  (setresutils/set-in-transform new-set))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   user-id
   base-url
   entity-uri-prefix
   entity-uri
   new-set-id
   new-set]
  (setresutils/set-out-transform new-set base-url entity-uri-prefix))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Save new set function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-new-set-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   plaintext-auth-token
   new-set-id
   set]
  (if (nil? (:set/imported-at set))
    (dao/save-new-set db-spec user-id new-set-id set)
    (let [user (:user ctx)
          is-not-verified (nil? (:user/verified-at user))
          max-import-allowed (:user/max-allowed-set-import user)
          num-imported (dao/num-imported-sets db-spec user-id)]
      (if (> num-imported max-import-allowed)
        (throw (IllegalArgumentException. (str val/set-imported-limit-exceeded)))
        (if is-not-verified
          (throw (IllegalArgumentException. (str val/set-imported-unverified-email)))
          (dao/save-new-set db-spec user-id new-set-id set))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Next set id function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod next-set-id-fn meta/v001
  [version db-spec]
  (dao/next-set-id db-spec))
