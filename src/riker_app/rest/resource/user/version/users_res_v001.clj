(ns riker-app.rest.resource.user.version.users-res-v001
  (:require [riker-app.rest.user-meta :as meta]
            [clojure.tools.logging :as log]
            [pe-core-utils.core :as ucore]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.meta :as rumeta]
            [riker-app.core.user-dao :as usercore]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [riker-app.core.user-validation :as userval]
            [riker-app.rest.user-utils :as userresutils]
            [riker-app.rest.resource.user.users-res :refer [new-user-validator-fn
                                                            body-data-in-transform-fn
                                                            body-data-out-transform-fn
                                                            next-user-account-id-fn
                                                            save-new-user-fn
                                                            send-email-verification-fn
                                                            make-session-fn]]
            [riker-app.app.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod new-user-validator-fn meta/v001
  [version user]
  (userval/save-new-user-validation-mask user))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user]
  (identity user))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   base-url
   entity-uri-prefix
   entity-uri
   new-user-id
   new-user]
  (userresutils/user-out-transform new-user))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Next user account id function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod next-user-account-id-fn meta/v001
  [version db-spec]
  (usercore/next-user-account-id db-spec))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Save new user function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-new-user-fn meta/v001
  [ctx
   version
   db-spec
   _ ; plain text auth-token (not relevant; always null)
   new-user-id
   user
   trial-period-in-days]
  (usercore/save-new-user db-spec
                          new-user-id
                          (-> user
                              (assoc :user/max-allowed-set-import config/max-allowed-set-import)
                              (assoc :user/max-allowed-bml-import config/max-allowed-bml-import)
                              (assoc :user/is-payment-past-due false)
                              (assoc :user/trial-ends-at (c/to-long (t/plus (t/now)
                                                                            (t/days (inc trial-period-in-days))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Send email verification function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod send-email-verification-fn meta/v001
  [version
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
                                     config/r-mailgun-api-key))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Make session function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod make-session-fn meta/v001
  [version db-spec user-entid]
  (let [new-token-id (usercore/next-auth-token-id db-spec)]
    (usercore/create-and-save-auth-token db-spec user-entid new-token-id)))
