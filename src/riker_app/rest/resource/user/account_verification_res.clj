(ns riker-app.rest.resource.user.account-verification-res
  (:require [liberator.core :refer [defresource]]
            [liberator.representation :refer [ring-response]]
            [clojure.java.jdbc :as j]
            [clojure.tools.logging :as log]
            [clostache.parser :refer [render-resource]]
            [ring.util.response :refer [redirect]]
            [riker-app.core.user-ddl :as uddl]
            [riker-app.core.user-dao :as usercore]
            [riker-app.rest.user-meta :as meta]
            [riker-app.rest.utils.macros :refer [defmulti-by-version]]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.meta :as rumeta]
            [riker-app.utils :refer [log-e]]
            [riker-app.rest.user-utils :as userresutils]
            [riker-app.app.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn verify-user
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   user-uri
   email
   verification-token
   verification-success-web-url
   verification-error-web-url
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email
   trial-expired-grace-period-in-days]
  (try
    (let [user (usercore/verify-user db-spec
                                     email
                                     verification-token
                                     trial-expired-grace-period-in-days)]
      (if (not (nil? user))
        (do
          (j/delete! db-spec uddl/tbl-account-verification-token ["user_id = ?" (:user/id user)])
          (ring-response (redirect verification-success-web-url)))
        (ring-response (redirect verification-error-web-url))))
    (catch clojure.lang.ExceptionInfo e
      (let [cause (-> e ex-data :cause)]
        (cond
          (= cause :trial-and-grace-expired) (ring-response (redirect verification-error-web-url))
          :else (ring-response (redirect verification-error-web-url)))))
    (catch Exception e
      (log-e e (str "Exception in verify-user. (email: "
                    email
                    ", verification-success-web-url: "
                    verification-success-web-url
                    ", verification-error-web-url: "
                    verification-error-web-url ")"))
      (usercore/send-email err-notification-mustache-template
                           {:exception e}
                           err-subject
                           err-from-email
                           err-to-email
                           config/r-domain
                           config/r-mailgun-api-key)
      (ring-response (redirect verification-error-web-url)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resource definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource account-verification-res
  [db-spec
   base-url
   entity-uri-prefix
   email
   verification-token
   verification-success-mustache-template
   verification-error-mustache-template
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email
   trial-expired-grace-period-in-days]
  :available-media-types ["text/html"]
  :allowed-methods [:get]
  :handle-ok (fn [ctx]
               (verify-user ctx
                            db-spec
                            base-url
                            entity-uri-prefix
                            (:uri (:request ctx))
                            email
                            verification-token
                            verification-success-mustache-template
                            verification-error-mustache-template
                            err-notification-mustache-template
                            err-subject
                            err-from-email
                            err-to-email
                            trial-expired-grace-period-in-days)))
