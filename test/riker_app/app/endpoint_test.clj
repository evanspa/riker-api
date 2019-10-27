(ns riker-app.app.endpoint-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as j]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.java.io :as io]
            [ring.mock.request :as mock]
            [pe-core-utils.core :as ucore]
            [riker-app.core.jdbc :as jcore]
            [riker-app.rest.utils.core :as rucore]
            [riker-app.rest.utils.meta :as rumeta]
            [riker-app.rest.utils.changelog.meta :as clmeta]
            [riker-app.app.test-utils :as rtucore]
            [riker-app.core.user-dao :as usercore]
            [riker-app.rest.user-meta :as usermeta]
            [riker-app.rest.meta :as rmeta]
            [riker-app.core.dao :as dao]
            [riker-app.app.endpoint :as endpoint]
            [riker-app.app.core :as core]
            [riker-app.app.lifecycle :as lifecycle]
            [riker-app.app.config :as config]))

; We do this because otherwise the 'drop-database' call will fail because you
; cannot drop a database in postgres in there are existing open connections
; connected to it.  So by do this, we effectively convert the pooled db-spec to
; a regular, non-pooled one.
(alter-var-root (var config/pooled-db-spec) (fn [_] config/db-spec))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fixtures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(use-fixtures :each (fn [f]
                      (try
                        (jcore/drop-database config/db-spec-without-db config/r-db-name)
                        (catch Exception e))
                      (jcore/create-database config/db-spec-without-db config/r-db-name)
                      (lifecycle/init-database)
                      (f)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
#_(deftest integration-test-1
  (testing "Multiple Logins of user and subsequent authenticated request."
    (is (nil? (usercore/load-user-by-email config/db-spec "smithka@testing.com")))
    (is (nil? (usercore/load-user-by-username config/db-spec "smithk")))


    ; TODO - instead of saving a new user using the DAO, create a new user using
    ; the web API!!!

    (let [user {:user/name "Karen Smith"
                :user/email "smithka@testing.com"
                :user/password "insecure"
                :user/is-payment-past-due false}
          new-user-id (usercore/next-user-account-id config/db-spec)]
      (usercore/save-new-user config/db-spec
                              new-user-id
                              user)
      (is (not (nil? (usercore/load-user-by-email config/db-spec "smithka@testing.com")))))
    ;; 1st Login
    (let [user {"user/username-or-email" "smithka@testing.com"
                "user/password" "insecure"}
          req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                          (usermeta/mt-subtype-user config/r-mt-subtype-prefix)
                                          usermeta/v001
                                          "UTF-8;q=1,ISO-8859-1;q=0"
                                          "json"
                                          "en-US"
                                          :post
                                          core/login-uri-template)
                  (mock/body (json/write-str user))
                  (mock/content-type (rucore/content-type rumeta/mt-type
                                                          (usermeta/mt-subtype-user config/r-mt-subtype-prefix)
                                                          usermeta/v001
                                                          "json"
                                                          "UTF-8")))
          resp (endpoint/riker-app req)]
      (testing "status code" (is (= 200 (:status resp)))))

    ;; Login again (creating a 2nd auth token in the database)
    #_(let [user {"user/username-or-email" "smithka@testing.com"
                "user/password" "insecure"}
          req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                          (usermeta/mt-subtype-user config/r-mt-subtype-prefix)
                                          usermeta/v001
                                          "UTF-8;q=1,ISO-8859-1;q=0"
                                          "json"
                                          "en-US"
                                          :post
                                          core/login-uri-template)
                  (mock/body (json/write-str user))
                  (mock/content-type (rucore/content-type rumeta/mt-type
                                                          (usermeta/mt-subtype-user config/r-mt-subtype-prefix)
                                                          usermeta/v001
                                                          "json"
                                                          "UTF-8")))
          resp (endpoint/riker-app req)]
      (testing "status code" (is (= 200 (:status resp))))
      (let [hdrs (:headers resp)
            resp-body-stream (:body resp)
            user-location-str (get hdrs "location")
            resp-user-entid-str (rtucore/last-url-part user-location-str)
            pct (rucore/parse-media-type (get hdrs "Content-Type"))
            charset (get rumeta/char-sets (:charset pct))
            resp-user (rucore/read-res pct resp-body-stream charset)
            auth-token (get hdrs config/rhdr-auth-token)
            [loaded-user-entid loaded-user-ent] (usercore/load-user-by-authtoken config/db-spec
                                                                                 (Long. resp-user-entid-str)
                                                                                 auth-token)]
        (letfn [(get-changelog [modified-since expected-status-code expected-num-entities]
                  (let [changelog-uri (str config/r-base-url
                                           config/r-entity-uri-prefix
                                           usermeta/pathcomp-users
                                           "/"
                                           resp-user-entid-str
                                           "/"
                                           clmeta/pathcomp-changelog)
                        req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                                        (clmeta/mt-subtype-changelog config/r-mt-subtype-prefix)
                                                        rmeta/v001
                                                        "UTF-8;q=1,ISO-8859-1;q=0"
                                                        "json"
                                                        "en-US"
                                                        :get
                                                        changelog-uri)
                                (rtucore/header config/rhdr-if-modified-since (Long/toString (c/to-long modified-since)))
                                (rtucore/header "Authorization" (rtucore/authorization-req-hdr-val config/r-auth-scheme
                                                                                                   config/r-auth-scheme-param-name
                                                                                                   auth-token)))
                        resp (endpoint/riker-app req)]
                    (testing "status code" (is (= expected-status-code (:status resp))))
                    (testing "headers and body of fetched change log"
                      (when (= expected-status-code 200)
                        (let [hdrs (:headers resp)
                              resp-body-stream (:body resp)]
                          (is (= "Accept, Accept-Charset, Accept-Language" (get hdrs "Vary")))
                          (is (not (nil? resp-body-stream)))
                          (let [pct (rucore/parse-media-type (get hdrs "Content-Type"))
                                charset (get rumeta/char-sets (:charset pct))
                                changelog (rucore/read-res pct resp-body-stream charset)]
                            (is (not (nil? changelog)))
                            (is (= (count (get changelog "_embedded")) expected-num-entities))))))))]
          (get-changelog (t/now) 304 0)
          (is (not (nil? loaded-user-ent)))))
      ;; doing a light login
      (let [user {"user/username-or-email" "smithka@testing.com"
                  "user/password" "insecure"}
            req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                            (usermeta/mt-subtype-user config/r-mt-subtype-prefix)
                                            usermeta/v001
                                            "UTF-8;q=1,ISO-8859-1;q=0"
                                            "json"
                                            "en-US"
                                            :post
                                            core/light-login-uri-template)
                    (mock/body (json/write-str user))
                    (mock/content-type (rucore/content-type rumeta/mt-type
                                                            (usermeta/mt-subtype-user config/r-mt-subtype-prefix)
                                                            usermeta/v001
                                                            "json"
                                                            "UTF-8")))
            resp (endpoint/riker-app req)]
        (testing "status code" (is (= 204 (:status resp))))))))

