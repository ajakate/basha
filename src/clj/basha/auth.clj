(ns basha.auth
  (:require
   [buddy.hashers :as hashers]
   [next.jdbc :as jdbc]
   [basha.db.core :as db]))

(defn create-user! [login password]
  ; TODO: remvoe db/db?
  (jdbc/with-transaction [t-conn db/*db*]
    (db/create-user!* t-conn
                      {:username    login
                       :pass (hashers/derive password)})))
