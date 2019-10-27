(ns riker-app.rest.resource.bodyjournallog.journals-import-res
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

(declare do-body-journal-logs-import-fn)
(declare body-data-in-transform-fn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-body-journal-logs-import-post!
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   entity-uri
   user-id
   plaintext-auth-token
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
                              nil ; embedded-resources-fn
                              nil ; links-fn
                              [user-id]
                              plaintext-auth-token
                              nil ; user-validator-fn
                              nil ; any-issues-bit
                              body-data-in-transform-fn
                              nil ; body-data-out-transform-fn
                              nil ; next-entity-id-fn
                              nil ; save-new-entity-fn
                              nil ; save-entity-fn
                              nil ; hdr-establish-session
                              nil ; make-session-fn
                              do-body-journal-logs-import-fn
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
;; body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version body-data-in-transform-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Body journal log importer function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version do-body-journal-logs-import-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resource definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource body-journal-logs-import-res
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
   trial-expired-grace-period-in-days]
  :available-media-types (rucore/enumerate-media-types (meta/supported-media-types mt-subtype-prefix))
  :available-charsets rumeta/supported-char-sets
  :available-languages rumeta/supported-languages
  :allowed-methods [:post]
  :authorized? (fn [ctx]
                 (userresutils/authorized? ctx
                                           db-spec
                                           user-id
                                           auth-scheme
                                           auth-scheme-param-name
                                           trial-expired-grace-period-in-days))
  :allowed? (fn [ctx]
              (let [user (:user ctx)]
                (and ((userresutils/make-allowed-fn) ctx)
                     (not (nil? (:user/verified-at user))))))
  :known-content-type? (fn [ctx]
                         (= (get-in ctx [:request :headers "content-type"]) "text/csv"))
  :post! (fn [ctx] (handle-body-journal-logs-import-post! ctx
                                                          db-spec
                                                          base-url
                                                          entity-uri-prefix
                                                          (:uri (:request ctx))
                                                          user-id
                                                          (:plaintext-auth-token ctx)
                                                          err-notification-mustache-template
                                                          err-subject
                                                          err-from-email
                                                          err-to-email
                                                          trial-expired-grace-period-in-days))
  :handle-created (fn [ctx] (rucore/handle-resp ctx
                                                hdr-auth-token
                                                hdr-error-mask)))
