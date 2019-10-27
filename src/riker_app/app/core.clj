(ns riker-app.app.core
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [liberator.core :refer [resource]]
            [environ.core :refer [env]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [ring.util.codec :refer [url-decode]]
            [compojure.core :refer [ANY GET]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.handler :as handler]
            [pe-core-utils.core :as ucore]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.meta :as rumeta]
            [riker-app.rest.utils.changelog.meta :as clmeta]
            [riker-app.rest.utils.changelog.resource-support :as clres]
            [riker-app.rest.utils.changelog.version.resource-support-v001]
            [riker-app.core.user-ddl :as userddl]
            [riker-app.core.user-dao :as usercore]
            [riker-app.rest.user-utils :as userresutils]
            [riker-app.rest.user-meta :as usermeta]
            [riker-app.rest.resource.user.users-res :as usersres]
            [riker-app.rest.resource.user.version.users-res-v001]
            [riker-app.rest.resource.user.user-res :as userres]
            [riker-app.rest.resource.user.version.user-res-v001]
            [riker-app.rest.resource.user.login-res :as loginres]
            [riker-app.rest.resource.user.version.login-res-v001]
            [riker-app.rest.resource.user.logout-res :as logoutres]
            [riker-app.rest.resource.user.version.logout-res-v001]
            [riker-app.rest.resource.user.logout-all-other-res :as logoutallotherres]
            [riker-app.rest.resource.user.version.logout-all-other-res-v001]
            [riker-app.rest.resource.user.send-verification-email-res :as sendveriemailres]
            [riker-app.rest.resource.user.version.send-verification-email-res-v001]
            [riker-app.rest.resource.user.account-verification-res :as verificationres]
            [riker-app.rest.resource.user.send-password-reset-email-res :as sendpwdresetemailres]
            [riker-app.rest.resource.user.version.send-password-reset-email-res-v001]
            [riker-app.rest.resource.user.prepare-password-reset-res :as preparepwdresetres]
            [riker-app.rest.resource.user.password-reset-res :as pwdresetres]
            [riker-app.rest.resource.user.version.password-reset-res-v001]

            [riker-app.core.ddl :as rddl]
            [riker-app.core.dao :as dao]
            [riker-app.rest.meta :as rmeta]
            [riker-app.app.config :as config]

            [riker-app.rest.resource.resource-utils :as resutils]

            [riker-app.rest.resource.plan.plan-res :as planres]
            [riker-app.rest.resource.plan.plan-utils :as planresutils]
            [riker-app.rest.resource.plan.version.plan-res-v001]

            [riker-app.rest.resource.usersettings.usersettings-res :as usersettingsres]
            [riker-app.rest.resource.usersettings.usersettings-utils :as usersettingsresutils]
            [riker-app.rest.resource.usersettings.version.usersettings-res-v001]

            [riker-app.rest.resource.stripe.stripe-tokens-res :as stripetokensres]
            [riker-app.rest.resource.stripe.version.stripe-tokens-res-v001]
            [riker-app.rest.resource.stripe.stripe-events-res :as stripeeventsres]
            [riker-app.rest.resource.stripe.version.stripe-events-res-v001]

            [riker-app.rest.resource.iad.iad-attributions-res :as iadattrsres]
            [riker-app.rest.resource.iad.version.iad-attributions-res-v001]
            [riker-app.rest.resource.iad.iad-attribution-res :as iadattrres]

            [riker-app.rest.resource.facebook.facebook-res :as facebookres]

            [riker-app.rest.resource.movement.movement-res :as movementres]
            [riker-app.rest.resource.movement.version.movement-res-v001]

            [riker-app.rest.resource.movementvariant.movement-variant-res :as movementvariantres]
            [riker-app.rest.resource.movementvariant.version.movement-variant-res-v001]

            [riker-app.rest.resource.set.sets-res :as setsres]
            [riker-app.rest.resource.set.version.sets-res-v001]
            [riker-app.rest.resource.set.set-res :as setres]
            [riker-app.rest.resource.set.version.set-res-v001]
            [riker-app.rest.resource.set.sets-import-res :as setsimportres]
            [riker-app.rest.resource.set.version.sets-import-res-v001]
            [riker-app.rest.resource.set.set-utils :as setresutils]

            [riker-app.rest.resource.bodyjournallog.journals-res :as djsres]
            [riker-app.rest.resource.bodyjournallog.version.journals-res-v001]
            [riker-app.rest.resource.bodyjournallog.journal-res :as djres]
            [riker-app.rest.resource.bodyjournallog.version.journal-res-v001]
            [riker-app.rest.resource.bodyjournallog.journals-import-res :as djsimportres]
            [riker-app.rest.resource.bodyjournallog.version.journals-import-res-v001]
            [riker-app.rest.resource.bodyjournallog.journal-utils :as djresutils]

            [riker-app.rest.resource.soreness.sorenesses-res :as sorenessesres]
            [riker-app.rest.resource.soreness.version.sorenesses-res-v001]
            [riker-app.rest.resource.soreness.soreness-res :as sorenessres]
            [riker-app.rest.resource.soreness.soreness-utils :as sorenessresutils]
            [riker-app.rest.resource.soreness.version.soreness-res-v001]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; URL templates for routing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def root-uri-template config/r-entity-uri-prefix)

(def build-version-uri-template
  (format "%s%s"
          config/r-entity-uri-prefix
          "build-version"))

