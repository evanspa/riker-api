(ns riker-app.rest.resource.movement.version.movement-res-v001
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.movement.movement-utils :as movementresutils]
            [riker-app.rest.resource.movement.movement-res :refer [body-data-in-transform-fn
                                                                   body-data-out-transform-fn
                                                                   load-movement-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   movement-entid
   movement]
  (movementresutils/movement-in-transform movement))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   user-id
   movement-id
   base-url
   entity-uri-prefix
   entity-uri
   movement]
  (movementresutils/movement-out-transform movement base-url entity-uri-prefix))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Load movement function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod load-movement-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   movement-id
   plaintext-auth-token
   if-modified-since]
  (dao/movement-by-id db-spec movement-id true))
