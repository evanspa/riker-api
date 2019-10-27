(ns riker-app.rest.resource.movementvariant.version.movement-variant-res-v001
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.movementvariant.movement-variant-utils :as movementvariantresutils]
            [riker-app.rest.resource.movementvariant.movement-variant-res :refer [body-data-in-transform-fn
                                                                                  body-data-out-transform-fn
                                                                                  load-movement-variant-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   movement-variant-entid
   movement-variant]
  (movementvariantresutils/movement-variant-in-transform movement-variant))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   user-id
   movement-variant-id
   base-url
   entity-uri-prefix
   entity-uri
   movement-variant]
  (movementvariantresutils/movement-variant-out-transform movement-variant base-url entity-uri-prefix))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Load movement variant function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod load-movement-variant-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   movement-variant-id
   plaintext-auth-token
   if-modified-since]
  (dao/movement-variant-by-id db-spec movement-variant-id true))
