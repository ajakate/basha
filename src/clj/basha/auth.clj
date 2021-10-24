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

;; (defonce token-secret (select-keys env [:token-secret]))
(def backend (backends/jws {:secret "token-secret"}))

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
  ; TODO: remvoe db/db?
  ; TODO: add logic to check for existing user
  (jdbc/with-transaction [t-conn db/*db*]
    (db/create-user!* t-conn
                      {:username    username
                       :password (hashers/derive password)})))

(defn generate-token [payload time-interval]
  (jwt/sign payload "token-secret"
            {:exp   (t/plus (t/instant) time-interval)}))

(defn new-tokens [user]
  {:access-token (generate-token user (t/hours 1))
   :refresh-token (generate-token user (t/days 1))}
  )

(defn login [username password]
  (let [user (db/get-user-for-login {:username username})
        authenticated (hashers/check password (:password user))]
    (if authenticated
      (new-tokens (dissoc user :password))
      {})))

(defn refresh [username token]
  (let [unsigned (jwt/unsign token "token-secret")]
    (if (= (:username unsigned) username)
      (new-tokens username)
      {})))
