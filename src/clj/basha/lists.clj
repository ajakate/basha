(ns basha.lists
  (:require
   [basha.db.core :as db]
   [next.jdbc :as jdbc]
   [honey.sql :as sql]
   [clojure.string :as str]))

(defn format-list [resp]
  (let [name (-> resp first :name)
        source_language (-> resp first :source_language)
        target_language (-> resp first :target_language)
        creator (-> resp first :creator)
        users (-> resp first :users)]
    {:id (-> resp first :list_id)
     :name name
     :source_language source_language
     :target_language target_language
     :creator creator
     :users users
     :translations (for [r resp]
                     {:id (:translation_id r)
                      :source_text (:source_text r)
                      :target_text (:target_text r)
                      :target_text_roman (:target_text_roman r)
                      :translator (:translator r)
                      :list_index (:list_index r)
                      :has_audio (:has_audio r)})}))

(defn create-list [name source target user_id sentences]
  (db/execute-one
   (sql/format
    {:with [[:created_list {:insert-into :lists
                            :columns [:name :source_language :target_language :user_id]
                            :values [[name source target user_id]]
                            :returning :id}]]
     :insert-into :translations
     :columns [:source_text :list_id :list_index]
     :values (map-indexed (fn [idx s] [s {:select :id :from :created_list} idx]) sentences)
     :returning :*})))


; ADD cljc validation
(defn create! [name source target file user_id]
  (let [sentences (-> file slurp str/split-lines)]
    (create-list name source target (java.util.UUID/fromString user_id) sentences)))

(defn get-summary [id]
  (let [incomplete-filter [:filter [:count :t.id] {:where [:or [:is :audio :null] [:is :target_text_roman :null]]}]]
    (db/execute
     (sql/format
      {:select [:l.id
                :l.name
                :l.source_language
                :l.target_language
                [{:select :username :from :users :where [:= :users.id :l.user_id]} :creator]
                [[:count :t.id] :list_count]
                [incomplete-filter :open_count]
                [{:select [[[:string_agg :uu.username ","]]]
                  :from [[:users :uu]]
                  :join [[:list_users :lluu] [:= :lluu.user_id :uu.id]]
                  :where [:= :lluu.list_id :l.id]}
                 :users]]
       :from [[:lists :l]]
       :join [[:translations :t] [:= :t.list_id :l.id]]
       :full-join [[:list_users :li] [:= :li.list_id :l.id]]
       :where [:or [:= :l.user_id (java.util.UUID/fromString id)] [:= :li.user_id (java.util.UUID/fromString id)]]
       :group-by [:l.id]
       :order-by [[incomplete-filter :asc] [:l.created_at :asc]]}))))

(defn fetch [id]
  (format-list
   (db/execute
    (sql/format
     {:select [:*
               [:t.id :translation_id]
               [:l.id :list_id]
               [:u.username :creator]
               [{:select :username
                 :from :users
                 :where [:= :t.translator_id :users.id]} :translator]
               [{:select [[[:string_agg :uu.username ","]]]
                 :from [[:users :uu]]
                 :join [[:list_users :lluu] [:= :lluu.user_id :uu.id]]
                 :where [:= :lluu.list_id (java.util.UUID/fromString id)]} :users]
               [[:is :t.audio [:not :null]] :has_audio]]
      :from [[:translations :t]]
      :join [[:lists :l] [:= :l.id :t.list_id]
             [:users :u] [:= :l.user_id :u.id]]
      :where [:= :l.id (java.util.UUID/fromString id)]
      :order-by [[[:is :target_text_roman :null] :desc]
                 [[:is :target_text :null] :desc]
                 [:t.list_index :asc]]}))))

(defn delete-list [id]
  (db/execute-one
   (sql/format
    {:with [[:delete_list {:delete-from :lists
                           :where [:= :id (java.util.UUID/fromString id)]
                           :returning [:id]}]
            [:delete_translations {:delete-from :translations
                                   :where [:in :list_id {:select [:*]
                                                         :from :delete_list}]
                                   :returning :translations.list_id}]]
     :delete-from :list_users
     :where [:in :list_id {:select [:*] :from :delete_translations}]}))
  )

(defn list-errors [tried actual]
  (reduce
   (fn [es elem]
     (if (or (contains? actual elem) (= "" elem))
       es
       (conj es elem)))
   []
   tried))

(defn get-users-by-name [users]
  (db/execute
   (sql/format
    {:select [:id :username]
     :from :users
     :where [:in :username users]})))

(defn update-users [id users]
  (let [id (java.util.UUID/fromString id)
        new (get-users-by-name users)
        new-names (set (map #(:username %) new))
        new-ids (set (map #(:id %) new))
        errors (list-errors users new-names)]
    (if (seq errors)
      {:error {:message (str "The following users could not be found: " (str/join ", " errors))}}
      (db/execute-one
       (sql/format
        {:with [[:noop {:delete-from :list_users
                        :where [:= :list_id id]}]]
         :insert-into :list_users
         :columns [:user_id :list_id]
         :values (map (fn [e] [e id]) new-ids)
         :returning :*})))))
