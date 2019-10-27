(ns riker-app.rest.resource.soreness.version.soreness-res-v001
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.soreness.soreness-utils :as sorenessresutils]
            [riker-app.rest.resource.soreness.soreness-res :refer [save-soreness-validator-fn
                                                                   body-data-in-transform-fn
                                                                   body-data-out-transform-fn
                                                                   save-soreness-fn
                                                                   delete-soreness-fn
                                                                   load-soreness-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-soreness-validator-fn meta/v001
  [version soreness]
  0)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   soreness-entid
   soreness]
  (sorenessresutils/soreness-in-transform soreness))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   user-id
   soreness-id
   base-url
   entity-uri-prefix
   entity-uri
   soreness]
  (sorenessresutils/soreness-out-transform soreness base-url entity-uri-prefix))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Save soreness function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-soreness-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   soreness-id
   plaintext-auth-token
   soreness
   if-unmodified-since]
  (dao/save-soreness db-spec
                     soreness-id
                     (assoc soreness :soreness/user-id user-id)
                     if-unmodified-since))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Delete soreness function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod delete-soreness-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   soreness-id
   delete-reason
   plaintext-auth-token
   if-unmodified-since]
  (dao/mark-soreness-as-deleted db-spec soreness-id if-unmodified-since))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Load soreness function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod load-soreness-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   soreness-id
   plaintext-auth-token
   if-modified-since]
  (dao/soreness-by-id db-spec soreness-id true))
