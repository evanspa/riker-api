(ns riker-app.rest.resource.soreness.version.sorenesses-res-v001
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.soreness.soreness-utils :as sorenessresutils]
            [riker-app.rest.resource.soreness.sorenesses-res :refer [new-soreness-validator-fn
                                                                     body-data-in-transform-fn
                                                                     body-data-out-transform-fn
                                                                     save-new-soreness-fn
                                                                     next-soreness-id-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod new-soreness-validator-fn meta/v001
  [version soreness]
  0)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   new-soreness]
  (sorenessresutils/soreness-in-transform new-soreness))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   user-id
   base-url
   entity-uri-prefix
   entity-uri
   new-soreness-id
   new-soreness]
  (sorenessresutils/soreness-out-transform new-soreness base-url entity-uri-prefix))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Save new soreness function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-new-soreness-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   plaintext-auth-token
   new-soreness-id
   soreness]
  (dao/save-new-soreness db-spec
                         user-id
                         new-soreness-id
                         soreness))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Next soreness id function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod next-soreness-id-fn meta/v001
  [version db-spec]
  (dao/next-soreness-id db-spec))
