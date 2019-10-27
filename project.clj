(defproject riker-app "0.0.40"
  :description "Riker's REST API."
  :url "https://bitbucket.org/evanspa/riker-app"
  :license "Proprietary"
  :plugins [[lein-pprint "1.2.0"]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [org.slf4j/slf4j-api "1.7.25"]
                 [clj-time "0.14.4"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/java.jdbc "0.7.7"]
                 [org.postgresql/postgresql "42.2.2"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.3"]
                 [com.cemerick/friend "0.2.3"]
                 [ring/ring-codec "1.1.1"]
                 [ring/ring-core "1.6.3"]
                 [ring-server "0.5.0"]
                 [compojure "1.6.1"]
                 [liberator "0.15.2"]
                 [environ "1.1.0"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [clojurewerkz/quartzite "2.1.0"]
                 [pe-core-utils "0.0.15"]
                 [abengoa/clj-stripe "1.0.4"]
                 [clj-mailgun "0.2.0"]
                 [org.apache.httpcomponents/httpclient "4.5.5"]
                 [cheshire "5.8.0"]
                 [clj-http "3.9.0"]]
  :resource-paths ["resources"
                   "email-templates"]
  :ring {:handler riker-app.app.endpoint/riker-app
         :init    riker-app.app.lifecycle/init
         :destroy riker-app.app.lifecycle/stop
         :port    3006}
  :profiles {:dev {:source-paths ["dev"]  ;ensures 'user.clj' gets auto-loaded
                   :env {:r-app-version "0.0.40"
                         :r-domain "rikerapp.com"
                         :r-uri-prefix "/riker/d/"
                         :r-db-name "riker"
                         :r-db-server-host "localhost"
                         :r-db-server-port 5432
                         :r-db-username "riker"
                         :r-jdbc-driver-class "org.postgresql.Driver"
                         :r-jdbc-subprotocol "postgresql"
                         :r-base-url "https://dev.rikerapp.com"
                         :r-login-cookie-secure "false"
                         :r-email-live-mode false
                         :r-mailgun-api-key "_REMOVED_"
                         :r-max-allowed-set-import 5000
                         :r-max-allowed-bml-import 5000
                         :r-nrepl-server-port 7889
                         :r-trial-period-in-days 90
                         :r-trial-period-expired-grace-period-in-days 10
                         :r-trial-almost-expires-threshold-in-days 5
                         :r-subscription-amount 1149
                         :r-stripe-secret-key "_REMOVED_"
                         :r-stripe-subscription-name "basic"
                         :r-stripe-webhook-secret-path-comp "_REMOVED_"
                         :r-app-store-receipt-validation-url "https://sandbox.itunes.apple.com/verifyReceipt"
                         :r-app-store-receipt-validation-shared-secret "_REMOVED_"
                         :r-apple-search-ads-attribution-upload-secret-path-comp "_REMOVED_"
                         :r-facebook-app-secret "_REMOVED_"
                         :r-facebook-callback-secret-path-comp "_REMOVED_"
                         :r-new-user-notification-from-email "alerts@rikerapp.com"
                         :r-new-user-notification-to-email "alerts@rikerapp.com"
                         :r-new-user-notification-subject "New Riker Sign-up!"
                         :r-err-notification-subject "Riker Error Caught"
                         :r-err-notification-from-email "errors@rikerapp.com"
                         :r-err-notification-to-email "errors@rikerapp.com"}
                   :plugins [[cider/cider-nrepl "0.17.0"]
                             [lein-environ "1.1.0"]
                             [lein-ring "0.12.4"]]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [org.clojure/java.classpath "0.3.0"]
                                  [ring/ring-mock "0.3.0"]
                                  [ring.middleware.logger "0.5.0"]]
                   :resource-paths ["test-resources"]}
             :test {:resource-paths ["test-resources"]}}
  :jvm-opts ["-Xmx1g"
             "-DRIKERAPP_LOGS_DIR=logs"
             "-DRIKERAPP_LOGS_ROLLOVER_DIR=logs"])
