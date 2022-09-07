(ns basha.info
  (:require
   [basha.db.core :as db]
   [honey.sql :as sql]
   [basha.config :refer [env]]))

(defn fetch []
  (let [init-query [[{:select :username :from :users :order-by [[:created_at :asc]] :limit 1} :admin]
                    [{:select [[[:count :*]]] :from :users} :total_users]]
        render-query [{:select [[[:date_part "day"
                                  {:select
                                   [[[:-
                                      :current_date
                                      {:select :applied
                                       :from :schema_migrations
                                       :order-by [[:applied :asc]]
                                       :limit 1}]]]}]]]}
                      :db_uptime_days]]
    (db/execute-one
     (sql/format
      {:select (if (:render env)
                 (conj init-query render-query)
                 init-query)}))))
