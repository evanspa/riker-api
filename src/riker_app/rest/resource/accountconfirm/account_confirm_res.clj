(ns riker-app.rest.resource.accountconfirm.account-confirm-res
  (:require [clojure.tools.logging :as log]
            [liberator.core :refer [defresource]]
            [riker-app.rest.utils.macros :refer [defmulti-by-version]]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.meta :as rumeta]
            [riker-app.rest.user-utils :as userresutils]
            [riker-app.core.user-dao :as usercore]
            [riker-app.rest.meta :as meta]
            [riker-app.core.validation :as rval]
            [riker-app.app.config :as config]))

(declare body-data-in-transform-fn)
(declare do-account-confirm-fn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-account-confirm-post!
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   entity-uri
   hdr-establish-session
   user-id
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
        err-to-email (nth more 7)]
    (rucore/put-or-post-invoker ctx
                                :post-as-do
                                db-spec
                                base-url
                                entity-uri-prefix
                                entity-uri
                                nil ; embedded-resources-fn
                                nil ; links-fn
                                [user-id]
                                nil ; plaintext-auth-token
                                nil ; account-confirm-validator-fn
                                rval/account-confirm-any-issues
                                body-data-in-transform-fn
                                nil ; body-data-out-transform-fn
                                nil ; next-entity-id-fn
                                nil ; save-new-entity-fn
                                nil ; save-entity-fn
                                nil ; hdr-establish-session (not used in post-as-do)
                                nil ; make-session-fn (not used in post-as-do)
                                (fn [ctx
                                     version
                                     db-spec
                                     user-id
                                     base-url
                                     entity-uri-prefix
                                     account-confirm-uri
                                     plaintext-auth-token ; not used (will be nil)
                                     account-confirm-post-as-do-body
                                     merge-embedded-fn ; not used
                                     merge-links-fn] ; not used
                                  (do-account-confirm-fn version
                                                         db-spec
                                                         user-id
                                                         account-confirm-post-as-do-body
                                                         welcome-and-verification-email-mustache-template
                                                         welcome-and-verification-email-subject-line
                                                         welcome-and-verification-email-from
                                                         verification-url-maker-fn
                                                         verification-flagged-url-maker-fn
                                                         new-user-notification-mustache-template
                                                         new-user-notification-subject
                                                         new-user-notification-from-email
                                                         new-user-notification-to-email))
                                nil ; if-unmodified-since-hdr
                                (fn [exc-and-params]
                                  (usercore/send-email err-notification-mustache-template
                                                       exc-and-params
                                                       err-subject
                                                       err-from-email
                                                       err-to-email
                                                       config/r-domain
                                                       config/r-mailgun-api-key))
                                #(identity %))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version body-data-in-transform-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Perform account confirmation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version do-account-confirm-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resource definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource account-confirm-res
  [db-spec
   mt-subtype-prefix
   hdr-auth-token
   hdr-error-mask
   base-url
   entity-uri-prefix
   hdr-establish-session
   user-id
   welcome-and-verification-email-mustache-template
   welcome-and-verification-email-subject-line
   welcome-and-verification-email-from
   verification-url-maker-fn
   verification-flagged-url-maker-fn
   &
   more]
  :available-media-types (rucore/enumerate-media-types (meta/supported-media-types mt-subtype-prefix))
  :available-charstripe-tokens rumeta/supported-char-sets
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
                 err-to-email (nth more 7)]
             (handle-account-confirm-post! ctx
                                           db-spec
                                           base-url
                                           entity-uri-prefix
                                           (:uri (:request ctx))
                                           hdr-establish-session
                                           user-id
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
                                           err-to-email)))
  :handle-created (fn [ctx] (rucore/handle-resp ctx hdr-auth-token hdr-error-mask)))
