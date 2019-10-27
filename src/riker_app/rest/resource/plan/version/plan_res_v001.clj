(ns riker-app.rest.resource.plan.version.plan-res-v001
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.plan.plan-utils :as planresutils]
            [riker-app.rest.resource.plan.plan-res :refer [body-data-out-transform-fn
                                                           load-plan-fn]]))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   base-url
   entity-uri-prefix
   entity-uri
   plan]
  (planresutils/plan-out-transform plan base-url entity-uri-prefix))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Load set function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod load-plan-fn meta/v001
  [ctx
   version
   db-spec
   plaintext-auth-token
   if-modified-since]
  (dao/plan db-spec))
