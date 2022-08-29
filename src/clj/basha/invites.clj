(ns basha.invites
  (:require
   [basha.util :refer [decode-uuid]]
   [basha.db.core :as db]
   [honey.sql :as sql]))

(defn fetch [code]
  (let [id (java.util.UUID/fromString (decode-uuid code))]
    (db/execute-one
     (sql/format
      {:select [:l.name
                :l.id
                :u.username
                :l.user_id
                :l.target_language
                [[:string_agg  [:cast :li.user_id :text] ","] :users]]
       :from [[:lists :l]]
       :join [[:users :u] [:= :u.id :l.user_id]]
       :full-join [[:list_users :li] [:= :li.list_id :l.id]]
       :where [:= id :l.id]
       :group-by [:l.id :u.username]}))))

(defn fetch-existing-share [u_id l_id]
  (db/execute
   (sql/format
    {:select [:*]
     :from [:list_users]
     :where [:and [:= :list_id l_id] [:= :user_id u_id]]})))

;; TODOO: don't create if it's the owner
(defn create-share [user_id list_id]
  (let [user_uid (java.util.UUID/fromString user_id)
        list_uid (java.util.UUID/fromString list_id)
        existing (fetch-existing-share user_uid list_uid)]
    (if (< 0 (count existing))
      {}
      (db/execute-one
       (sql/format
        {:insert-into :list_users
         :columns [:user_id :list_id]
         :values [[user_uid list_uid]]
         :returning :*})))))
