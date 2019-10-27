(ns riker-app.rest.resource.user.password-reset-res
  (:require [liberator.core :refer [defresource]]
            [liberator.representation :refer [ring-response]]
            [clj-time.core :as t]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [clojure.walk :refer [keywordize-keys]]
            [clostache.parser :refer [render-resource]]
            [riker-app.rest.utils.macros :refer [defmulti-by-version]]
            [riker-app.rest.user-meta :as meta]
            [clojure.tools.logging :as log]
            [pe-core-utils.core :as ucore]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.meta :as rumeta]
            [riker-app.core.user-dao :as usercore]
            [riker-app.rest.user-utils :as userresutils]
            [riker-app.core.user-validation :as userval]
            [riker-app.rest.resource.user.password-reset-util :as pwdresetutil]
            [riker-app.app.config :as config]))

(declare body-data-in-transform-fn)
(declare body-data-out-transform-fn)
(declare do-password-reset-fn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn handle-password-reset!
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   entity-uri
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email]
  (rucore/put-or-post-invoker ctx
                              :post-as-do
                              db-spec
                              base-url
                              entity-uri-prefix
                              entity-uri
                              nil ; embedded-resources-fn
                              nil ; links-fn
                              []  ; entids
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
                              do-password-reset-fn ; post-as-do-fn
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
;; do password reset function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version do-password-reset-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resources Definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource password-reset-res
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
  :allowed-methods [:post]
  :known-content-type? (rucore/known-content-type-predicate (meta/supported-media-types mt-subtype-prefix))
  :post! (fn [ctx] (handle-password-reset! ctx
                                           db-spec
                                           base-url
                                           entity-uri-prefix
                                           (:uri (:request ctx))
                                           err-notification-mustache-template
                                           err-subject
                                           err-from-email
                                           err-to-email))
  :handle-created (fn [ctx] (rucore/handle-resp ctx
                                                nil
                                                hdr-error-mask
                                                nil)))