(def users-uri-template
  (format "%s%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users))

(def verification-uri-template
  (format "%s%s/:email/%s/:verification-token"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          usermeta/pathcomp-verification))

(def login-uri-template
  (format "%s%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-login))

(def light-login-uri-template
  (format "%s%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-light-login))

(def logout-uri-template
  (format "%s%s/:user-id/%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          usermeta/pathcomp-logout))

(def logout-all-other-uri-template
  (format "%s%s/:user-id/%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          usermeta/pathcomp-logout-all-other))

(def send-verification-email-uri-template
  (format "%s%s/:user-id/%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          usermeta/pathcomp-send-verification-email))

(def plan-uri-template
  (format "%s%s"
          config/r-entity-uri-prefix
          rmeta/pathcomp-plan))

(def send-password-reset-email-uri-template
  (format "%s%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-send-password-reset-email))

(def prepare-password-reset-uri-template
  (format "%s%s/:email/%s/:password-reset-token"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          usermeta/pathcomp-prepare-password-reset))

(def password-reset-uri-template
  (format "%s%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-password-reset))

(def user-uri-template
  (format "%s%s/:user-id"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users))

(def changelog-uri-template
  (format "%s%s/:user-id/%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          clmeta/pathcomp-changelog))

(def refdata-changelog-uri-template
  (format "%s%s/:user-id/%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-refdata-changelog))

(def userdata-changelog-uri-template
  (format "%s%s/:user-id/%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-userdata-changelog))

(def usersettings-uri-template
  (format "%s%s/:user-id/%s/:usersettings-id"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-usersettings))

(def sets-file-import-uri-template
  (format "%s%s/:user-id/%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-sets-file-import))

(def sets-uri-template
  (format "%s%s/:user-id/%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-sets))

(def set-uri-template
  (format "%s%s/:user-id/%s/:set-id"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-sets))

(def movement-uri-template
  (format "%s%s/:user-id/%s/:movement-id"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-movements))

(def movement-variant-uri-template
  (format "%s%s/:user-id/%s/:movement-variant-id"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-movement-variants))

(def stripe-tokens-uri-template
  (format "%s%s/:user-id/%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-stripe-tokens))

(def stripe-events-uri-template
  (format "%s%s/stripe-events"
          config/r-entity-uri-prefix
          config/r-stripe-webhook-secret-path-comp))

(def apple-search-ads-attributions-uri-template
  (format "%s%s/iad-attributions"
          config/r-entity-uri-prefix
          config/r-apple-search-ads-attribution-upload-secret-path-comp))

(def apple-search-ads-attribution-uri-template
  (format "%s%s/%s/:user-id/iad-attributions/:attribution-id"
          config/r-entity-uri-prefix
          config/r-apple-search-ads-attribution-upload-secret-path-comp
          usermeta/pathcomp-users))

(def continue-with-facebook-uri-template
  (format "%s%s/continue-with-facebook"
          config/r-entity-uri-prefix
          config/r-facebook-callback-secret-path-comp))

(def facebook-deauthorize-uri-template
  (format "%s%s/facebook-deauthorize"
          config/r-entity-uri-prefix
          config/r-facebook-callback-secret-path-comp))

(def facebook-data-delete-request-uri-template
  (format "%s%s/facebook-data-delete-request"
          config/r-entity-uri-prefix
          config/r-facebook-callback-secret-path-comp))

(def sorenesses-uri-template
  (format "%s%s/:user-id/%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-sorenesses))

(def soreness-uri-template
  (format "%s%s/:user-id/%s/:soreness-id"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-sorenesses))

(def body-journal-logs-uri-template
  (format "%s%s/:user-id/%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-body-journal-logs))

(def body-journal-logs-file-import-uri-template
  (format "%s%s/:user-id/%s"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-body-journal-logs-file-import))

(def body-journal-log-uri-template
  (format "%s%s/:user-id/%s/:body-journal-log-id"
          config/r-entity-uri-prefix
          usermeta/pathcomp-users
          rmeta/pathcomp-body-journal-logs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Entity Keys
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; ref data
(def key-body-segments     :body-segments)
(def key-muscle-groups     :muscle-groups)
(def key-muscles           :muscles)
(def key-muscle-aliases    :muscle-aliases)
(def key-movements         :movements)
(def key-movement-variants :movement-variants)
(def key-movement-aliases  :movement-aliases)
(def key-origination-devices :origination-devices)

; user data
(def key-usersettings      :usersettings)
(def key-sets              :sets)
(def key-soreness-logs     :soreness-logs)
(def key-body-journal-logs :body-journal-logs)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Link and Embedded-resources functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn user-links-fn
  [version
   base-url
   entity-uri-prefix
   entity-uri
   user-id]
  (let [link-fn (fn [rel mt-subtype-fn path-comp]
                  (rucore/make-abs-link version
                                        rel
                                        (mt-subtype-fn config/r-mt-subtype-prefix)
                                        base-url
                                        (str config/r-entity-uri-prefix
                                             usermeta/pathcomp-users
                                             "/"
                                             user-id
                                             "/"
                                             path-comp)))]
    (-> {}
        (rucore/assoc-link (rucore/make-abs-link version
                                                 rmeta/r-plan-relation
                                                 (rmeta/mt-subtype-plan config/r-mt-subtype-prefix)
                                                 base-url
                                                 (str config/r-entity-uri-prefix
                                                      rmeta/pathcomp-plan)))
        (rucore/assoc-link (link-fn usermeta/logout-relation
                                    usermeta/mt-subtype-user
                                    usermeta/pathcomp-logout))
        (rucore/assoc-link (link-fn usermeta/logout-all-other-relation
                                    usermeta/mt-subtype-user
                                    usermeta/pathcomp-logout-all-other))
        (rucore/assoc-link (link-fn usermeta/send-verification-email-relation
                                    usermeta/mt-subtype-user
                                    usermeta/pathcomp-send-verification-email))
        (rucore/assoc-link (link-fn usermeta/send-password-reset-email-relation
                                    usermeta/mt-subtype-user
                                    usermeta/pathcomp-send-password-reset-email))
        (rucore/assoc-link [rmeta/r-sets-file-import-relation {:href (rucore/make-abs-link-href base-url
                                                                                                (str config/r-entity-uri-prefix
                                                                                                     usermeta/pathcomp-users
                                                                                                     "/"
                                                                                                     user-id
                                                                                                     "/"
                                                                                                     rmeta/pathcomp-sets-file-import))
                                                               :type "text/csv"}])
        (rucore/assoc-link [rmeta/r-body-journal-logs-file-import-relation {:href (rucore/make-abs-link-href base-url
                                                                                                             (str config/r-entity-uri-prefix
                                                                                                                  usermeta/pathcomp-users
                                                                                                                  "/"
                                                                                                                  user-id
                                                                                                                  "/"
                                                                                                                  rmeta/pathcomp-body-journal-logs-file-import))
                                                                            :type "text/csv"}])
        (rucore/assoc-link (link-fn rmeta/r-sets-relation
                                    rmeta/mt-subtype-set
                                    rmeta/pathcomp-sets))
        (rucore/assoc-link (link-fn rmeta/r-stripe-tokens-relation
                                    rmeta/mt-subtype-stripe-token
                                    rmeta/pathcomp-stripe-tokens))
        (rucore/assoc-link (link-fn rmeta/r-body-journal-logs-relation
                                    rmeta/mt-subtype-body-journal-log
                                    rmeta/pathcomp-body-journal-logs))
        (rucore/assoc-link (link-fn rmeta/r-sorenesses-relation
                                    rmeta/mt-subtype-soreness
                                    rmeta/pathcomp-sorenesses))
        (rucore/assoc-link (link-fn clmeta/changelog-relation
                                    clmeta/mt-subtype-changelog
                                    clmeta/pathcomp-changelog))
        (rucore/assoc-link (link-fn rmeta/r-refdata-changelog-relation
                                    rmeta/mt-subtype-refdata-changelog
                                    rmeta/pathcomp-refdata-changelog))
        (rucore/assoc-link (link-fn rmeta/r-userdata-changelog-relation
                                    rmeta/mt-subtype-userdata-changelog
                                    rmeta/pathcomp-userdata-changelog)))))

(defn make-user-subentity-url
  [user-id pathcomp-subent sub-id]
  (rucore/make-abs-link-href config/r-base-url
                             (str config/r-entity-uri-prefix
                                  usermeta/pathcomp-users
                                  "/"
                                  user-id
                                  "/"
                                  pathcomp-subent
                                  "/"
                                  sub-id)))

(defn make-subentity-url
  [pathcomp-subent sub-id]
  (rucore/make-abs-link-href config/r-base-url
                             (str config/r-entity-uri-prefix
                                  pathcomp-subent
                                  "/"
                                  sub-id)))

(defn embedded-user-subentity
  [user-id
   sub-id
   mt-subtype-fn
   pathcomp-subent
   payload-transform-fn
   conn
   entity
   version
   format-ind]
  {:media-type (rucore/media-type rumeta/mt-type
                                  (mt-subtype-fn config/r-mt-subtype-prefix)
                                  version
                                  format-ind)
   :location (make-user-subentity-url user-id pathcomp-subent sub-id)
   :payload (-> entity
                (payload-transform-fn))})

(defn embedded-refdata-subentity
  [sub-id
   mt-subtype-fn
   pathcomp-subent
   payload-transform-fn
   conn
   entity
   version
   format-ind]
  {:media-type (rucore/media-type rumeta/mt-type
                                  (mt-subtype-fn config/r-mt-subtype-prefix)
                                  version
                                  format-ind)
   :location (make-subentity-url pathcomp-subent sub-id)
   :payload (-> entity
                (payload-transform-fn))})

(defn- make-embedded-user-entity-fn
  [mt-subtype-fn
   pathcomp
   out-transform-fn]
  (fn [user-id
       entity-id
       conn
       entity
       version
       format-ind]
    (embedded-user-subentity user-id
                             entity-id
                             mt-subtype-fn
                             pathcomp
                             #(out-transform-fn % config/r-base-url config/r-entity-uri-prefix)
                             conn
                             entity
                             version
                             format-ind)))

(defn- make-embedded-refdata-entity-fn
  [mt-subtype-fn
   pathcomp
   keyword-ns]
  (fn [user-id ; ignored, because this is ref-data, but needed to treat generated functions uniformly with user-data
       entity-id
       conn
       entity
       version
       format-ind]
    (embedded-refdata-subentity entity-id
                                mt-subtype-fn
                                pathcomp
                                #(resutils/resource-out-transform % keyword-ns config/r-base-url config/r-entity-uri-prefix)
                                conn
                                entity
                                version
                                format-ind)))

(def embedded-origination-device (make-embedded-refdata-entity-fn rmeta/mt-subtype-origination-device
                                                                  rmeta/pathcomp-origination-devices
                                                                  dao/origination-device-keyword-namespace))

(def embedded-body-segment (make-embedded-refdata-entity-fn rmeta/mt-subtype-body-segment
                                                            rmeta/pathcomp-body-segments
                                                            dao/body-segment-keyword-namespace))

(def embedded-muscle-group (make-embedded-refdata-entity-fn rmeta/mt-subtype-muscle-group
                                                            rmeta/pathcomp-muscle-groups
                                                            dao/muscle-group-keyword-namespace))

(def embedded-muscle (make-embedded-refdata-entity-fn rmeta/mt-subtype-muscle
                                                      rmeta/pathcomp-muscles
                                                      dao/muscle-keyword-namespace))

(def embedded-muscle-alias (make-embedded-refdata-entity-fn rmeta/mt-subtype-muscle-alias
                                                            rmeta/pathcomp-muscle-aliases
                                                            dao/muscle-alias-keyword-namespace))

(def embedded-movement (make-embedded-refdata-entity-fn rmeta/mt-subtype-movement
                                                        rmeta/pathcomp-movements
                                                        dao/movement-keyword-namespace))

(def embedded-movement-alias (make-embedded-refdata-entity-fn rmeta/mt-subtype-movement-alias
                                                              rmeta/pathcomp-movement-aliases
                                                              dao/movement-alias-keyword-namespace))

(def embedded-movement-variant (make-embedded-refdata-entity-fn rmeta/mt-subtype-movement-variant
                                                                rmeta/pathcomp-movement-variants
                                                                dao/movement-variant-keyword-namespace))

(def embedded-usersettings (make-embedded-user-entity-fn rmeta/mt-subtype-usersettings
                                                         rmeta/pathcomp-usersettings
                                                         usersettingsresutils/usersettings-out-transform))

(def embedded-set (make-embedded-user-entity-fn rmeta/mt-subtype-set
                                                rmeta/pathcomp-sets
                                                setresutils/set-out-transform))

(def embedded-soreness (make-embedded-user-entity-fn rmeta/mt-subtype-soreness
                                                     rmeta/pathcomp-sorenesses
                                                     sorenessresutils/soreness-out-transform))

(def embedded-journal (make-embedded-user-entity-fn rmeta/mt-subtype-body-journal-log
                                                    rmeta/pathcomp-body-journal-logs
                                                    djresutils/body-journal-log-out-transform))

(defn r-entities->vec
  [version
   db-spec
   accept-format-ind
   user-id
   &
   entities-and-embedded-fns]
  (vec (reduce (fn [results [_ entities embedded-fn]]
                 (concat (map (fn [[entity-id entity]]
                                (embedded-fn user-id
                                             entity-id
                                             db-spec
                                             entity
                                             version
                                             accept-format-ind))
                              entities)
                         results))
               []
               entities-and-embedded-fns)))

(defn r-entities->map
  [version
   db-spec
   accept-format-ind
   user-id
   &
   entities-and-embedded-fns]
  (reduce (fn [results [key entities embedded-fn]]
            (assoc results
                   key
                   (reduce (fn [added-entities [entity-id entity :as e]]
                             (merge added-entities
                                    {entity-id (embedded-fn user-id
                                                            entity-id
                                                            db-spec
                                                            entity
                                                            version
                                                            accept-format-ind)}))
                           {}
                           entities)))
          {}
          entities-and-embedded-fns))

(defn- user-embedded-coll-fn
  [version
   base-url
   entity-uri-prefix
   entity-uri
   db-spec
   accept-format-ind
   user-id
   r-entities->coll]
  (let [origination-devices (dao/origination-devices db-spec)
        body-segments       (dao/body-segments db-spec)
        muscle-groups       (dao/muscle-groups db-spec)
        muscles             (dao/muscles db-spec)
        muscle-aliases      (dao/muscle-aliases db-spec)
        movements           (dao/movements db-spec)
        movmnt-variants     (dao/movement-variants db-spec)
        movmnt-aliases      (dao/movement-aliases db-spec)
        usersettings        (dao/usersettings-for-user db-spec user-id)
        sets                (dao/sets-for-user db-spec user-id)
        sorenesses          (dao/sorenesses-for-user db-spec user-id)
        journals            (dao/body-journal-logs-for-user db-spec user-id)]
    (r-entities->coll version
                      db-spec
                      accept-format-ind
                      user-id
                      [key-origination-devices origination-devices embedded-origination-device]
                      [key-body-segments       body-segments       embedded-body-segment]
                      [key-muscle-groups       muscle-groups       embedded-muscle-group]
                      [key-muscles             muscles             embedded-muscle]
                      [key-muscle-aliases      muscle-aliases      embedded-muscle-alias]
                      [key-movements           movements           embedded-movement]
                      [key-movement-variants   movmnt-variants     embedded-movement-variant]
                      [key-movement-aliases    movmnt-aliases      embedded-movement-alias]
                      [key-usersettings        usersettings        embedded-usersettings]
                      [key-sets                sets                embedded-set]
                      [key-soreness-logs       sorenesses          embedded-soreness]
                      [key-body-journal-logs   journals            embedded-journal])))

(defn- user-embedded-vec-fn
  [version
   base-url
   entity-uri-prefix
   entity-uri
   db-spec
   accept-format-ind
   user-id]
  (user-embedded-coll-fn version
                         base-url
                         entity-uri-prefix
                         entity-uri
                         db-spec
                         accept-format-ind
                         user-id
                         r-entities->vec))

(defn- user-embedded-map-fn
  [version
   base-url
   entity-uri-prefix
   entity-uri
   db-spec
   accept-format-ind
   user-id]
  (user-embedded-coll-fn version
                         base-url
                         entity-uri-prefix
                         entity-uri
                         db-spec
                         accept-format-ind
                         user-id
                         r-entities->map))

(defn- user-embedded-fn-maker
  [ctx]
  (fn [version
       base-url
       entity-uri-prefix
       entity-uri
       db-spec
       accept-format-ind
       user-id]
    (let [desired-embedded-format (get-in ctx [:request :headers config/rhdr-desired-embedded-format])
          embedded-fn (if (= desired-embedded-format config/id-keyed-embedded-format)
                        r-entities->map
                        r-entities->vec)]
      (user-embedded-coll-fn version
                             base-url
                             entity-uri-prefix
                             entity-uri
                             db-spec
                             accept-format-ind
                             user-id
                             embedded-fn))))

(defn changelog-embedded-fn
  [version
   base-url
   entity-uri-prefix
   entity-uri
   db-spec
   accept-format-ind
   user-id
   modified-since]
  (let [user-result (usercore/load-user-by-id-if-modified-since db-spec user-id modified-since)
        {origination-devices :entities} (dao/origination-devices-modified-since db-spec modified-since)
        {body-segments :entities} (dao/body-segments-modified-since db-spec modified-since)
        {muscle-groups :entities} (dao/muscle-groups-modified-since db-spec modified-since)
        {muscles :entities} (dao/muscles-modified-since db-spec modified-since)
        {muscle-aliases :entities} (dao/muscle-aliases-modified-since db-spec modified-since)
        {movements :entities} (dao/movements-modified-since db-spec modified-since)
        {movement-aliases :entities} (dao/movement-aliases-modified-since db-spec modified-since)
        {movement-variants :entities} (dao/movement-variants-modified-since db-spec modified-since)
        {usersettings :entities} (dao/usersettings-modified-since db-spec user-id modified-since)
        {sets :entities} (dao/sets-modified-since db-spec user-id modified-since)
        {sorenesses :entities} (dao/sorenesses-modified-since db-spec user-id modified-since)
        {journals :entities} (dao/body-journal-logs-modified-since db-spec user-id modified-since)
        r-embedded-entities (r-entities->vec version
                                             db-spec
                                             accept-format-ind
                                             user-id
                                             [key-origination-devices origination-devices embedded-origination-device]
                                             [key-body-segments       body-segments       embedded-body-segment]
                                             [key-muscle-groups       muscle-groups       embedded-muscle-group]
                                             [key-muscles             muscles             embedded-muscle]
                                             [key-muscle-aliases      muscle-aliases      embedded-muscle-alias]
                                             [key-movements           movements           embedded-movement]
                                             [key-movement-aliases    movement-aliases    embedded-movement-alias]
                                             [key-movement-variants   movement-variants   embedded-movement-variant]
                                             [key-usersettings        usersettings        embedded-usersettings]
                                             [key-sets                sets                embedded-set]
                                             [key-soreness-logs       sorenesses          embedded-soreness]
                                             [key-body-journal-logs   journals            embedded-journal])]
    (if (not (nil? user-result))
      (let [[_ user] user-result]
        (conj
         r-embedded-entities
         {:media-type (rucore/media-type rumeta/mt-type
                                         (usermeta/mt-subtype-user config/r-mt-subtype-prefix)
                                         version
                                         accept-format-ind)
          :location (rucore/make-abs-link-href config/r-base-url
                                               (str config/r-entity-uri-prefix
                                                    usermeta/pathcomp-users
                                                    "/"
                                                    user-id))
          :payload (userresutils/user-out-transform user)}))
      r-embedded-entities)))

(defn refdata-changelog-embedded-fn
  [version
   base-url
   entity-uri-prefix
   entity-uri
   db-spec
   accept-format-ind
   user-id
   modified-since]
  (let [user-result (usercore/load-user-by-id-if-modified-since db-spec user-id modified-since)
        {origination-devices :entities} (dao/origination-devices-modified-since db-spec modified-since)
        {body-segments :entities} (dao/body-segments-modified-since db-spec modified-since)
        {muscle-groups :entities} (dao/muscle-groups-modified-since db-spec modified-since)
        {muscles :entities} (dao/muscles-modified-since db-spec modified-since)
        {muscle-aliases :entities} (dao/muscle-aliases-modified-since db-spec modified-since)
        {movements :entities} (dao/movements-modified-since db-spec modified-since)
        {movement-aliases :entities} (dao/movement-aliases-modified-since db-spec modified-since)
        {movement-variants :entities} (dao/movement-variants-modified-since db-spec modified-since)
        r-embedded-entities (r-entities->vec version
                                             db-spec
                                             accept-format-ind
                                             user-id
                                             [key-origination-devices origination-devices embedded-origination-device]
                                             [key-body-segments       body-segments       embedded-body-segment]
                                             [key-muscle-groups       muscle-groups       embedded-muscle-group]
                                             [key-muscles             muscles             embedded-muscle]
                                             [key-muscle-aliases      muscle-aliases      embedded-muscle-alias]
                                             [key-movements           movements           embedded-movement]
                                             [key-movement-aliases    movement-aliases    embedded-movement-alias]
                                             [key-movement-variants   movement-variants   embedded-movement-variant])]
    (if (not (nil? user-result))
      (let [[_ user] user-result]
        (conj
         r-embedded-entities
         {:media-type (rucore/media-type rumeta/mt-type
                                         (usermeta/mt-subtype-user config/r-mt-subtype-prefix)
                                         version
                                         accept-format-ind)
          :location (rucore/make-abs-link-href config/r-base-url
                                               (str config/r-entity-uri-prefix
                                                    usermeta/pathcomp-users
                                                    "/"
                                                    user-id))
          :payload (userresutils/user-out-transform user)}))
      r-embedded-entities)))

(defn userdata-changelog-embedded-fn
  [version
   base-url
   entity-uri-prefix
   entity-uri
   db-spec
   accept-format-ind
   user-id
   modified-since]
  (let [user-result (usercore/load-user-by-id-if-modified-since db-spec user-id modified-since)
        {usersettings :entities} (dao/usersettings-modified-since db-spec user-id modified-since)
        {sets :entities} (dao/sets-modified-since db-spec user-id modified-since)
        {sorenesses :entities} (dao/sorenesses-modified-since db-spec user-id modified-since)
        {journals :entities} (dao/body-journal-logs-modified-since db-spec user-id modified-since)
        r-embedded-entities (r-entities->vec version
                                             db-spec
                                             accept-format-ind
                                             user-id
                                             [key-usersettings      usersettings      embedded-usersettings]
                                             [key-sets              sets              embedded-set]
                                             [key-soreness-logs     sorenesses        embedded-soreness]
                                             [key-body-journal-logs journals          embedded-journal])]
    (if (not (nil? user-result))
      (let [[_ user] user-result]
        (conj
         r-embedded-entities
         {:media-type (rucore/media-type rumeta/mt-type
                                         (usermeta/mt-subtype-user config/r-mt-subtype-prefix)
                                         version
                                         accept-format-ind)
          :location (rucore/make-abs-link-href config/r-base-url
                                               (str config/r-entity-uri-prefix
                                                    usermeta/pathcomp-users
                                                    "/"
                                                    user-id))
          :payload (userresutils/user-out-transform user)}))
      r-embedded-entities)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The routes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-route-definitions
  [
   (GET build-version-uri-template
        []
        (fn [req] (resource
                   :available-charsets rumeta/supported-char-sets
                   :available-languages rumeta/supported-languages
                   :allowed-methods [:get]
                   :available-media-types ["text/plain" "text/html"]
                   :handle-ok
                   config/r-app-version)))
   (ANY users-uri-template
        []
        (usersres/users-res config/pooled-db-spec
                            config/r-mt-subtype-prefix
                            config/rhdr-auth-token
                            config/rhdr-error-mask
                            config/r-base-url
                            config/r-entity-uri-prefix
                            config/rhdr-establish-session
                            user-embedded-fn-maker
                            user-links-fn
                            config/r-welcome-and-verification-email-mustache-template
                            config/r-welcome-and-verification-email-subject-line
                            config/r-support-email-address
                            config/r-verification-url-maker
                            config/r-verification-flagged-url-maker
                            config/new-user-notification-mustache-template
                            config/new-user-notification-from-email
                            config/new-user-notification-to-email
                            config/new-user-notification-subject
                            config/err-notification-mustache-template
                            config/err-subject
                            config/err-from-email
                            config/err-to-email
                            config/r-trial-expired-grace-period-in-days
                            config/r-trial-period-in-days))
   (ANY verification-uri-template
        [email
         verification-token]
        (verificationres/account-verification-res config/pooled-db-spec
                                                  config/r-base-url
                                                  config/r-entity-uri-prefix
                                                  email
                                                  verification-token
                                                  config/r-verification-success-uri
                                                  config/r-verification-error-uri
                                                  config/err-notification-mustache-template
                                                  config/err-subject
                                                  config/err-from-email
                                                  config/err-to-email
                                                  config/r-trial-expired-grace-period-in-days))
   (ANY login-uri-template
        []
        (loginres/login-res config/pooled-db-spec
                            config/r-mt-subtype-prefix
                            config/rhdr-auth-token
                            config/rhdr-error-mask
                            config/r-base-url
                            config/r-entity-uri-prefix
                            user-embedded-fn-maker
                            user-links-fn
                            config/rhdr-login-failed-reason
                            config/err-notification-mustache-template
                            config/err-subject
                            config/err-from-email
                            config/err-to-email
                            config/r-trial-expired-grace-period-in-days))
   (ANY light-login-uri-template
        []
        (loginres/light-login-res config/pooled-db-spec
                                  config/r-mt-subtype-prefix
                                  config/rhdr-auth-token
                                  config/rhdr-error-mask
                                  config/r-base-url
                                  config/r-entity-uri-prefix
                                  config/rhdr-login-failed-reason
                                  config/err-notification-mustache-template
                                  config/err-subject
                                  config/err-from-email
                                  config/err-to-email
                                  config/r-trial-expired-grace-period-in-days))
   (ANY logout-uri-template
        [user-id]
        (logoutres/logout-res config/pooled-db-spec
                              config/r-mt-subtype-prefix
                              config/rhdr-auth-token
                              config/rhdr-error-mask
                              config/r-auth-scheme
                              config/r-auth-scheme-param-name
                              config/r-base-url
                              config/r-entity-uri-prefix
                              (Long. user-id)
                              config/err-notification-mustache-template
                              config/err-subject
                              config/err-from-email
                              config/err-to-email))
   (ANY logout-all-other-uri-template
        [user-id]
        (logoutallotherres/logout-all-other-res config/pooled-db-spec
                                                config/r-mt-subtype-prefix
                                                config/rhdr-auth-token
                                                config/rhdr-error-mask
                                                config/r-auth-scheme
                                                config/r-auth-scheme-param-name
                                                config/r-base-url
                                                config/r-entity-uri-prefix
                                                (Long. user-id)
                                                config/err-notification-mustache-template
                                                config/err-subject
                                                config/err-from-email
                                                config/err-to-email
                                                config/r-trial-expired-grace-period-in-days))
   (ANY send-verification-email-uri-template
        [user-id]
        (sendveriemailres/send-verification-email-res config/pooled-db-spec
                                                      config/r-mt-subtype-prefix
                                                      config/rhdr-auth-token
                                                      config/rhdr-error-mask
                                                      config/r-auth-scheme
                                                      config/r-auth-scheme-param-name
                                                      config/r-base-url
                                                      config/r-entity-uri-prefix
                                                      (Long. user-id)
                                                      config/r-verification-email-mustache-template
                                                      config/r-verification-email-subject-line
                                                      config/r-support-email-address
                                                      config/r-verification-url-maker
                                                      config/r-verification-flagged-url-maker
                                                      config/err-notification-mustache-template
                                                      config/err-subject
                                                      config/err-from-email
                                                      config/err-to-email
                                                      config/r-trial-expired-grace-period-in-days))
   (ANY plan-uri-template
        []
        (planres/plan-res config/pooled-db-spec
                          config/r-mt-subtype-prefix
                          config/rhdr-error-mask
                          config/r-base-url
                          config/r-entity-uri-prefix
                          config/err-notification-mustache-template
                          config/err-subject
                          config/err-from-email
                          config/err-to-email))
   (ANY send-password-reset-email-uri-template
        []
        (sendpwdresetemailres/send-password-reset-email-res config/pooled-db-spec
                                                            config/r-mt-subtype-prefix
                                                            config/rhdr-error-mask
                                                            config/r-base-url
                                                            config/r-entity-uri-prefix
                                                            config/r-password-reset-email-mustache-template
                                                            config/r-password-reset-email-subject-line
                                                            config/r-support-email-address
                                                            config/r-prepare-password-reset-url-maker
                                                            config/r-password-reset-flagged-url-maker
                                                            config/err-notification-mustache-template
                                                            config/err-subject
                                                            config/err-from-email
                                                            config/err-to-email
                                                            config/r-trial-expired-grace-period-in-days))
   (ANY prepare-password-reset-uri-template
        [email
         password-reset-token]
        (preparepwdresetres/prepare-password-reset-res config/pooled-db-spec
                                                       config/r-base-url
                                                       config/r-entity-uri-prefix
                                                       (url-decode email)
                                                       password-reset-token
                                                       (config/r-password-reset-web-url-maker email password-reset-token)
                                                       config/r-password-reset-error-web-url
                                                       config/err-notification-mustache-template
                                                       config/err-subject
                                                       config/err-from-email
                                                       config/err-to-email
                                                       config/r-trial-expired-grace-period-in-days))
   (ANY password-reset-uri-template
        []
        (pwdresetres/password-reset-res config/pooled-db-spec
                                        config/r-mt-subtype-prefix
                                        config/rhdr-error-mask
                                        config/r-base-url
                                        config/r-entity-uri-prefix
                                        config/err-notification-mustache-template
                                        config/err-subject
                                        config/err-from-email
                                        config/err-to-email))
   (ANY user-uri-template
        [user-id]
        (userres/user-res config/pooled-db-spec
                          config/r-mt-subtype-prefix
                          config/rhdr-auth-token
                          config/rhdr-error-mask
                          config/r-auth-scheme
                          config/r-auth-scheme-param-name
                          config/r-base-url
                          config/r-entity-uri-prefix
                          (Long. user-id)
                          nil ; embedded-resources-fn
                          user-links-fn
                          config/r-stripe-secret-key
                          config/rhdr-if-unmodified-since
                          config/rhdr-if-modified-since
                          config/rhdr-delete-reason
                          config/err-notification-mustache-template
                          config/err-subject
                          config/err-from-email
                          config/err-to-email
                          config/r-trial-expired-grace-period-in-days))

   (ANY changelog-uri-template
        [user-id]
        (let [user-id-l (Long. user-id)]
          (letfn [(mt-fn-maker [mt-subtype-fn]
                    (fn [version accept-format-ind]
                      (rucore/media-type rumeta/mt-type
                                         (mt-subtype-fn config/r-mt-subtype-prefix)
                                         version
                                         accept-format-ind)))
                  (loc-fn-maker [pathcomp]
                    (fn [id]
                      (make-user-subentity-url user-id pathcomp id)))]
            (clres/changelog-res config/pooled-db-spec
                                 config/r-mt-subtype-prefix
                                 config/rhdr-auth-token
                                 config/rhdr-error-mask
                                 config/r-auth-scheme
                                 config/r-auth-scheme-param-name
                                 config/r-base-url
                                 config/r-entity-uri-prefix
                                 user-id-l
                                 changelog-embedded-fn
                                 nil ; links-fn
                                 config/rhdr-if-modified-since
                                 (fn [ctx] (userresutils/authorized? ctx
                                                                     config/pooled-db-spec
                                                                     user-id-l
                                                                     config/r-auth-scheme
                                                                     config/r-auth-scheme-param-name
                                                                     config/r-trial-expired-grace-period-in-days))
                                 userresutils/get-plaintext-auth-token
                                 [[userddl/tbl-user-account    "id"      "=" user-id-l "updated_at" "deleted_at"]
                                  [rddl/tbl-origination-device nil       nil nil       "updated_at" "deleted_at"]
                                  [rddl/tbl-body-segment       nil       nil nil       "updated_at" "deleted_at"]
                                  [rddl/tbl-muscle-group       nil       nil nil       "updated_at" "deleted_at"]
                                  [rddl/tbl-muscle             nil       nil nil       "updated_at" "deleted_at"]
                                  [rddl/tbl-muscle-alias       nil       nil nil       "updated_at" "deleted_at"]
                                  [rddl/tbl-movement           nil       nil nil       "updated_at" "deleted_at"]
                                  [rddl/tbl-movement-alias     nil       nil nil       "updated_at" "deleted_at"]
                                  [rddl/tbl-movement-variant   nil       nil nil       "updated_at" "deleted_at"]
                                  [rddl/tbl-user-settings      "user_id" "=" user-id-l "updated_at" "deleted_at"]
                                  [rddl/tbl-set                "user_id" "=" user-id-l "updated_at" "deleted_at"]
                                  [rddl/tbl-body-journal-log   "user_id" "=" user-id-l "updated_at" "deleted_at"]
                                  [rddl/tbl-soreness           "user_id" "=" user-id-l "updated_at" "deleted_at"]]
                                 (fn [exc-and-params]
                                   (usercore/send-email config/err-notification-mustache-template
                                                        exc-and-params
                                                        config/err-subject
                                                        config/err-from-email
                                                        config/err-to-email
                                                        config/r-domain
                                                        config/r-mailgun-api-key))))))

   (ANY refdata-changelog-uri-template
        [user-id]
        (let [user-id-l (Long. user-id)]
          (clres/changelog-res config/pooled-db-spec
                               config/r-mt-subtype-prefix
                               config/rhdr-auth-token
                               config/rhdr-error-mask
                               config/r-auth-scheme
                               config/r-auth-scheme-param-name
                               config/r-base-url
                               config/r-entity-uri-prefix
                               user-id-l
                               refdata-changelog-embedded-fn
                               nil ; links-fn
                               config/rhdr-if-modified-since
                               (fn [ctx] (userresutils/authorized? ctx
                                                                   config/pooled-db-spec
                                                                   user-id-l
                                                                   config/r-auth-scheme
                                                                   config/r-auth-scheme-param-name
                                                                   config/r-trial-expired-grace-period-in-days))
                               (fn [ctx _ __] (:plaintext-auth-token ctx))
                               [[rddl/tbl-origination-device nil nil nil "updated_at" "deleted_at"]
                                [rddl/tbl-body-segment       nil nil nil "updated_at" "deleted_at"]
                                [rddl/tbl-muscle-group       nil nil nil "updated_at" "deleted_at"]
                                [rddl/tbl-muscle             nil nil nil "updated_at" "deleted_at"]
                                [rddl/tbl-muscle-alias       nil nil nil "updated_at" "deleted_at"]
                                [rddl/tbl-movement           nil nil nil "updated_at" "deleted_at"]
                                [rddl/tbl-movement-alias     nil nil nil "updated_at" "deleted_at"]
                                [rddl/tbl-movement-variant   nil nil nil "updated_at" "deleted_at"]]
                               (fn [exc-and-params]
                                 (usercore/send-email config/err-notification-mustache-template
                                                      exc-and-params
                                                      config/err-subject
                                                      config/err-from-email
                                                      config/err-to-email
                                                      config/r-domain
                                                      config/r-mailgun-api-key)))))

   (ANY userdata-changelog-uri-template
        [user-id]
        (let [user-id-l (Long. user-id)]
          (clres/changelog-res config/pooled-db-spec
                               config/r-mt-subtype-prefix
                               config/rhdr-auth-token
                               config/rhdr-error-mask
                               config/r-auth-scheme
                               config/r-auth-scheme-param-name
                               config/r-base-url
                               config/r-entity-uri-prefix
                               user-id-l
                               userdata-changelog-embedded-fn
                               nil ; links-fn
                               config/rhdr-if-modified-since
                               (fn [ctx] (userresutils/authorized? ctx
                                                                   config/pooled-db-spec
                                                                   user-id-l
                                                                   config/r-auth-scheme
                                                                   config/r-auth-scheme-param-name
                                                                   config/r-trial-expired-grace-period-in-days))
                               userresutils/get-plaintext-auth-token
                               [[rddl/tbl-user-settings    "user_id" "=" user-id-l "updated_at" "deleted_at"]
                                [rddl/tbl-set              "user_id" "=" user-id-l "updated_at" "deleted_at"]
                                [rddl/tbl-body-journal-log "user_id" "=" user-id-l "updated_at" "deleted_at"]
                                [rddl/tbl-soreness         "user_id" "=" user-id-l "updated_at" "deleted_at"]]
                               (fn [exc-and-params]
                                 (usercore/send-email config/err-notification-mustache-template
                                                      exc-and-params
                                                      config/err-subject
                                                      config/err-from-email
                                                      config/err-to-email
                                                      config/r-domain
                                                      config/r-mailgun-api-key)))))

   (ANY usersettings-uri-template
        [user-id usersettings-id]
        (usersettingsres/usersettings-res config/pooled-db-spec
                                          config/r-mt-subtype-prefix
                                          config/rhdr-auth-token
                                          config/rhdr-error-mask
                                          config/r-auth-scheme
                                          config/r-auth-scheme-param-name
                                          config/r-base-url
                                          config/r-entity-uri-prefix
                                          (Long. user-id)
                                          (Long. usersettings-id)
                                          nil ; embedded-resources-fn
                                          nil ; links-fn
                                          config/rhdr-if-unmodified-since
                                          config/rhdr-if-modified-since
                                          config/err-notification-mustache-template
                                          config/err-subject
                                          config/err-from-email
                                          config/err-to-email
                                          config/r-trial-expired-grace-period-in-days))
   (ANY sets-uri-template
        [user-id]
        (setsres/sets-res config/pooled-db-spec
                          config/r-mt-subtype-prefix
                          config/rhdr-auth-token
                          config/rhdr-error-mask
                          config/r-auth-scheme
                          config/r-auth-scheme-param-name
                          config/r-base-url
                          config/r-entity-uri-prefix
                          (Long. user-id)
                          nil ; embedded-resources-fn
                          nil ; links-fn
                          config/err-notification-mustache-template
                          config/err-subject
                          config/err-from-email
                          config/err-to-email
                          config/r-trial-expired-grace-period-in-days))
   (ANY sets-file-import-uri-template
        [user-id]
        (setsimportres/sets-import-res config/pooled-db-spec
                                       config/r-mt-subtype-prefix
                                       config/rhdr-auth-token
                                       config/rhdr-error-mask
                                       config/r-auth-scheme
                                       config/r-auth-scheme-param-name
                                       config/r-base-url
                                       config/r-entity-uri-prefix
                                       (Long. user-id)
                                       nil ; embedded-resources-fn
                                       nil ; links-fn
                                       config/err-notification-mustache-template
                                       config/err-subject
                                       config/err-from-email
                                       config/err-to-email
                                       config/r-trial-expired-grace-period-in-days))
   (ANY set-uri-template
        [user-id set-id]
        (setres/set-res config/pooled-db-spec
                        config/r-mt-subtype-prefix
                        config/rhdr-auth-token
                        config/rhdr-error-mask
                        config/r-auth-scheme
                        config/r-auth-scheme-param-name
                        config/r-base-url
                        config/r-entity-uri-prefix
                        (Long. user-id)
                        (Long. set-id)
                        nil ; embedded-resources-fn
                        nil ; links-fn
                        config/rhdr-if-unmodified-since
                        config/rhdr-if-modified-since
                        config/err-notification-mustache-template
                        config/err-subject
                        config/err-from-email
                        config/err-to-email
                        config/r-trial-expired-grace-period-in-days))
   (ANY movement-uri-template
        [user-id movement-id]
        (movementres/movement-res config/pooled-db-spec
                                  config/r-mt-subtype-prefix
                                  config/rhdr-auth-token
                                  config/rhdr-error-mask
                                  config/r-auth-scheme
                                  config/r-auth-scheme-param-name
                                  config/r-base-url
                                  config/r-entity-uri-prefix
                                  (Long. user-id)
                                  (Long. movement-id)
                                  nil ; embedded-resources-fn
                                  nil ; links-fn
                                  config/rhdr-if-unmodified-since
                                  config/rhdr-if-modified-since
                                  config/err-notification-mustache-template
                                  config/err-subject
                                  config/err-from-email
                                  config/err-to-email
                                  config/r-trial-expired-grace-period-in-days))
   (ANY movement-variant-uri-template
        [user-id movement-variant-id]
        (movementvariantres/movement-variant-res config/pooled-db-spec
                                                 config/r-mt-subtype-prefix
                                                 config/rhdr-auth-token
                                                 config/rhdr-error-mask
                                                 config/r-auth-scheme
                                                 config/r-auth-scheme-param-name
                                                 config/r-base-url
                                                 config/r-entity-uri-prefix
                                                 (Long. user-id)
                                                 (Long. movement-variant-id)
                                                 nil ; embedded-resources-fn
                                                 nil ; links-fn
                                                 config/rhdr-if-unmodified-since
                                                 config/rhdr-if-modified-since
                                                 config/err-notification-mustache-template
                                                 config/err-subject
                                                 config/err-from-email
                                                 config/err-to-email
                                                 config/r-trial-expired-grace-period-in-days))
   (ANY stripe-tokens-uri-template
        [user-id]
        (stripetokensres/stripe-tokens-res config/pooled-db-spec
                                           config/r-mt-subtype-prefix
                                           config/rhdr-auth-token
                                           config/rhdr-error-mask
                                           config/r-auth-scheme
                                           config/r-auth-scheme-param-name
                                           config/r-base-url
                                           config/r-entity-uri-prefix
                                           (Long. user-id)
                                           nil ; embedded-resources-fn
                                           nil ; links-fn
                                           config/r-stripe-secret-key
                                           config/r-stripe-subscription-name
                                           config/err-notification-mustache-template
                                           config/err-subject
                                           config/err-from-email
                                           config/err-to-email
                                           config/r-trial-expired-grace-period-in-days))
   (ANY stripe-events-uri-template
        []
        (stripeeventsres/stripe-events-res config/pooled-db-spec
                                           config/r-mt-subtype-prefix
                                           config/rhdr-auth-token
                                           config/rhdr-error-mask
                                           config/r-base-url
                                           config/r-entity-uri-prefix
                                           config/err-notification-mustache-template
                                           config/err-subject
                                           config/err-from-email
                                           config/err-to-email
                                           config/r-invoice-payment-failed-email-subject
                                           config/r-invoice-payment-failed-email-mustache-template))
   (ANY apple-search-ads-attributions-uri-template
        []
        (iadattrsres/iad-attributions-res config/pooled-db-spec
                                          config/r-mt-subtype-prefix
                                          config/rhdr-auth-token
                                          config/rhdr-error-mask
                                          config/r-base-url
                                          config/r-entity-uri-prefix
                                          config/err-notification-mustache-template
                                          config/err-subject
                                          config/err-from-email
                                          config/err-to-email))
   (ANY apple-search-ads-attribution-uri-template
        [user-id attribution-id]
        (iadattrres/iad-attribution-res config/pooled-db-spec
                                        config/r-mt-subtype-prefix
                                        config/rhdr-auth-token
                                        config/rhdr-error-mask
                                        config/r-auth-scheme
                                        config/r-auth-scheme-param-name
                                        config/r-base-url
                                        config/r-entity-uri-prefix
                                        (Long. user-id)
                                        (Long. attribution-id)
                                        config/err-notification-mustache-template
                                        config/err-subject
                                        config/err-from-email
                                        config/err-to-email))
   (ANY continue-with-facebook-uri-template
        []
        (facebookres/continue-with-facebook-res config/pooled-db-spec
                                                config/r-trial-period-in-days
                                                config/r-mt-subtype-prefix
                                                config/rhdr-auth-token
                                                config/rhdr-error-mask
                                                config/r-base-url
                                                config/r-entity-uri-prefix
                                                user-embedded-fn-maker
                                                user-links-fn
                                                config/err-notification-mustache-template
                                                config/err-subject
                                                config/err-from-email
                                                config/err-to-email))
   (ANY facebook-deauthorize-uri-template
        []
        (facebookres/facebook-deauthorize-res config/pooled-db-spec
                                              config/r-mt-subtype-prefix
                                              config/r-base-url
                                              config/r-entity-uri-prefix
                                              config/err-notification-mustache-template
                                              config/err-subject
                                              config/err-from-email
                                              config/err-to-email))
   (ANY facebook-data-delete-request-uri-template
        []
        (facebookres/facebook-data-delete-request-res config/pooled-db-spec
                                                      config/r-mt-subtype-prefix
                                                      config/r-base-url
                                                      config/r-entity-uri-prefix
                                                      config/err-notification-mustache-template
                                                      config/err-subject
                                                      config/err-from-email
                                                      config/err-to-email))
   (ANY body-journal-logs-uri-template
        [user-id]
        (djsres/body-journal-logs-res config/pooled-db-spec
                                      config/r-mt-subtype-prefix
                                      config/rhdr-auth-token
                                      config/rhdr-error-mask
                                      config/r-auth-scheme
                                      config/r-auth-scheme-param-name
                                      config/r-base-url
                                      config/r-entity-uri-prefix
                                      (Long. user-id)
                                      nil ; embedded-resources-fn
                                      nil ; links-fn
                                      config/err-notification-mustache-template
                                      config/err-subject
                                      config/err-from-email
                                      config/err-to-email
                                      config/r-trial-expired-grace-period-in-days))
   (ANY body-journal-logs-file-import-uri-template
        [user-id]
        (djsimportres/body-journal-logs-import-res config/pooled-db-spec
                                                   config/r-mt-subtype-prefix
                                                   config/rhdr-auth-token
                                                   config/rhdr-error-mask
                                                   config/r-auth-scheme
                                                   config/r-auth-scheme-param-name
                                                   config/r-base-url
                                                   config/r-entity-uri-prefix
                                                   (Long. user-id)
                                                   nil ; embedded-resources-fn
                                                   nil ; links-fn
                                                   config/err-notification-mustache-template
                                                   config/err-subject
                                                   config/err-from-email
                                                   config/err-to-email
                                                   config/r-trial-expired-grace-period-in-days))
   (ANY body-journal-log-uri-template
        [user-id body-journal-log-id]
        (djres/body-journal-log-res config/pooled-db-spec
                                    config/r-mt-subtype-prefix
                                    config/rhdr-auth-token
                                    config/rhdr-error-mask
                                    config/r-auth-scheme
                                    config/r-auth-scheme-param-name
                                    config/r-base-url
                                    config/r-entity-uri-prefix
                                    (Long. user-id)
                                    (Long. body-journal-log-id)
                                    nil ; embedded-resources-fn
                                    nil ; links-fn
                                    config/rhdr-if-unmodified-since
                                    config/rhdr-if-modified-since
                                    config/err-notification-mustache-template
                                    config/err-subject
                                    config/err-from-email
                                    config/err-to-email
                                    config/r-trial-expired-grace-period-in-days))
   (ANY sorenesses-uri-template
        [user-id]
        (sorenessesres/sorenesses-res config/pooled-db-spec
                                      config/r-mt-subtype-prefix
                                      config/rhdr-auth-token
                                      config/rhdr-error-mask
                                      config/r-auth-scheme
                                      config/r-auth-scheme-param-name
                                      config/r-base-url
                                      config/r-entity-uri-prefix
                                      (Long. user-id)
                                      nil ; embedded-resources-fn
                                      nil ; links-fn
                                      config/err-notification-mustache-template
                                      config/err-subject
                                      config/err-from-email
                                      config/err-to-email
                                      config/r-trial-expired-grace-period-in-days))
   (ANY soreness-uri-template
        [user-id soreness-id]
        (sorenessres/soreness-res config/pooled-db-spec
                                  config/r-mt-subtype-prefix
                                  config/rhdr-auth-token
                                  config/rhdr-error-mask
                                  config/r-auth-scheme
                                  config/r-auth-scheme-param-name
                                  config/r-base-url
                                  config/r-entity-uri-prefix
                                  (Long. user-id)
                                  (Long. soreness-id)
                                  nil ; embedded-resources-fn
                                  nil ; links-fn
                                  config/rhdr-if-unmodified-since
                                  config/rhdr-if-modified-since
                                  config/err-notification-mustache-template
                                  config/err-subject
                                  config/err-from-email
                                  config/err-to-email
                                  config/r-trial-expired-grace-period-in-days))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Middleware-decorated app
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def r-app
  (-> r-route-definitions
      (handler/api)
      (wrap-params)
      (wrap-cookies)))
