; TODO clean this imports
(ns basha.lists
  (:require
   [basha.db.core :as db]
   [next.jdbc :as jdbc]
   [clojure.string :as str]))

(defn create-translations [sentences list_id conn]
  (map-indexed (fn [idx s]
         (:id (db/create-translation!* conn {:source_text s :list_id list_id :list_index idx})))
       sentences))

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

; ADD cljc validation
(defn create! [name source target file user_id]
  (jdbc/with-transaction [t-conn db/*db*]
    (let [sentences (-> file slurp clojure.string/split-lines)
          list_id (db/create-list!*
                   t-conn
                   {:name name
                    :source_language source
                    :target_language target
                    :user_id (java.util.UUID/fromString user_id)})
          sentence_ids (create-translations sentences (:id list_id) t-conn)]
      ;; TODO: WHY DO I NEED THIS PRINTLN HERE?
      (println sentence_ids)
      {:id list_id})))

(defn get-summary [id]
  (db/get-list-summary {:id (java.util.UUID/fromString id)}))

(defn fetch [id]
  (format-list (db/get-list {:id (java.util.UUID/fromString id)})))

; TODO: cleanup
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
