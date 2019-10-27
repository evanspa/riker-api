(ns riker-app.rest.resource.user.version.logout-res-v001
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]
            [riker-app.rest.user-meta :as meta]
            [clojure.java.jdbc :as j]
            [clojure.tools.logging :as log]
            [pe-core-utils.core :as ucore]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.meta :as rumeta]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.user-validation :as userval]
            [riker-app.rest.user-meta :as meta]
            [riker-app.rest.resource.user.logout-res :refer [body-data-in-transform-fn
                                                      do-logout-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   post-as-do-logout-input]
  (identity post-as-do-logout-input))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 do logout function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod do-logout-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   base-url
   entity-uri-prefix
   logout-uri
   plaintext-auth-token
   logout-body
   merge-embedded-fn
   merge-links-fn]
  (usercore/invalidate-user-token db-spec
                                  user-id
                                  plaintext-auth-token
                                  usercore/invalrsn-logout)
  {:status 204 :logout true})
