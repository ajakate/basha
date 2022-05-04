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
  (db/get-list-summary {:id (java.util.UUID/fromString id)}))

(defn fetch [id]
  (format-list (db/get-list {:id (java.util.UUID/fromString id)})))

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

(defn update-users [id users]
  (let [id (java.util.UUID/fromString id)
        new (db/get-users-by-username {:users users})
        new-names (set (map #(:username %) new))
        new-ids (set (map #(:id %) new))
        errors (list-errors users new-names)]
    (if (seq errors)
      {:error {:message (str "The following users could not be found: " (str/join ", " errors))}}
      (jdbc/with-transaction
        [t-conn db/*db*]
        (db/delete-list-users t-conn {:list_id id})
        (when (seq new-ids)
          (db/create-list-users  t-conn {:users (map (fn [e] [e id]) new-ids)}))
        {:ok "fine"}))))
