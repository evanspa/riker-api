(ns riker-app.rest.resource.iad.iad-attribution-res
  (:require [clojure.tools.logging :as log]
            [liberator.core :refer [defresource]]
            [liberator.representation :refer [ring-response]]
            [clojure.walk :refer [postwalk keywordize-keys]]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as j]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.meta :as rumeta]
            [riker-app.rest.user-utils :as userresutils]
            [riker-app.core.user-dao :as usercore]
            [riker-app.utils :as rutils]
            [riker-app.core.ddl :as ddl]
            [riker-app.rest.meta :as meta]
            [riker-app.app.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn handle-iad-attribution-post!
  [ctx
   db-spec
   user-id
   attribution-id
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email]
  (let [body (get-in ctx [:request :body])
        body-data (keywordize-keys (json/read-str (slurp body :encoding "UTF-8")))
        body-data-user-id (:user-id body-data)]
    (if (= user-id body-data-user-id)
      (try
        (let [result (j/update! db-spec
                                ddl/tbl-apple-search-ads-attribution
                                {:user_id user-id}
                                ["id = ?" attribution-id])]
          (when (or (not (and (not (nil? result)) (= (count result) 1)))
                    (not (= (first result) 1)))
            (log/error (str "Error attempting to associate user ID to iad attribution. Request body: " body-data ", sql result: " result))
            {:status 500}))
        (catch Exception e
          (rutils/log-e e (str "Exception associating user ID to iad attribution.  Request body: " body-data))
          (usercore/send-email err-notification-mustache-template
                               e
                               err-subject
                               err-from-email
                               err-to-email
                               config/r-domain
                               config/r-mailgun-api-key)
          {:status 500}))
      (do
        (log/error "The user id on the URL: " user-id " does not match the user id in the body: " body-data-user-id)
        {:status 400}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resource definition
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource iad-attribution-res
  [db-spec
   mt-subtype-prefix
   hdr-auth-token
   hdr-error-mask
   auth-scheme
   auth-scheme-param-name
   base-url
   entity-uri-prefix
   user-id
   attribution-id
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email]
  :available-media-types ["application/json"]
  :available-charsets rumeta/supported-char-sets
  :available-languages rumeta/supported-languages
  :allowed-methods [:post]
  :known-content-type? (fn [ctx] true)
  :new? false
  :authorized? (fn [ctx]
                 (userresutils/authorized? ctx
                                           db-spec
                                           user-id
                                           auth-scheme
                                           auth-scheme-param-name
                                           nil))
  :post! (fn [ctx]
           (handle-iad-attribution-post! ctx
                                         db-spec
                                         user-id
                                         attribution-id
                                         err-notification-mustache-template
                                         err-subject
                                         err-from-email
                                         err-to-email)))
