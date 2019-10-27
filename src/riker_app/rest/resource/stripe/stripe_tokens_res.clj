(ns riker-app.rest.resource.stripe.stripe-tokens-res
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

(declare process-stripe-tokens-post!)
(declare new-stripe-token-validator-fn)
(declare body-data-in-transform-fn)
(declare body-data-out-transform-fn)
(declare process-new-stripe-token-fn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-stripe-tokens-post!
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   stripe-tokens-uri
   user-id
   plaintext-auth-token
   embedded-resources-fn
   links-fn
   stripe-api-key
   stripe-subscription-name
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email]
  (rucore/put-or-post-invoker ctx
                              :post-as-do
                              db-spec
                              base-url
                              entity-uri-prefix
                              stripe-tokens-uri
                              embedded-resources-fn
                              links-fn
                              [user-id]
                              plaintext-auth-token
                              new-stripe-token-validator-fn
                              rval/stripe-token-any-issues
                              body-data-in-transform-fn
                              body-data-out-transform-fn
                              nil ; next-entity-id-fn
                              nil ; save-new-entity-fn
                              nil ; save-entity-fn
                              nil ; hdr-establish-session
                              nil ; make-session-fn
                              (fn [ctx
                                   version
                                   db-spec
                                   user-id
                                   base-url
                                   entity-uri-prefix
                                   stripe-tokens-uri
                                   plaintext-auth-token
                                   stripe-token
                                   merge-embedded-fn
                                   merge-links-fn]
                                (process-new-stripe-token-fn ctx
                                                             version
                                                             db-spec
                                                             user-id
                                                             base-url
                                                             entity-uri-prefix
                                                             stripe-tokens-uri
                                                             plaintext-auth-token
                                                             stripe-token
                                                             merge-embedded-fn
                                                             merge-links-fn
                                                             stripe-api-key
                                                             stripe-subscription-name))
                              nil ; if-unmodified-since-hdr
                              (fn [exc-and-params]
                                (usercore/send-email err-notification-mustache-template
                                                     exc-and-params
                                                     err-subject
                                                     err-from-email
                                                     err-to-email
                                                     config/r-domain
                                                     config/r-mailgun-api-key))
                              #(identity %)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version new-stripe-token-validator-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version body-data-in-transform-fn meta/v001)
(defmulti-by-version body-data-out-transform-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Save new stripe-token function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version process-new-stripe-token-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resource definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource stripe-tokens-res
  [db-spec
   mt-subtype-prefix
   hdr-auth-token
   hdr-error-mask
   auth-scheme
   auth-scheme-param-name
   base-url
   entity-uri-prefix
   user-id
   embedded-resources-fn
   links-fn
   stripe-api-key
   stripe-subscription-name
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email
   &
   more]
  :available-media-types (rucore/enumerate-media-types (meta/supported-media-types mt-subtype-prefix))
  :available-charsets rumeta/supported-char-sets
  :available-languages rumeta/supported-languages
  :allowed-methods [:post]
  :authorized? (fn [ctx]
                 (let [trial-expired-grace-period-in-days (nth more 0)]
                   (userresutils/authorized? ctx
                                             db-spec
                                             user-id
                                             auth-scheme
                                             auth-scheme-param-name
                                             trial-expired-grace-period-in-days)))
  :known-content-type? (rucore/known-content-type-predicate (meta/supported-media-types mt-subtype-prefix))
  :post! (fn [ctx]
           (handle-stripe-tokens-post! ctx
                                       db-spec
                                       base-url
                                       entity-uri-prefix
                                       (:uri (:request ctx))
                                       user-id
                                       (:plaintext-auth-token ctx)
                                       embedded-resources-fn
                                       links-fn
                                       stripe-api-key
                                       stripe-subscription-name
                                       err-notification-mustache-template
                                       err-subject
                                       err-from-email
                                       err-to-email))
  :handle-created (fn [ctx] (rucore/handle-resp ctx
                                                hdr-auth-token
                                                hdr-error-mask)))
