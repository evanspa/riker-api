(ns riker-app.rest.resource.user.version.password-reset-res-v001
  (:require [riker-app.rest.user-meta :as meta]
            [clojure.tools.logging :as log]
            [riker-app.core.user-dao :as usercore]
            [riker-app.rest.user-meta :as meta]
            [riker-app.rest.resource.user.password-reset-res :refer [body-data-in-transform-fn
                                                                     body-data-out-transform-fn
                                                                     do-password-reset-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   post-as-do-password-reset-input]
  (identity post-as-do-password-reset-input))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   base-url
   entity-uri-prefix
   password-reset-uri
   post-as-do-password-reset-result]
  (identity post-as-do-password-reset-result))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 do password reset function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod do-password-reset-fn meta/v001
  [ctx
   version
   db-spec
   base-url
   entity-uri-prefix
   password-reset-uri
   _ ; auth token (obviously not relevant here)
   body-data
   merge-embedded-fn
   merge-links-fn]
  (let [{new-password :new-password
         email :email
         password-reset-token :password-reset-token} body-data]
    (if (not (nil? new-password))
      (do
        (usercore/reset-password db-spec email password-reset-token new-password)
        {:status 204})
      {:status 400})))
