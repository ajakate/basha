(ns basha.auth
  (:require
   [buddy.hashers :as hashers]
   [next.jdbc :as jdbc]
   [basha.db.core :as db]
   [buddy.sign.jwt :as jwt]
   [basha.config :refer [env]]))

;; (defonce token-secret (select-keys env [:token-secret]))

(defn create-user! [login password]
  ; TODO: remvoe db/db?
  (jdbc/with-transaction [t-conn db/*db*]
    (db/create-user!* t-conn
                      {:username    login
                       :pass (hashers/derive password)})))

(defn login [login password]
  (let [user (db/get-user-for-login {:username login})
        authenticated (hashers/check password (:pass user))]
    (if authenticated
      {:access-token (jwt/sign {:user login} "token-secret")}
      {})))
