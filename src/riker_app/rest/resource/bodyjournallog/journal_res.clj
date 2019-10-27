(ns riker-app.rest.resource.bodyjournallog.journal-res
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

(declare save-body-journal-log-validator-fn)
(declare body-data-in-transform-fn)
(declare body-data-out-transform-fn)
(declare save-body-journal-log-fn)
(declare delete-body-journal-log-fn)
(declare load-body-journal-log-fn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-body-journal-log-put!
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   body-journal-log-uri
   user-id
   body-journal-log-id
   plaintext-auth-token
   embedded-resources-fn
   links-fn
   if-unmodified-since-hdr
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email]
  (rucore/put-or-post-invoker ctx
                              :put
                              db-spec
                              base-url
                              entity-uri-prefix
                              body-journal-log-uri
                              embedded-resources-fn
                              links-fn
                              [user-id body-journal-log-id]
                              plaintext-auth-token
                              save-body-journal-log-validator-fn
                              val/body-journal-log-any-issues
                              body-data-in-transform-fn
                              body-data-out-transform-fn
                              nil ; next-entity-id-fn
                              nil ; save-new-entity-fn
                              save-body-journal-log-fn
                              nil ; hdr-establish-session
                              nil ; make-session-fn
                              nil ; post-as-do-fn
                              if-unmodified-since-hdr
                              (fn [exc-and-params]
                                (usercore/send-email err-notification-mustache-template
                                                     exc-and-params
                                                     err-subject
                                                     err-from-email
                                                     err-to-email
                                                     config/r-domain
                                                     config/r-mailgun-api-key))
                              #(identity %)))

(defn handle-body-journal-log-delete!
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   body-journal-log-uri
   user-id
   body-journal-log-id
   plaintext-auth-token
   embedded-resources-fn
   links-fn
   if-unmodified-since-hdr
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email]
  (rucore/delete-invoker ctx
                         db-spec
                         base-url
                         entity-uri-prefix
                         body-journal-log-uri
                         embedded-resources-fn
                         links-fn
                         [user-id body-journal-log-id]
                         plaintext-auth-token
                         body-data-out-transform-fn
                         delete-body-journal-log-fn
                         nil ; delete-reason-hdr
                         if-unmodified-since-hdr
                         (fn [exc-and-params]
                           (usercore/send-email err-notification-mustache-template
                                                exc-and-params
                                                err-subject
                                                err-from-email
                                                err-to-email
                                                config/r-domain
                                                config/r-mailgun-api-key))))

(defn handle-body-journal-log-get
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   body-journal-log-uri
   user-id
   body-journal-log-id
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
                      body-journal-log-uri
                      embedded-resources-fn
                      links-fn
                      [user-id body-journal-log-id]
                      plaintext-auth-token
                      body-data-out-transform-fn
                      load-body-journal-log-fn
                      if-modified-since-hdr
                      :bodyjournallog/updated-at
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
;; Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version save-body-journal-log-validator-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version body-data-in-transform-fn meta/v001)
(defmulti-by-version body-data-out-transform-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Save new body-journal-log function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version save-body-journal-log-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Delete body-journal-log function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version delete-body-journal-log-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Load body-journal-log function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version load-body-journal-log-fn meta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resource definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource body-journal-log-res
  [db-spec
   mt-subtype-prefix
   hdr-auth-token
   hdr-error-mask
   auth-scheme
   auth-scheme-param-name
   base-url
   entity-uri-prefix
   user-id
   body-journal-log-id
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
  :allowed-methods [:put :delete :get]
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
  :can-put-to-missing? false
  :new? false
  :respond-with-entity? true
  :multiple-representations? false
  :put! (fn [ctx]
          (handle-body-journal-log-put! ctx
                                        db-spec
                                        base-url
                                        entity-uri-prefix
                                        (:uri (:request ctx))
                                        user-id
                                        body-journal-log-id
                                        (:plaintext-auth-token ctx)
                                        embedded-resources-fn
                                        links-fn
                                        if-unmodified-since-hdr
                                        err-notification-mustache-template
                                        err-subject
                                        err-from-email
                                        err-to-email))
  :delete! (fn [ctx]
             (handle-body-journal-log-delete! ctx
                                              db-spec
                                              base-url
                                              entity-uri-prefix
                                              (:uri (:request ctx))
                                              user-id
                                              body-journal-log-id
                                              (:plaintext-auth-token ctx)
                                              embedded-resources-fn
                                              links-fn
                                              if-unmodified-since-hdr
                                              err-notification-mustache-template
                                              err-subject
                                              err-from-email
                                              err-to-email))
  :handle-ok (fn [ctx]
               (if (= (get-in ctx [:request :request-method]) :get)
                 (handle-body-journal-log-get ctx
                                              db-spec
                                              base-url
                                              entity-uri-prefix
                                              (:uri (:request ctx))
                                              user-id
                                              body-journal-log-id
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
