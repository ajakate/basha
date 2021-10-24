(ns basha.handler-test
  (:require
   [clojure.test :refer :all]
   [basha.db.core :refer [*db*] :as db]
   [ring.mock.request :refer :all]
   [basha.handler :refer :all]
   [basha.middleware.formats :as formats]
   [muuntaja.core :as m]
   [luminus-migrations.core :as migrations]
   [basha.config :refer [env]]
   [next.jdbc :as jdbc]
   [mount.core :as mount]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'basha.config/env
     #'basha.db.core/*db*
     #'basha.handler/app-routes)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    ;; TODO: refactor this
    (jdbc/with-transaction [t-conn *db* {:rollback-only false}]
      (jdbc/execute! t-conn ["truncate users;"]))
    (f)
    (jdbc/with-transaction [t-conn *db* {:rollback-only false}]
      (jdbc/execute! t-conn ["truncate users;"]))))

(deftest test-app
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "signup"
    (let [signup-response ((app) (-> (request :post "/api/signup")

                                     (json-body {:username "user" :password "pass"})))
          login-response ((app) (-> (request :post "/api/login")
                                    (json-body {:username "user" :password "pass"})))
          login-body (-> login-response :body parse-json)
          refresh-token (:refresh-token login-body)
          refresh-response ((app) (-> (request :post "/api/refresh")
                                    (json-body {:username "user" :refresh-token refresh-token})))
          refresh-body (-> refresh-response :body parse-json)
          ]
      (is (= 200 (:status signup-response)))
      (is (= 200 (:status login-response)))
      (is (= 200 (:status refresh-response)))
      (is (seq (:access-token login-body)))
      (is (seq (:refresh-token login-body)))
      (is (seq (:refresh-token refresh-body)))
      (is (seq (:access-token refresh-body))))))
