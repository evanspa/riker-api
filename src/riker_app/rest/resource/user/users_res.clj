(ns riker-app.rest.resource.user.users-res
  (:require [liberator.core :refer [defresource]]
            [liberator.representation :refer [ring-response]]
            [clj-time.core :as t]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [clojure.walk :refer [keywordize-keys]]
            [riker-app.rest.user-meta :as meta]
            [clojure.tools.logging :as log]
            [riker-app.rest.utils.macros :refer [defmulti-by-version]]
            [pe-core-utils.core :as ucore]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.meta :as rumeta]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.user-validation :as userval]
            [riker-app.app.config :as config]))

(declare process-users-post!)
(declare process-login-post!)
(declare new-user-validator-fn)
(declare body-data-in-transform-fn)
(declare body-data-out-transform-fn)
(declare save-new-user-fn)
(declare send-email-verification-fn)
(declare make-session-fn)
(declare next-user-account-id-fn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-users-post!
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   entity-uri
   hdr-establish-session
   embedded-resources-fn
   links-fn
   welcome-and-verification-email-mustache-template
   welcome-and-verification-email-subject-line
   welcome-and-verification-email-from
   verification-url-maker-fn
   verification-flagged-url-maker-fn
   &
   more]
  (let [new-user-notification-mustache-template (nth more 0)
        new-user-notification-from-email (nth more 1)
        new-user-notification-to-email (nth more 2)
        new-user-notification-subject (nth more 3)
        err-notification-mustache-template (nth more 4)
        err-subject (nth more 5)
        err-from-email (nth more 6)
        err-to-email (nth more 7)
        trial-expired-grace-period-in-days (nth more 8)
        trial-period-in-days (nth more 9)]
    (rucore/put-or-post-invoker ctx
                                :post-as-create
                                db-spec
                                base-url
                                entity-uri-prefix
                                entity-uri
                                embedded-resources-fn
                                links-fn
                                []
                                nil ; plain text auth-token (not relevant here)
                                new-user-validator-fn
                                userval/su-any-issues
                                body-data-in-transform-fn
                                body-data-out-transform-fn
                                next-user-account-id-fn
                                (fn [ctx
                                     version
                                     db-spec
                                     plaintext-authtoken ; not used
                                     new-user-id
                                     user]
                                  (let [save-result (save-new-user-fn ctx
                                                                      version
                                                                      db-spec
                                                                      plaintext-authtoken ; not used
                                                                      new-user-id
                                                                      user
                                                                      trial-period-in-days)]
                                    (when save-result
                                      (send-email-verification-fn version
                                                                  db-spec
                                                                  new-user-id
                                                                  welcome-and-verification-email-mustache-template
                                                                  welcome-and-verification-email-subject-line
                                                                  welcome-and-verification-email-from
                                                                  verification-url-maker-fn
                                                                  verification-flagged-url-maker-fn
                                                                  trial-expired-grace-period-in-days)
                                      (usercore/send-email new-user-notification-mustache-template
                                                           user
                                                           new-user-notification-subject
                                                           new-user-notification-from-email
                                                           new-user-notification-to-email
                                                           config/r-domain
                                                           config/r-mailgun-api-key))
                                    save-result))
                                nil ; save-entity-fn
                                hdr-establish-session
                                make-session-fn
                                nil ; post-as-do-fn
                                nil ; if-unmodified-since-hdr
                                (fn [exc-and-params]
                                  (usercore/send-email err-notification-mustache-template
                                                       exc-and-params
                                                       err-subject
                                                       err-from-email
                                                       err-to-email
                                                       config/r-domain
                                                       config/r-mailgun-api-key))
                                (fn [body-data] (dissoc body-data :user/password)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version new-user-validator-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version body-data-in-transform-fn meta/v001)
(defmulti-by-version body-data-out-transform-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Next user account id function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version next-user-account-id-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Save new user function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version save-new-user-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Send verification email function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version send-email-verification-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Make session function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version make-session-fn meta/v001)

(defresource users-res
  [db-spec
   mt-subtype-prefix
   hdr-auth-token
   hdr-error-mask
   base-url
   entity-uri-prefix
   hdr-establish-session
   embedded-resources-fn-maker
   links-fn
   welcome-and-verification-email-mustache-template
   welcome-and-verification-email-subject-line
   welcome-and-verification-email-from
   verification-url-maker-fn
   verification-flagged-url-maker-fn
   &
   more]
  :available-media-types (rucore/enumerate-media-types (meta/supported-media-types mt-subtype-prefix))
  :available-charsets rumeta/supported-char-sets
  :available-languages rumeta/supported-languages
  :allowed-methods [:post]
  :known-content-type? (rucore/known-content-type-predicate (meta/supported-media-types mt-subtype-prefix))
  :post! (fn [ctx]
           (let [new-user-notification-mustache-template (nth more 0)
                 new-user-notification-from-email (nth more 1)
                 new-user-notification-to-email (nth more 2)
                 new-user-notification-subject (nth more 3)
                 err-notification-mustache-template (nth more 4)
                 err-subject (nth more 5)
                 err-from-email (nth more 6)
                 err-to-email (nth more 7)
                 trial-expired-grace-period-in-days (nth more 8)
                 trial-period-in-days (nth more 9)]
             (handle-users-post! ctx
                                 db-spec
                                 base-url
                                 entity-uri-prefix
                                 (:uri (:request ctx))
                                 hdr-establish-session
                                 (embedded-resources-fn-maker ctx)
                                 links-fn
                                 welcome-and-verification-email-mustache-template
                                 welcome-and-verification-email-subject-line
                                 welcome-and-verification-email-from
                                 verification-url-maker-fn
                                 verification-flagged-url-maker-fn
                                 new-user-notification-mustache-template
                                 new-user-notification-from-email
                                 new-user-notification-to-email
                                 new-user-notification-subject
                                 err-notification-mustache-template
                                 err-subject
                                 err-from-email
                                 err-to-email
                                 trial-expired-grace-period-in-days
                                 trial-period-in-days)))
  :handle-created (fn [ctx] (rucore/handle-resp ctx hdr-auth-token hdr-error-mask)))
