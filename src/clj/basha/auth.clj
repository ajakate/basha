(ns basha.auth
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.hashers :as hashers]
   [next.jdbc :as jdbc]
   [buddy.auth.backends :as backends]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [basha.db.core :as db]
   [buddy.sign.jwt :as jwt]
   [java-time :as t]
   [basha.config :refer [env]]))

; TODO: fix this
(defonce token-secret  "secret")

(def backend (backends/jws {:secret token-secret}))

(defn wrap-jwt-authentication
  [handler]
  (wrap-authentication handler backend))

(defn auth-middleware
  [handler]
  (fn [request]
    (if (authenticated? request)
      (handler request)
      {:status 401 :body {:error "Unauthorized"}})))

(defn create-user! [username password]
  (let [user (db/get-user-for-login {:username username})]
    (if user
      (throw (ex-info "User already exists" {:type :conflict}))
      (db/create-user!* {:username    username
                         :password (hashers/derive password)}))))

(defn generate-token [payload time-interval]
  (jwt/sign payload token-secret
            {:exp   (t/plus (t/instant) time-interval)}))

(defn new-tokens [user]
  {:access-token (generate-token user (t/hours 2))
   :refresh-token (generate-token user (t/days 5))})

(defn login [username password]
  (let [user (db/get-user-for-login {:username username})
        authenticated (hashers/check password (:password user))]
    (if authenticated
      (assoc (new-tokens (dissoc user :password)) :username username)
      (throw (ex-info "Wrong username or password entered" {:type :bad-request})))))

(defn refresh [username]
  (let [user (db/get-user-for-login {:username username})]
    (assoc (new-tokens (dissoc user :password)) :username username)))
