(ns riker-app.rest.resource.movementvariant.movement-variant-res
  (:require [clojure.tools.logging :as log]
            [liberator.core :refer [defresource]]
            [riker-app.rest.utils.macros :refer [defmulti-by-version]]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.meta :as rumeta]
            [riker-app.rest.user-utils :as userresutils]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.validation :as val]
            [riker-app.rest.meta :as meta]
            [riker-app.app.config :as config]))

(declare body-data-in-transform-fn)
(declare body-data-out-transform-fn)
(declare load-movement-variant-fn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-movement-variant-get
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   movement-variant-uri
   user-id
   movement-variant-id
   plaintext-auth-token
   embedded-resources-fn
   links-fn
   if-modified-since-hdr
   resp-gen-fn
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email]
  (rucore/get-invoker ctx
                      db-spec
                      base-url
                      entity-uri-prefix
                      movement-variant-uri
                      embedded-resources-fn
                      links-fn
                      [user-id movement-variant-id]
                      plaintext-auth-token
                      body-data-out-transform-fn
                      load-movement-variant-fn
                      if-modified-since-hdr
                      :movementvariant/updated-at
                      resp-gen-fn
                      (fn [exc-and-params]
                        (usercore/send-email err-notification-mustache-template
                                             exc-and-params
                                             err-subject
                                             err-from-email
                                             err-to-email
                                             config/r-domain
                                             config/r-mailgun-api-key))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version body-data-in-transform-fn meta/v001)
(defmulti-by-version body-data-out-transform-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Load movement variant function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version load-movement-variant-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resource definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource movement-variant-res
  [db-spec
   mt-subtype-prefix
   hdr-auth-token
   hdr-error-mask
   auth-scheme
   auth-scheme-param-name
   base-url
   entity-uri-prefix
   user-id
   movement-variant-id
   embedded-resources-fn
   links-fn
   if-unmodified-since-hdr
   if-modified-since-hdr
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email
   &
   more]
  :available-media-types (rucore/enumerate-media-types (meta/supported-media-types mt-subtype-prefix))
  :available-charsets rumeta/supported-char-sets
  :available-languages rumeta/supported-languages
  :allowed-methods [:get]
  :authorized? (fn [ctx]
                 (let [trial-expired-grace-period-in-days (nth more 0)]
                   (userresutils/authorized? ctx
                                             db-spec
                                             user-id
                                             auth-scheme
                                             auth-scheme-param-name
                                             trial-expired-grace-period-in-days)))
  :allowed? (userresutils/make-allowed-fn)
  :known-content-type? (rucore/known-content-type-predicate (meta/supported-media-types mt-subtype-prefix))
  :new? false
  :respond-with-entity? true
  :multiple-representations? false
  :handle-ok (fn [ctx]
               (if (= (get-in ctx [:request :request-method]) :get)
                 (handle-movement-variant-get ctx
                                              db-spec
                                              base-url
                                              entity-uri-prefix
                                              (:uri (:request ctx))
                                              user-id
                                              movement-variant-id
                                              (:plaintext-auth-token ctx)
                                              embedded-resources-fn
                                              links-fn
                                              if-modified-since-hdr
                                              #(rucore/handle-resp % hdr-auth-token hdr-error-mask)
                                              err-notification-mustache-template
                                              err-subject
                                              err-from-email
                                              err-to-email)
                 (rucore/handle-resp ctx hdr-auth-token hdr-error-mask))))
