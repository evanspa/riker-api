(ns riker-app.rest.resource.usersettings.version.usersettings-res-v001
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.usersettings.usersettings-utils :as usersettingsresutils]
            [riker-app.rest.resource.usersettings.usersettings-res :refer [save-usersettings-validator-fn
                                                         body-data-in-transform-fn
                                                         body-data-out-transform-fn
                                                         save-usersettings-fn
                                                         delete-usersettings-fn
                                                         load-usersettings-fn]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-usersettings-validator-fn meta/v001
  [version usersettings]
  0)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version
   user-id
   usersettings-entid
   usersettings]
  (usersettingsresutils/usersettings-in-transform usersettings))

(defmethod body-data-out-transform-fn meta/v001
  [version
   db-spec
   user-id
   usersettings-id
   base-url
   entity-uri-prefix
   entity-uri
   usersettings]
  (usersettingsresutils/usersettings-out-transform usersettings base-url entity-uri-prefix))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Save user settings function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod save-usersettings-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   usersettings-id
   plaintext-auth-token
   usersettings
   if-unmodified-since]
  (dao/save-usersettings db-spec
                         usersettings-id
                         (assoc usersettings :usersettings/user-id user-id)
                         if-unmodified-since))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Load user settings function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod load-usersettings-fn meta/v001
  [ctx
   version
   db-spec
   user-id
   usersettings-id
   plaintext-auth-token
   if-modified-since]
  (dao/usersettings-by-id db-spec usersettings-id true))