#_(deftest integration-test-2
  (testing "Login of user and subsequent authenticated request."
    (is (nil? (usercore/load-user-by-email config/db-spec "smithka@testing.com")))
    (is (nil? (usercore/load-user-by-username config/db-spec "smithk")))
    (let [user {:user/name "Karen Smith"
                :user/email "smithka@testing.com"
                :user/password "insecure"
                :user/is-payment-past-due false}]
      (usercore/save-new-user config/db-spec
                              (usercore/next-user-account-id config/db-spec)
                              user)
      (is (not (nil? (usercore/load-user-by-email config/db-spec "smithka@testing.com")))))
    (let [user {"user/username-or-email" "smithka@testing.com"
                "user/password" "insecure"}
          req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                          (usermeta/mt-subtype-user config/r-mt-subtype-prefix)
                                          usermeta/v001
                                          "UTF-8;q=1,ISO-8859-1;q=0"
                                          "json"
                                          "en-US"
                                          :post
                                          core/login-uri-template)
                  (mock/body (json/write-str user))
                  (mock/content-type (rucore/content-type rumeta/mt-type
                                                          (usermeta/mt-subtype-user config/r-mt-subtype-prefix)
                                                          usermeta/v001
                                                          "json"
                                                          "UTF-8")))
          resp (endpoint/riker-app req)]
      (testing "status code" (is (= 200 (:status resp))))
      (let [hdrs (:headers resp)
            resp-body-stream (:body resp)
            user-location-str (get hdrs "location")
            resp-user-entid-str (rtucore/last-url-part user-location-str)
            pct (rucore/parse-media-type (get hdrs "Content-Type"))
            charset (get rumeta/char-sets (:charset pct))
            resp-user (rucore/read-res pct resp-body-stream charset)
            auth-token (get hdrs config/rhdr-auth-token)
            [loaded-user-entid loaded-user-ent] (usercore/load-user-by-authtoken config/db-spec
                                                                                 (Long. resp-user-entid-str)
                                                                                 auth-token)]
        (is (not (nil? loaded-user-ent)))))))

