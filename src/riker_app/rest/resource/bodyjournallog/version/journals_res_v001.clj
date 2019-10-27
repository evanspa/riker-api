(ns riker-app.rest.resource.bodyjournallog.version.journals-res-v001
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.bodyjournallog.journal-utils :as djresutils]
            [riker-app.rest.resource.bodyjournallog.journals-res :refer [new-body-journal-log-validator-fn
                                                                         body-data-in-transform-fn
                                                                         body-data-out-transform-fn
                                                                         save-new-body-journal-log-fn
                                                                         next-body-journal-log-id-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod new-body-journal-log-validator-fn meta/v001
  [version body-journal-log]
  0)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   new-body-journal-log]
  (djresutils/body-journal-log-in-transform new-body-journal-log))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   user-id
   base-url
   entity-uri-prefix
   entity-uri
   new-body-journal-log-id
   new-body-journal-log]
  (djresutils/body-journal-log-out-transform new-body-journal-log base-url entity-uri-prefix))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Save new body-journal-log function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-new-body-journal-log-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   plaintext-auth-token
   new-body-journal-log-id
   body-journal-log]
  (if (nil? (:bodyjournallog/imported-at body-journal-log))
    (dao/save-new-body-journal-log db-spec
                                   user-id
                                   new-body-journal-log-id
                                   body-journal-log)
    (let [user (:user ctx)
          is-not-verified (nil? (:user/verified-at user))
          max-import-allowed (:user/max-allowed-bml-import user)
          num-imported (dao/num-imported-body-journal-logs db-spec user-id)]
      (if (> num-imported max-import-allowed)
        (throw (IllegalArgumentException. (str val/body-journal-log-imported-limit-exceeded)))
        (if is-not-verified
          (throw (IllegalArgumentException. (str val/body-journal-log-imported-unverified-email)))
          (dao/save-new-body-journal-log db-spec
                                         user-id
                                         new-body-journal-log-id
                                         body-journal-log))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Next body-journal-log id function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod next-body-journal-log-id-fn meta/v001
  [version db-spec]
  (dao/next-body-journal-log-id db-spec))
