(ns riker-app.rest.resource.user.version.logout-all-other-res-v001
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
            [riker-app.rest.resource.user.logout-all-other-res :refer [body-data-in-transform-fn
                                                                       do-logout-all-other-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   post-as-do-logout-input]
  (identity post-as-do-logout-input))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 do logout-all-other function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod do-logout-all-other-fn meta/v001
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
  (usercore/invalidate-user-tokens db-spec
                                   user-id
                                   usercore/invalrsn-logout-all-other)
  (let [new-token-id (usercore/next-auth-token-id db-spec)
        plaintext-token (usercore/create-and-save-auth-token db-spec user-id new-token-id)]
    {:status 200 :auth-token plaintext-token}))
