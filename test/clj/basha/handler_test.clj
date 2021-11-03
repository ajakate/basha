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
    (jdbc/execute! t-conn ["truncate users, lists, sentences;"])))

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
    (f)))

(use-fixtures
  :each
  (fn [f]
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

(deftest signup
  (testing "can't signup with same username"
    (let [signup-response ((app) (-> (request :post "/api/signup")
                                     (json-body {:username "user" :password "pass"})))
          signup2-response ((app) (-> (request :post "/api/signup")
                                      (json-body {:username "user" :password "pass2"})))]
      (is (= 200 (:status signup-response)))
      (is (= 409 (:status signup2-response))))))

(deftest login
  (testing "signup"
    (let [_ (create-user {:username "ajay" :password "pass"})
          login-success ((app) (-> (request :post "/api/login")
                                   (json-body {:username "ajay" :password "pass"})))
          login-fail ((app) (-> (request :post "/api/login")
                                (json-body {:username "ajay" :password "wrong"})))
          login-body (-> login-success :body parse-json)]
      (is (= 200 (:status login-success)))
      (is (seq (:access-token login-body)))
      (is (seq (:refresh-token login-body)))
      (is (= 400 (:status login-fail))))))

(deftest refresh
  (testing "should refresh the token"
    (let [user (create-user {:username "ajay" :password "pass"})
          refresh-response ((app) (-> (request :post "/api/refresh")
                                      (header "Authorization" (str "Token " (:access-token user)))))
          refresh-body (-> refresh-response :body parse-json)]
      (is (= 200 (:status refresh-response)))
      (is (seq (:access-token refresh-body)))
      (is (seq (:refresh-token refresh-body))))))

(deftest lists
  (testing "create a list"
    (let [user (create-user {})
          list-response ((app) (-> (request :post "/api/lists")
                                   (json-body {:name "test" :target_language "oidf" :source_language "ksjdf"})
                                   (header "Authorization" (str "Token " (:access-token user)))))]
      (is (= 200 (:status list-response))))))

(deftest sentences
  (testing "create a sentence and a translation"
    (let [user (create-user {})
          sent-response ((app) (-> (request :post "/api/sentences")
                                   (json-body {:text "Hello" :language "EN"})
                                   (header "Authorization" (str "Token " (:access-token user)))))
          sent-id  (-> sent-response :body parse-json :id)
          trans-response ((app) (-> (request :post "/api/sentences")
                                    (json-body {:text "Hola" :language "ES" :sentence_id sent-id})
                                    (header "Authorization" (str "Token " (:access-token user)))))]
      (is (= 200 (:status sent-response)))
      (is (seq sent-id))
      (is (= 200 (:status trans-response))))))
