(ns riker-app.rest.resource.user.version.send-verification-email-res-v001
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
            [riker-app.rest.resource.user.send-verification-email-res :refer [body-data-in-transform-fn
                                                                              do-send-verification-email-fn]]
            [riker-app.app.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   post-as-do-send-verification-email-input]
  (identity post-as-do-send-verification-email-input))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 do send verification email function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod do-send-verification-email-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   verification-email-mustache-template
   verification-email-subject-line
   verification-email-from
   verification-url-maker-fn
   flagged-url-maker-fn
   trial-expired-grace-period-in-days]
  (usercore/send-verification-notice db-spec
                                     user-id
                                     verification-email-mustache-template
                                     verification-email-subject-line
                                     verification-email-from
                                     verification-url-maker-fn
                                     flagged-url-maker-fn
                                     trial-expired-grace-period-in-days
                                     config/r-domain
                                     config/r-mailgun-api-key)
  {:status 204})
