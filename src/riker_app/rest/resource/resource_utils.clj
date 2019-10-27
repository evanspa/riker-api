(ns riker-app.rest.resource.resource-utils
  (:require [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [pe-core-utils.core :as ucore]))

(defn resource-out-transform
  [entity keyword-ns base-url entity-uri-prefix]
  (-> entity
      (ucore/transform-map-val (keyword keyword-ns "created-at") #(c/to-long %))
      (ucore/transform-map-val (keyword keyword-ns "deleted-at") #(c/to-long %))
      (ucore/transform-map-val (keyword keyword-ns "updated-at") #(c/to-long %))
      (dissoc (keyword keyword-ns "updated-count"))
      (dissoc (keyword keyword-ns "user-id"))))
