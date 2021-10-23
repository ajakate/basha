(ns basha.auth
  (:require
   [buddy.hashers :as hashers]
   [next.jdbc :as jdbc]
   [basha.db.core :as db]
   [buddy.sign.jwt :as jwt]
   [java-time :as t]
   [basha.config :refer [env]]))

;; (defonce token-secret (select-keys env [:token-secret]))

(defn create-user! [username password]
  ; TODO: remvoe db/db?
  (jdbc/with-transaction [t-conn db/*db*]
    (db/create-user!* t-conn
                      {:username    username
                       :password (hashers/derive password)})))

(defn generate-token [payload time-interval]
  (jwt/sign payload "token-secret"
            {:exp   (t/plus (t/instant) time-interval)}))

(defn new-tokens [username]
  {:access-token (generate-token {:user username} (t/hours 1))
   :refresh-token (generate-token {:user username} (t/days 1))}
  )

(defn login [username password]
  (let [user (db/get-user-for-login {:username username})
        authenticated (hashers/check password (:password user))]
    (if authenticated
      (new-tokens username)
      {})))

(defn refresh [username token]
  (let [unsigned (jwt/unsign token "token-secret")]
    (if (= (:user unsigned) username)
      (new-tokens username)
      {})))
