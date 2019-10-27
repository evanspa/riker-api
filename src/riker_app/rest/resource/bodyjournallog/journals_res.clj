(ns riker-app.rest.resource.bodyjournallog.journals-res
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

(declare process-body-journal-logs-post!)
(declare new-body-journal-log-validator-fn)
(declare body-data-in-transform-fn)
(declare body-data-out-transform-fn)
(declare save-new-body-journal-log-fn)
(declare next-body-journal-log-id-fn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-body-journal-logs-post!
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   body-journal-logs-uri
   user-id
   plaintext-auth-token
   embedded-resources-fn
   links-fn
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email]
  (rucore/put-or-post-invoker ctx
                              :post-as-create
                              db-spec
                              base-url
                              entity-uri-prefix
                              body-journal-logs-uri
                              embedded-resources-fn
                              links-fn
                              [user-id]
                              plaintext-auth-token
                              new-body-journal-log-validator-fn
                              rval/body-journal-log-any-issues
                              body-data-in-transform-fn
                              body-data-out-transform-fn
                              next-body-journal-log-id-fn
                              save-new-body-journal-log-fn
                              nil ; save-entity-fn
                              nil ; hdr-establish-session
                              nil ; make-session-fn
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
                              #(identity %)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version new-body-journal-log-validator-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version body-data-in-transform-fn meta/v001)
(defmulti-by-version body-data-out-transform-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Next fuel purchase log id function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version next-body-journal-log-id-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Save new body-journal-log function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version save-new-body-journal-log-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resource definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource body-journal-logs-res
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
  :allowed? (userresutils/make-allowed-fn)
  :known-content-type? (rucore/known-content-type-predicate (meta/supported-media-types mt-subtype-prefix))
  :post! (fn [ctx] (handle-body-journal-logs-post! ctx
                                                   db-spec
                                                   base-url
                                                   entity-uri-prefix
                                                   (:uri (:request ctx))
                                                   user-id
                                                   (:plaintext-auth-token ctx)
                                                   embedded-resources-fn
                                                   links-fn
                                                   err-notification-mustache-template
                                                   err-subject
                                                   err-from-email
                                                   err-to-email))
  :handle-created (fn [ctx] (rucore/handle-resp ctx
                                                hdr-auth-token
                                                hdr-error-mask)))
