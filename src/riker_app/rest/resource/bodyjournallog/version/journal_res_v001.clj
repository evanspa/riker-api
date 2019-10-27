(ns riker-app.rest.resource.bodyjournallog.version.journal-res-v001
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.bodyjournallog.journal-utils :as djresutils]
            [riker-app.rest.resource.bodyjournallog.journal-res :refer [save-body-journal-log-validator-fn
                                                                        body-data-in-transform-fn
                                                                        body-data-out-transform-fn
                                                                        save-body-journal-log-fn
                                                                        delete-body-journal-log-fn
                                                                        load-body-journal-log-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-body-journal-log-validator-fn meta/v001
  [version body-journal-log]
  0)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   body-journal-log-entid
   body-journal-log]
  (djresutils/body-journal-log-in-transform body-journal-log))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   user-id
   body-journal-log-id
   base-url
   entity-uri-prefix
   entity-uri
   body-journal-log]
  (djresutils/body-journal-log-out-transform body-journal-log base-url entity-uri-prefix))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Save body-journal-log function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-body-journal-log-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   body-journal-log-id
   plaintext-auth-token
   body-journal-log
   if-unmodified-since]
  (dao/save-body-journal-log db-spec
                         body-journal-log-id
                         (assoc body-journal-log :bodyjournallog/user-id user-id)
                         if-unmodified-since))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Delete body-journal-log function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod delete-body-journal-log-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   body-journal-log-id
   delete-reason
   plaintext-auth-token
   if-unmodified-since]
  (dao/mark-body-journal-log-as-deleted db-spec body-journal-log-id if-unmodified-since))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Load body-journal-log function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod load-body-journal-log-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   body-journal-log-id
   plaintext-auth-token
   if-modified-since]
  (dao/body-journal-log-by-id db-spec body-journal-log-id true))
