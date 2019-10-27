(ns riker-app.rest.resource.plan.plan-res
  (:require [clojure.tools.logging :as log]
            [liberator.core :refer [defresource]]
            [riker-app.rest.utils.macros :refer [defmulti-by-version]]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.meta :as rumeta]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.validation :as val]
            [riker-app.rest.meta :as meta]
            [riker-app.app.config :as config]))

(declare body-data-out-transform-fn)
(declare load-plan-fn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-plan-get
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   plan-uri
   resp-gen-fn
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email]
  (rucore/get-invoker ctx
                      db-spec
                      base-url
                      entity-uri-prefix
                      plan-uri
                      nil
                      nil
                      []
                      nil
                      body-data-out-transform-fn
                      load-plan-fn
                      nil
                      nil
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
(defmulti-by-version body-data-out-transform-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Load plan function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version load-plan-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resource definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource plan-res
  [db-spec
   mt-subtype-prefix
   hdr-error-mask
   base-url
   entity-uri-prefix
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email]
  :available-media-types (rucore/enumerate-media-types (meta/supported-media-types mt-subtype-prefix))
  :available-charsets rumeta/supported-char-sets
  :available-languages rumeta/supported-languages
  :allowed-methods [:get]
  :respond-with-entity? true
  :multiple-representations? false
  :handle-ok (fn [ctx]
               (handle-plan-get ctx
                                db-spec
                                base-url
                                entity-uri-prefix
                                (:uri (:request ctx))
                                #(rucore/handle-resp % nil hdr-error-mask)
                                err-notification-mustache-template
                                err-subject
                                err-from-email
                                err-to-email)))