#_(deftest integration-test-3
  (testing "Successful creation of user account."
    (is (nil? (usercore/load-user-by-email config/db-spec "smithka@testing.com")))
    (is (nil? (usercore/load-user-by-username config/db-spec "smithk")))
    (let [user {"user/name" "Karen Smith"
                "user/email" "smithka@testing.com"
                "user/password" "insecure"}
          req (-> (rtucore/req-w-std-hdrs rumeta/mt-type
                                          (usermeta/mt-subtype-user config/r-mt-subtype-prefix)
                                          usermeta/v001
                                          "UTF-8;q=1,ISO-8859-1;q=0"
                                          "json"
                                          "en-US"
                                          :post
                                          core/users-uri-template)
                  (rtucore/header config/rhdr-establish-session "true")
                  (rtucore/header "Cookie" "_gat=1; _ga=GA1.2.232303640.1469477990")
                  (rtucore/header "r-desired-embedded-format" "id-keyed")
                  (mock/body (json/write-str user))
                  (mock/content-type (rucore/content-type rumeta/mt-type
                                                          (usermeta/mt-subtype-user config/r-mt-subtype-prefix)
                                                          usermeta/v001
                                                          "json"
                                                          "UTF-8")))
          resp (endpoint/riker-app req)]
      (testing "status code" (is (= 201 (:status resp))))
      (let [hdrs (:headers resp)
            resp-body-stream (:body resp)
            user-location-str (get hdrs "location")
            resp-user-entid-str (rtucore/last-url-part user-location-str)
            pct (rucore/parse-media-type (get hdrs "Content-Type"))
            charset (get rumeta/char-sets (:charset pct))
            resp-user (rucore/read-res pct resp-body-stream charset)
            auth-token (get hdrs config/rhdr-auth-token)
            [loaded-user-entid loaded-user-ent] (usercore/load-user-by-authtoken config/db-spec
                                                                                 (Long. resp-user-entid-str)
                                                                                 auth-token)]
        (is (not (nil? loaded-user-ent)))))))

#_(deftest integration-test-4
  (testing "Stripe webhooks."
    (is (nil? (usercore/load-user-by-email config/db-spec "smithka@testing.com")))
    (is (nil? (usercore/load-user-by-username config/db-spec "smithk")))
    (let [user {:user/name "Karen Smith"
                :user/email "smithka@testing.com"
                :user/password "insecure"
                :user/stripe-customer-id "cust_abc123"
                :user/paid-enrollment-established-at (t/now)}]
      (usercore/save-new-user config/db-spec
                              (usercore/next-user-account-id config/db-spec)
                              user)
      (is (not (nil? (usercore/load-user-by-email config/db-spec "smithka@testing.com")))))
    (let [stripe-event  {"user/username-or-email" "smithka@testing.com"
                         "user/password" "insecure"}
          req (-> (mock/request :post core/stripe-events-uri-template)
                  (mock/body (slurp (io/resource "test_stripe_webhook_request.json")))
                  (mock/content-type "application/json"))
          resp (endpoint/riker-app req)]
      (testing "status code" (is (= 204 (:status resp)))))))
