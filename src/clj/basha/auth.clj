(ns basha.auth
  (:require
   [buddy.hashers :as hashers]
   [next.jdbc :as jdbc]
   [basha.db.core :as db]
   [buddy.sign.jwt :as jwt]
   [java-time :as t]
   [basha.config :refer [env]]))

;; (defonce token-secret (select-keys env [:token-secret]))

(defn create-user! [login password]
  ; TODO: remvoe db/db?
  (jdbc/with-transaction [t-conn db/*db*]
    (db/create-user!* t-conn
                      {:username    login
                       :pass (hashers/derive password)})))

(defn generate-token [payload time-interval]
  (jwt/sign payload "token-secret"
            {:exp   (t/plus (t/instant) time-interval)}))

(defn new-tokens [login]
  {:access-token (generate-token {:user login} (t/hours 1))
   :refresh-token (generate-token {:user login} (t/days 1))}
  )

(defn login [login password]
  (let [user (db/get-user-for-login {:username login})
        authenticated (hashers/check password (:pass user))]
    (if authenticated
      (new-tokens login)
      {})))

(defn refresh [user token]
  (let [unsigned (jwt/unsign token "token-secret")]
    (if (= (:user unsigned) user)
      (new-tokens user)
      {})))
