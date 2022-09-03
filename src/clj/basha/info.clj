(ns basha.info
  (:require
   [basha.db.core :as db]
   [honey.sql :as sql]))

(defn fetch []
  (db/execute-one
   (sql/format
    {:select [[{:select :username :from :users :order-by [[:created_at :asc]] :limit 1} :username]
              [{:select [[[:count :*]]] :from :users} :total_users]]})))
