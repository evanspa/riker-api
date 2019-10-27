(ns riker-app.rest.user-utils
  (:require [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.user-meta :as meta]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.user-dao :as usercore]
            [riker-app.app.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-plaintext-auth-token
  [ctx scheme scheme-param-name]
  (let [authorization (get-in ctx [:request :headers "authorization"])
        [_
         _
         auth-scheme-param-value] (rucore/parse-auth-header authorization
                                                            scheme
                                                            scheme-param-name)]
    auth-scheme-param-value))

(defn become-unauthenticated
  ([db-spec user-id plaintext-auth-token]
   (become-unauthenticated db-spec user-id plaintext-auth-token nil))
  ([db-spec user-id plaintext-auth-token reason]
   (do
     (usercore/invalidate-user-token db-spec user-id plaintext-auth-token reason)
     (throw (ex-info nil {:cause :became-unauthenticated})))))

(defn load-user-by-authorization-hdr-contents
  [ctx
   conn
   user-entid
   scheme
   scheme-param-name
   trial-expired-grace-period-in-days]
  (let [authorization (get-in ctx [:request :headers "authorization"])]
    (when-let [[auth-scheme
                auth-scheme-param-name
                auth-scheme-param-value] (rucore/parse-auth-header authorization
                                                                   scheme
                                                                   scheme-param-name)]
      (conj (usercore/authenticate-user-by-authtoken conn
                                                     user-entid
                                                     auth-scheme-param-value
                                                     trial-expired-grace-period-in-days)
            auth-scheme-param-value))))

(defn authorized?
  [ctx
   conn
   user-entid
   scheme
   scheme-param-name
   trial-expired-grace-period-in-days]
  (try
    (let [[found-user-entid
           found-user-ent
           plaintext-auth-token] (load-user-by-authorization-hdr-contents ctx
                                                                          conn
                                                                          user-entid
                                                                          scheme
                                                                          scheme-param-name
                                                                          trial-expired-grace-period-in-days)]
      (if (and (not (nil? found-user-ent))
               (= found-user-entid user-entid))
        [true {:user found-user-ent :plaintext-auth-token plaintext-auth-token}]
        false))
    (catch clojure.lang.ExceptionInfo e
      (let [cause (-> e ex-data :cause)]
        (cond
          (= cause :trial-and-grace-expired) false)))))

(defn make-allowed-fn
  []
  (fn [ctx]
    (let [user (:user ctx)]
      (or (and (not (nil? (:user/paid-enrollment-established-at user)))
               (nil? (:user/paid-enrollment-cancelled-at user))
               (nil? (:user/final-failed-payment-attempt-occurred-at user)))
          (and (nil? (:user/paid-enrollment-established-at user))
               (not (t/after? (t/now) (:user/trial-ends-at user))))))))

(defn make-user-subentity-url
  [base-url entity-uri-prefix user-id pathcomp-subent sub-id]
  (rucore/make-abs-link-href base-url
                             (str entity-uri-prefix
                                  meta/pathcomp-users
                                  "/"
                                  user-id
                                  "/"
                                  pathcomp-subent
                                  "/"
                                  sub-id)))

(defn user-out-transform
  [user]
  (let [hashed-pwd (:user/hashed-password user)]
    )
  (-> user
      (ucore/assoc-if-contains user :user/hashed-password :user/has-password #(not (nil? %)))
      (dissoc :user/password)
      (dissoc :user/hashed-password)
      (dissoc :user/app-store-receipt-data-base64)
      (ucore/transform-map-val :user/trial-almost-expired-notice-sent-at #(c/to-long %))
      (ucore/transform-map-val :user/validate-app-store-receipt-at #(c/to-long %))
      (ucore/transform-map-val :user/next-invoice-at #(c/to-long %))
      (ucore/transform-map-val :user/last-invoice-at #(c/to-long %))
      (ucore/transform-map-val :user/informed-of-maintenance-at #(c/to-long %))
      (ucore/transform-map-val :user/maintenance-starts-at #(c/to-long %))
      (ucore/transform-map-val :user/new-movements-added-at #(c/to-long %))
      (ucore/transform-map-val :user/created-at #(c/to-long %))
      (ucore/transform-map-val :user/deleted-at #(c/to-long %))
      (ucore/transform-map-val :user/updated-at #(c/to-long %))
      (ucore/transform-map-val :user/verified-at #(c/to-long %))
      (ucore/transform-map-val :user/paid-enrollment-established-at #(c/to-long %))
      (ucore/transform-map-val :user/paid-enrollment-cancelled-at #(c/to-long %))
      (ucore/transform-map-val :user/final-failed-payment-attempt-occurred-at #(c/to-long %))
      (ucore/transform-map-val :user/trial-ends-at #(c/to-long %))))
