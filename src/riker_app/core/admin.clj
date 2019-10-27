(ns riker-app.core.admin
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as j]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.dao :as dao]))

(declare copy-user)

(defn copy-user-by-id
  [db-spec user-id new-email new-name new-password]
  (let [[_ user] (usercore/load-user-by-id db-spec user-id)]
    (copy-user db-spec user new-email new-name new-password)))

(defn copy-user-by-email
  [db-spec email new-email new-name new-password]
  (let [[_ user] (usercore/load-user-by-email db-spec email)]
    (copy-user db-spec user new-email new-name new-password)))

(defn copy-user
  [db-spec user new-email new-name new-password]
  (j/with-db-transaction [conn db-spec]
    (let [user-id (:user/id user)
          new-user-id (usercore/next-user-account-id conn)]
      (usercore/save-new-user conn
                              new-user-id
                              {:user/name new-name
                               :user/email new-email
                               :user/password new-password})
      )))
