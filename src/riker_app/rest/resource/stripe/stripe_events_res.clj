(ns riker-app.rest.resource.stripe.stripe-events-res
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

(declare process-stripe-events-post!)
(declare new-stripe-event-validator-fn)
(declare body-data-in-transform-fn)
(declare body-data-out-transform-fn)
(declare process-new-stripe-event-fn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-stripe-events-post!
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   stripe-events-uri
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email
   invoice-payment-failed-email-subject
   invoice-payment-failed-email-mustache-template]
  (rucore/put-or-post-invoker ctx
                              :post-as-do
                              db-spec
                              base-url
                              entity-uri-prefix
                              stripe-events-uri
                              nil ; embedded-resources-fn
                              nil ; links-fn
                              []
                              nil ; plaintext-auth-token
                              nil ; validator-fn
                              nil ; any-issues-bit
                              body-data-in-transform-fn
                              nil ; body-data-out-transform-fn
                              nil ; next-entity-id-fn
                              nil ; save-new-entity-fn
                              nil ; save-entity-fn
                              nil ; hdr-establish-session
                              nil ; make-session-fn
                              (fn [ctx
                                   version
                                   db-spec
                                   base-url
                                   entity-uri-prefix
                                   stripe-events-uri
                                   plaintext-auth-token
                                   stripe-event
                                   merge-embedded-fn
                                   merge-links-fn]
                                (process-new-stripe-event-fn ctx
                                                             version
                                                             db-spec
                                                             base-url
                                                             entity-uri-prefix
                                                             stripe-events-uri
                                                             plaintext-auth-token
                                                             stripe-event
                                                             merge-embedded-fn
                                                             merge-links-fn
                                                             invoice-payment-failed-email-subject
                                                             invoice-payment-failed-email-mustache-template))
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
(defmulti-by-version new-stripe-event-validator-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version body-data-in-transform-fn meta/v001)
(defmulti-by-version body-data-out-transform-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Save new stripe-event function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version process-new-stripe-event-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resource definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource stripe-events-res
  [db-spec
   mt-subtype-prefix
   hdr-auth-token
   hdr-error-mask
   base-url
   entity-uri-prefix
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email
   invoice-payment-failed-email-subject
   invoice-payment-failed-email-mustache-template]
  :available-media-types ["application/json"]
  :available-charsets rumeta/supported-char-sets
  :available-languages rumeta/supported-languages
  :allowed-methods [:post]
  :known-content-type? (fn [ctx] true)
  :post! (fn [ctx]
           (handle-stripe-events-post! ctx
                                       db-spec
                                       base-url
                                       entity-uri-prefix
                                       (:uri (:request ctx))
                                       err-notification-mustache-template
                                       err-subject
                                       err-from-email
                                       err-to-email
                                       invoice-payment-failed-email-subject
                                       invoice-payment-failed-email-mustache-template))
  :handle-created (fn [ctx] (rucore/handle-resp ctx
                                                hdr-auth-token
                                                hdr-error-mask)))
