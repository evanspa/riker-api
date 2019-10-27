(ns riker-app.rest.resource.user.version.send-password-reset-email-res-v001
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
            [riker-app.rest.resource.user.send-password-reset-email-res :refer [body-data-in-transform-fn
                                                                                do-send-password-reset-email-fn]]
            [riker-app.app.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   post-as-do-send-password-reset-email-input]
  (identity post-as-do-send-password-reset-email-input))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 do send password-reset email function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod do-send-password-reset-email-fn meta/v001
  [ctx
   version
   db-spec
   send-password-reset-post-as-do-body
   password-reset-email-mustache-template
   password-reset-email-subject-line
   password-reset-email-from
   password-reset-url-maker-fn
   password-reset-flagged-url-maker-fn
   trial-expired-grace-period-in-days]
  (usercore/send-password-reset-notice db-spec
                                       (:user/email send-password-reset-post-as-do-body)
                                       password-reset-email-mustache-template
                                       password-reset-email-subject-line
                                       password-reset-email-from
                                       password-reset-url-maker-fn
                                       password-reset-flagged-url-maker-fn
                                       trial-expired-grace-period-in-days
                                       config/r-domain
                                       config/r-mailgun-api-key)
  {:status 204})
