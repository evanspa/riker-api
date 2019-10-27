(ns riker-app.rest.resource.set.version.set-res-v001
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.set.set-utils :as setresutils]
            [riker-app.rest.resource.set.set-res :refer [save-set-validator-fn
                                                         body-data-in-transform-fn
                                                         body-data-out-transform-fn
                                                         save-set-fn
                                                         delete-set-fn
                                                         load-set-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-set-validator-fn meta/v001
  [version set]
  0)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   set-entid
   set]
  (setresutils/set-in-transform set))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   user-id
   set-id
   base-url
   entity-uri-prefix
   entity-uri
   set]
  (setresutils/set-out-transform set base-url entity-uri-prefix))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Save set function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-set-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   set-id
   plaintext-auth-token
   set
   if-unmodified-since]
  (dao/save-set db-spec
                set-id
                (assoc set :set/user-id user-id)
                if-unmodified-since))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Delete set function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod delete-set-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   set-id
   delete-reason
   plaintext-auth-token
   if-unmodified-since]
  (dao/mark-set-as-deleted db-spec set-id if-unmodified-since))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Load set function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod load-set-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   set-id
   plaintext-auth-token
   if-modified-since]
  (dao/set-by-id db-spec set-id true))
