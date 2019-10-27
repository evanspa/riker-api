(ns riker-app.rest.resource.user.login-res
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

(declare body-data-in-transform-fn)
(declare body-data-out-transform-fn)
(declare do-login-fn)
(declare do-light-login-fn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-login-post!
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   entity-uri
   embedded-resources-fn
   links-fn
   login-fn
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email
   trial-expired-grace-period-in-days]
  (rucore/put-or-post-invoker ctx
                              :post-as-do
                              db-spec
                              base-url
                              entity-uri-prefix
                              entity-uri
                              embedded-resources-fn
                              links-fn
                              []
                              nil ; plaintext-auth-token
                              nil ; user-validator-fn
                              nil ; any-issues-bit
                              body-data-in-transform-fn
                              body-data-out-transform-fn
                              nil ; next-entity-id-fn
                              nil ; save-new-entity-fn
                              nil ; save-entity-fn
                              nil ; hdr-establish-session
                              nil ; make-session-fn
                              (fn [& args] ; post-as-do-fn
                                (apply login-fn (flatten (conj (vec args)
                                                               trial-expired-grace-period-in-days))))
                              nil ; if-unmodified-since-hdr
                              (fn [exc-and-params]
                                (usercore/send-email err-notification-mustache-template
                                                     exc-and-params
                                                     err-subject
                                                     err-from-email
                                                     err-to-email
                                                     config/r-domain
                                                     config/r-mailgun-api-key))
                              (fn [body-data] (dissoc body-data :user/password))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version body-data-in-transform-fn meta/v001)
(defmulti-by-version body-data-out-transform-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; do login function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version do-login-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; do light login function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version do-light-login-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resources Definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource login-res
  [db-spec
   mt-subtype-prefix
   hdr-auth-token
   hdr-error-mask
   base-url
   entity-uri-prefix
   embedded-resources-fn-maker
   links-fn
   login-failed-reason-hdr
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email
   trial-expired-grace-period-in-days]
  :available-media-types (rucore/enumerate-media-types (meta/supported-media-types mt-subtype-prefix))
  :available-charsets rumeta/supported-char-sets
  :available-languages rumeta/supported-languages
  :allowed-methods [:post]
  :known-content-type? (rucore/known-content-type-predicate (meta/supported-media-types mt-subtype-prefix))
  :post! (fn [ctx] (handle-login-post! ctx
                                       db-spec
                                       base-url
                                       entity-uri-prefix
                                       (:uri (:request ctx))
                                       (embedded-resources-fn-maker ctx)
                                       links-fn
                                       do-login-fn
                                       err-notification-mustache-template
                                       err-subject
                                       err-from-email
                                       err-to-email
                                       trial-expired-grace-period-in-days))
  :handle-created (fn [ctx] (rucore/handle-resp ctx
                                                hdr-auth-token
                                                hdr-error-mask
                                                login-failed-reason-hdr)))

(defresource light-login-res
  [db-spec
   mt-subtype-prefix
   hdr-auth-token
   hdr-error-mask
   base-url
   entity-uri-prefix
   login-failed-reason-hdr
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email
   trial-expired-grace-period-in-days]
  :available-media-types (rucore/enumerate-media-types (meta/supported-media-types mt-subtype-prefix))
  :available-charsets rumeta/supported-char-sets
  :available-languages rumeta/supported-languages
  :allowed-methods [:post]
  :known-content-type? (rucore/known-content-type-predicate (meta/supported-media-types mt-subtype-prefix))
  :post! (fn [ctx] (handle-login-post! ctx
                                       db-spec
                                       base-url
                                       entity-uri-prefix
                                       (:uri (:request ctx))
                                       nil ; embedded-resources-fn
                                       nil ; links-fn
                                       do-light-login-fn
                                       err-notification-mustache-template
                                       err-subject
                                       err-from-email
                                       err-to-email
                                       trial-expired-grace-period-in-days))
  :handle-created (fn [ctx] (rucore/handle-resp ctx
                                                hdr-auth-token
                                                hdr-error-mask
                                                login-failed-reason-hdr)))
