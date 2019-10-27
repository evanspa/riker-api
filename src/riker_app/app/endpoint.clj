(ns riker-app.app.endpoint
  (:require [compojure.core :refer [routes]]
            [liberator.dev :refer [wrap-trace]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.params :refer [wrap-params]]
            ;[ring.middleware.logger :refer [wrap-with-logger]] ; dev only
            [compojure.handler :as handler]
            [riker-app.app.core :refer [r-route-definitions]]))

(def riker-routes (apply routes r-route-definitions))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Middleware-decorated app
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def riker-app
  (-> riker-routes
      ;(wrap-with-logger) ; dev only
      (handler/api)
      (wrap-params)
      (wrap-cookies)
      #_(wrap-trace :header)))
