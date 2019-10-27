(ns riker-app.rest.utils.changelog.resource-support
  "Core components for exposing the server-side processing of the
  PEAppTransaction Logging Framework as a REST API."
  (:require [clj-time.core :as t]
            [liberator.core :refer [defresource]]
            [riker-app.rest.utils.changelog.meta :as clmeta]
            [clojure.tools.logging :as log]
            [clojure.walk :refer [keywordize-keys]]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.macros :refer [defmulti-by-version]]
            [riker-app.rest.utils.meta :as rumeta]))

(declare body-data-out-transform-fn)
(declare save-new-entity-fn)
(declare load-changelog-fn)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-changelog-get
  "Liberator handler function for fetching a changelog."
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   changelog-uri
   user-id
   plaintext-auth-token
   embedded-resources-fn
   links-fn
   if-modified-since-hdr
   resp-gen-fn
   tables-and-updated-at-cols
   err-notification-fn]
  (rucore/get-invoker ctx
                      db-spec
                      base-url
                      entity-uri-prefix
                      changelog-uri
                      embedded-resources-fn
                      links-fn
                      [user-id]
                      plaintext-auth-token
                      body-data-out-transform-fn
                      (fn [ctx
                           version
                           db-spec
                           user-id
                           plaintext-auth-token
                           if-modified-since]
                        (load-changelog-fn ctx
                                           version
                                           db-spec
                                           user-id
                                           plaintext-auth-token
                                           if-modified-since
                                           tables-and-updated-at-cols))
                      if-modified-since-hdr
                      :changelog/updated-at
                      resp-gen-fn
                      err-notification-fn))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fetch-changelog function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version load-changelog-fn clmeta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti-by-version body-data-out-transform-fn clmeta/v001)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; resource
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource changelog-res
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
   if-modified-since-hdr
   authorized-fn
   get-plaintext-auth-token-fn
   tables-and-updated-at-cols
   err-notification-fn]
  :available-media-types (rucore/enumerate-media-types (clmeta/supported-media-types mt-subtype-prefix))
  :available-charsets rumeta/supported-char-sets
  :available-languages rumeta/supported-languages
  :allowed-methods [:get]
  :authorized? authorized-fn
  :respond-with-entity? true
  :multiple-representations? false
  :handle-ok (fn [ctx]
               (handle-changelog-get ctx
                                     db-spec
                                     base-url
                                     entity-uri-prefix
                                     (:uri (:request ctx))
                                     user-id
                                     (get-plaintext-auth-token-fn ctx
                                                                  auth-scheme
                                                                  auth-scheme-param-name)
                                     embedded-resources-fn
                                     links-fn
                                     if-modified-since-hdr
                                     #(rucore/handle-resp % hdr-auth-token hdr-error-mask)
                                     tables-and-updated-at-cols
                                     err-notification-fn)))
