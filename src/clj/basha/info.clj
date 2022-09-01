(ns basha.info
  (:require
   [basha.db.core :as db]
   [honey.sql :as sql]))

;; (defn fetch []
;;   (db/execute-one
;;    (sql/format
;;     {:select [[:username :admin]
;;               [{:select [[[:count :*]]] :from :users} :total_users]]
;;      :from :users
;;      :order-by [[:created_at :asc]]
;;      :limit 1})))


(defn fetch []
  (let [response (db/execute-one
                  (sql/format
                   {:select [[:username :admin]
                             [{:select [[[:count :*]]] :from :users} :total_users]]
                    :from :users
                    :order-by [[:created_at :asc]]
                    :limit 1}))]
    (if (seq response)
      response
      {:total_users 0})))