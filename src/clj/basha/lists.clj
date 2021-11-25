; TODO clean this imports
(ns basha.lists
  (:require
   [basha.db.core :as db]
   [next.jdbc :as jdbc]
   [camel-snake-kebab.core :as csk]
   [java-time :as t]
   [basha.config :refer [env]]
   [cprop.source :as source]))

(defn format-sentences [sentences creator language]
  (map (fn [s] [s creator language]) sentences))

(defn create-sentences [sentences user source conn]
  (map (fn [s]
         (:id (db/create-sentence!* conn {:text s :creator_id user :language source})))
       sentences))

(defn create-list-items [sentence_ids list_id]
                   (map (fn [s]
                          (db/create-list-item!* {:sentence_id s :list_id list_id}))
                        sentence_ids))

; ADD cljc validation
(defn create! [name source target file user_id]
  (jdbc/with-transaction [t-conn db/*db*]
    (let [sentences (-> file slurp clojure.string/split-lines)
          list_id (db/create-list!* t-conn
                                    {:name name
                                     :source_language source
                                     :target_language target
                                     :user_id (java.util.UUID/fromString user_id)})
          sentence_ids (create-sentences sentences (java.util.UUID/fromString user_id) source t-conn)]
      (create-list-items sentence_ids list_id))))
