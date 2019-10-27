(ns riker-app.rest.resource.facebook.facebook-res
  (:require [clojure.tools.logging :as log]
            [liberator.core :refer [defresource]]
            [liberator.representation :refer [ring-response]]
            [clojure.walk :refer [postwalk keywordize-keys]]
            [clojure.string :refer [split]]
            [clojure.data.json :as json]
            [clojure.java.jdbc :as j]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.meta :as rumeta]
            [riker-app.rest.user-utils :as userresutils]
            [riker-app.core.user-dao :as usercore]
            [riker-app.rest.user-meta :as usermeta]
            [riker-app.utils :as rutils]
            [riker-app.rest.user-utils :as userresutils]
            [riker-app.core.ddl :as ddl]
            [riker-app.rest.meta :as meta]
            [riker-app.app.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- handle-continue-with-facebook-res-post!
  [ctx
   db-spec
   base-url
   entity-uri-prefix
   embedded-resources-fn
   user-links-fn
   trial-period-in-days
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email]
  (let [body (get-in ctx [:request :body])
        body-data (keywordize-keys (json/read-str (slurp body :encoding "UTF-8")))
        fb-user-id (:user/facebook-user-id body-data)
        email (:user/email body-data)
        existing-user-fn (fn [user-result]
                           (let [[user-id user] user-result]
                             (usercore/invalidate-user-tokens db-spec user-id usercore/invalrsn-continue-with-facebook)
                             (conj (usercore/save-user db-spec
                                                       user-id
                                                       (merge user {:user/hashed-password nil
                                                                    :user/email email
                                                                    :user/verified-at (c/to-long (t/now))
                                                                    :user/facebook-user-id fb-user-id}))
                                   200)))
        [user-id user status-code] (if-let [user-result (usercore/load-user-by-facebook-user-id db-spec fb-user-id)]
                                     (existing-user-fn user-result)
                                     (if-let [user-result (usercore/load-user-by-email db-spec email)]
                                       (existing-user-fn user-result)
                                       (let [new-user-id (usercore/next-user-account-id db-spec)]
                                         (conj (usercore/save-new-user db-spec
                                                                       new-user-id
                                                                       {:user/facebook-user-id fb-user-id
                                                                        :user/email email
                                                                        :user/verified-at (c/to-long (t/now))
                                                                        :user/max-allowed-set-import config/max-allowed-set-import
                                                                        :user/max-allowed-bml-import config/max-allowed-bml-import
                                                                        :user/is-payment-past-due false
                                                                        :user/trial-ends-at (c/to-long (t/plus (t/now)
                                                                                                               (t/days (inc trial-period-in-days))))})
                                               201))))]
    (let [{{:keys [media-type lang charset]} :representation} ctx
          charset (if (nil? charset) "UTF-8" charset) ; from web, accept-charset will not be present, so this check is needed
          parsed-accept-mt (rucore/parse-media-type media-type)
          version (:version parsed-accept-mt)
          accept-format-ind (:format-ind parsed-accept-mt)
          new-token-id (usercore/next-auth-token-id db-spec)
          auth-token (usercore/create-and-save-auth-token db-spec user-id new-token-id)
          transformed-user (-> user
                               (userresutils/user-out-transform)
                               (assoc :_links (user-links-fn version
                                                             base-url
                                                             entity-uri-prefix
                                                             "users/"
                                                             user-id))
                               (assoc :_embedded (embedded-resources-fn version
                                                                        base-url
                                                                        entity-uri-prefix
                                                                        (:uri (:request ctx))
                                                                        db-spec
                                                                        accept-format-ind
                                                                        user-id)))]
      {:status status-code
       :auth-token auth-token
       :location (rucore/make-abs-link-href base-url
                                            (str entity-uri-prefix
                                                 "users/"
                                                 user-id))
       :entity (rucore/write-res transformed-user accept-format-ind (get rumeta/char-sets charset))})))

(defn- fb-user-id-from-signed-request
  [ctx]
  (when-let [params (get-in ctx [:request :params])]
    (when-let [signed-request (:signed_request params)]
      (when-let [payload-raw (second (split signed-request #"\."))]
        (let [payload (keywordize-keys (json/read-str (rutils/decode-base64Url payload-raw)))]
          (:user_id payload))))))

(defn- handle-facebook-deauthorize-or-delete-request-post!
  [ctx
   db-spec
   log-msg-prefix
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email]
  (when-let [fb-user-id (fb-user-id-from-signed-request ctx)]
    (when-let [[user-id user] (usercore/load-user-by-facebook-user-id db-spec fb-user-id)]
      (log/info (format "%s Facebook user-id: [%s], Riker user-id: [%s]" log-msg-prefix fb-user-id, user-id))
      (usercore/invalidate-user-tokens db-spec
                                       user-id
                                       usercore/invalrsn-facebook-deauth))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Resource definitions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defresource continue-with-facebook-res
  [db-spec
   trial-period-in-days
   mt-subtype-prefix
   hdr-auth-token
   hdr-error-mask
   base-url
   entity-uri-prefix
   embedded-resources-fn-maker
   user-links-fn
   err-notification-mustache-template
   err-subject
   err-from-email
   err-to-email]
  :available-media-types (rucore/enumerate-media-types (usermeta/supported-media-types mt-subtype-prefix))
  :available-charsets rumeta/supported-char-sets
  :available-languages rumeta/supported-languages
  :allowed-methods [:post]
  :known-content-type? (rucore/known-content-type-predicate (usermeta/supported-media-types mt-subtype-prefix))
  :new? false
  :respond-with-entity? true
  :multiple-representations? false
  :post! (fn [ctx]
           (handle-continue-with-facebook-res-post! ctx
                                                    db-spec
                                                    base-url
                                                    entity-uri-prefix
                                                    (embedded-resources-fn-maker ctx)
                                                    user-links-fn
                                                    trial-period-in-days
                                                    err-notification-mustache-template
                                                    err-subject
                                                    err-from-email
                                                    err-to-email))
  :handle-ok (fn [ctx] (rucore/handle-resp ctx hdr-auth-token hdr-error-mask)))

(defresource facebook-deauthorize-res
  [db-spec
   mt-subtype-prefix
   base-url
   entity-uri-prefix
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
  :post! (fn [ctx]
           (handle-facebook-deauthorize-or-delete-request-post! ctx
                                                                db-spec
                                                                "facebook deauth"
                                                                err-notification-mustache-template
                                                                err-subject
                                                                err-from-email
                                                                err-to-email)))

(defresource facebook-data-delete-request-res
  [db-spec
   mt-subtype-prefixp
   base-url
   entity-uri-prefix
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
  :post! (fn [ctx]
           (handle-facebook-deauthorize-or-delete-request-post! ctx
                                                                db-spec
                                                                "facebook data delete request"
                                                                err-notification-mustache-template
                                                                err-subject
                                                                err-from-email
                                                                err-to-email)))
