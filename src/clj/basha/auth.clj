(ns basha.auth
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.hashers :as hashers]
   [buddy.auth.backends :as backends]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [basha.db.core :as db]
   [buddy.sign.jwt :as jwt]
   [java-time :as t]
   [next.jdbc :as jdbc]
   [honey.sql :as sql]
   [next.jdbc.result-set :as rs]
   [basha.config :refer [env]]))

(defn token-secret [] (:token-secret env))

(defn backend [] (backends/jws {:secret (token-secret)}))

(defn wrap-jwt-authentication
  [handler]
  (wrap-authentication handler (backend)))

(defn auth-middleware
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      {:status 401 :body {:error "Unauthorized"}})))

(defn get-user-from-db [username]
  (jdbc/with-transaction
    [conn db/*db*]
    (jdbc/execute-one! conn (sql/format
                             {:select [:username :password :id]
                              :from [:users]
                              :where [:= :username username]})
                       {:builder-fn rs/as-unqualified-lower-maps})))

(defn create-user! [username password]
  (let [user (get-user-from-db username)]
    (if user
      (throw (ex-info "User already exists" {:type :conflict}))
      (jdbc/with-transaction
        [conn db/*db*]
        (jdbc/execute! conn (sql/format
                             {:insert-into :users
                              :columns [:username :password]
                              :values [[username (hashers/derive password)]]}))))))

(defn generate-token [payload time-interval]
  (jwt/sign payload (token-secret)
            {:exp   (t/plus (t/instant) time-interval)}))

(defn new-tokens [user]
  {:access-token (generate-token user (t/hours 2))
   :refresh-token (generate-token user (t/days 5))})

(defn login [username password]
  (let [user (get-user-from-db username)
        authenticated (hashers/check password (:password user))]
    (if authenticated
      (assoc (new-tokens (dissoc user :password)) :username username)
      (throw (ex-info "Wrong username or password entered" {:type :bad-request})))))

(defn refresh [username]
  (let [user (get-user-from-db username)]
    (assoc (new-tokens (dissoc user :password)) :username username)))
