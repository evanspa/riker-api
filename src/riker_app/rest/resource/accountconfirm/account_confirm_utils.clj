(ns riker-app.rest.resource.accountconfirm.account-confirm-utils
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.user-utils :as userrestutil]
            [riker-app.rest.meta :as meta]))

(defn account-confirm-in-transform
  [stripe-token]
  (-> stripe-token
      (ucore/transform-map-val :stripetoken/token-created-at #(c/from-long (Long. %)))))
