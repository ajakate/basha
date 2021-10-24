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

(defn clear-tables []
  (jdbc/with-transaction [t-conn *db* {:rollback-only false}]
    (jdbc/execute! t-conn ["truncate users, lists;"])))

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
    (clear-tables)
    (f)
    (clear-tables)))

(defn create-user [params]
  (let [{:keys [username password] :or {username "user" password "pass"}} params
        _ ((app) (-> (request :post "/api/signup")
                     (json-body {:username username :password password})))
        login-response ((app) (-> (request :post "/api/login")
                                  (json-body {:username username :password password})))
        access-token (-> login-response :body parse-json :access-token)]
    {:username username
     :access-token access-token}))

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
          login-body (-> login-response :body parse-json)]
      (is (= 200 (:status signup-response)))
      (is (= 200 (:status login-response)))
      (is (seq (:access-token login-body)))
      (is (seq (:refresh-token login-body)))))

  (testing "list-create"
    ; TODO: add view list test here
    (let [user (create-user {})
          list-response ((app) (-> (request :post "/api/lists")
                                   (json-body {:name "test" :target_language "oidf" :source_language "ksjdf"})
                                   (header "Authorization" (str "Token " (:access-token user)))))])))
