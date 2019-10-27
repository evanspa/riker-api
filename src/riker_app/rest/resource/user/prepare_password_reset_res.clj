(ns riker-app.rest.resource.user.prepare-password-reset-res
  (:require [liberator.core :refer [defresource]]
            [riker-app.rest.user-meta :as meta]
            [clojure.tools.logging :as log]
            [riker-app.rest.utils.macros :refer [defmulti-by-version]]
            [liberator.representation :refer [ring-response]]
            [clostache.parser :refer [render-resource]]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.meta :as rumeta]
            [riker-app.rest.user-utils :as userresutils]
            [riker-app.core.user-dao :as usercore]
            [ring.util.response :refer [redirect]]
            [riker-app.rest.resource.user.password-reset-util :as pwdresetutil]
            [riker-app.app.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-prepare-password-reset
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   prepare-password-reset-uri
   email
   password-reset-token
   password-reset-web-url
   password-reset-error-web-url
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email
   trial-expired-grace-period-in-days]
  (try
    (let [user (usercore/prepare-password-reset db-spec
                                                email
                                                password-reset-token
                                                trial-expired-grace-period-in-days)]
      (if (not (nil? user))
        (ring-response (redirect password-reset-web-url))
        (ring-response (redirect password-reset-error-web-url))))
    (catch IllegalArgumentException e
      (ring-response
       (redirect (str password-reset-error-web-url "/" (.getMessage e)))))
    (catch clojure.lang.ExceptionInfo e
      (let [cause (-> e ex-data :cause)]
        (cond
          (= cause :trial-and-grace-expired) (ring-response (redirect password-reset-error-web-url))
          :else (ring-response (redirect password-reset-error-web-url)))))
    (catch Exception e
      (log/error e (str "Exception in handle-prepare-password-reset. (email: "
                        email
                        ", password-reset-web-url: "
                        password-reset-web-url
                        ", password-reset-error-web-url: "
                        password-reset-error-web-url ")"))
      (usercore/send-email err-notification-mustache-template
                           {:exception e}
                           err-subject
                           err-from-email
                           err-to-email
                           config/r-domain
                           config/r-mailgun-api-key)
      (ring-response (redirect password-reset-error-web-url)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resource definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource prepare-password-reset-res
  [db-spec
   base-url
   entity-uri-prefix
   email
   password-reset-token
   password-reset-web-url
   password-reset-error-web-url
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email
   trial-expired-grace-period-in-days]
  :available-media-types ["text/html"]
  :allowed-methods [:get]
  :handle-ok (fn [ctx]
               (handle-prepare-password-reset ctx
                                              db-spec
                                              base-url
                                              entity-uri-prefix
                                              (:uri (:request ctx))
                                              email
                                              password-reset-token
                                              password-reset-web-url
                                              password-reset-error-web-url
                                              err-notification-mustache-template
                                              err-subject
                                              err-from-email
                                              err-to-email
                                              trial-expired-grace-period-in-days)))
