(ns riker-app.rest.utils.changelog.version.resource-support-v001
  (:require [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.tools.logging :as log]
            [pe-core-utils.core :as ucore]
            [riker-app.core.jdbc :as jcore]
            [riker-app.rest.utils.core :as core]
            [riker-app.rest.user-utils :as userrestutil]
            [riker-app.rest.utils.changelog.meta :as clmeta]
            [riker-app.rest.utils.changelog.resource-support :refer [load-changelog-fn
                                                              body-data-out-transform-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-out-transform-fn clmeta/v001
  [version
   db-spec
   user-id
   base-url
   entity-uri-prefix
   entity-uri
   changelog]
  (-> changelog
      (userrestutil/user-out-transform)
      (ucore/transform-map-val :changelog/updated-at #(c/to-long %))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 load-changelog-fn function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod load-changelog-fn clmeta/v001
  [ctx
   version
   db-spec
   user-id
   plaintext-auth-token
   modified-since
   tables-and-updated-at-cols]
  [nil {:changelog/updated-at
        (jcore/most-recent-modified-at-overall db-spec
                                               modified-since
                                               tables-and-updated-at-cols)}])
