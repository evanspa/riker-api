(ns riker-app.rest.resource.iad.version.iad-attributions-res-v001
  (:require [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [clojure.java.jdbc :as j]
            [pe-core-utils.core :as ucore]
            [riker-app.utils :as rutils]
            [riker-app.core.user-dao :as usercore]
            [riker-app.core.dao :as dao]
            [riker-app.core.validation :as val]
            [riker-app.core.user-ddl :as uddl]
            [riker-app.core.ddl :as ddl]
            [riker-app.rest.meta :as meta]
            [riker-app.rest.resource.iad.iad-attributions-res :refer [new-iad-attribution-validator-fn
                                                                      body-data-in-transform-fn
                                                                      body-data-out-transform-fn
                                                                      process-new-iad-attribution-fn]]
            [riker-app.app.config :as config]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Validator function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod new-iad-attribution-validator-fn meta/v001
  [version iad-attribution]
  0)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 body-data transformation functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod body-data-in-transform-fn meta/v001
  [version new-iad-attribution]
  (identity new-iad-attribution))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 0.0.1 Save new iad attribution function
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod process-new-iad-attribution-fn meta/v001
  [ctx version db-spec iad-attribution]
  (try
    (let [result (j/insert! db-spec
                            ddl/tbl-apple-search-ads-attribution
                            (clojure.set/rename-keys iad-attribution
                                                     {:iad-attribution :iad_attribution
                                                      :iad-org-name :iad_org_name
                                                      :iad-campaign-id :iad_campaign_id
                                                      :iad-campaign-name :iad_campaign_name
                                                      :iad-purchase-date :iad_purchase_date
                                                      :iad-conversion-date :iad_conversion_date
                                                      :iad-conversion-type :iad_conversion_type
                                                      :iad-click-date :iad_click_date
                                                      :iad-adgroup-id :iad_adgroup_id
                                                      :iad-adgroup-name :iad_adgroup_name
                                                      :iad-keyword :iad_keyword
                                                      :iad-keyword-matchtype :iad_keyword_matchtype
                                                      :iad-creativeset-id :iad_creativeset_id
                                                      :iad-creativeset-name :iad_creativeset_name}))]
      (if (and (not (nil? result))
                 (= (count result) 1))
        {:status 201 :entity (json/write-str {:iad-attribution-id (:id (first result))})}
        (do
          (log/error (str "Error attempting to save new iad attribution: " iad-attribution ", sql result: " result))
          {:status 500})))
    (catch Exception e
      (rutils/log-e e (str "Exception processing iad attribution.  IAD attribution: " iad-attribution))
      {:status 500})))
