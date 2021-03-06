(ns basha.decks
  (:require
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [basha.config :refer [env]])
  (:import (java.io FileInputStream)))

(defn db-url [] (:database-url env))

(defn shell-cmd [cmd]
  (apply shell/sh (str/split cmd #" ")))

(defn create [list-id]
  (shell-cmd (str "rm -f ./temp_decks/" list-id ".apkg"))
  (shell-cmd (str "rm -f ./temp_decks/" list-id ".fail"))
  (shell-cmd (str "touch ./temp_decks/" list-id ".pending"))
  (future (shell-cmd (str "python create_deck.py " list-id " " (db-url)))))

(defn find-file [raw-string id]
  (->> (clojure.string/split raw-string #"\s")
       (filter #(clojure.string/includes? % id))
       first))

(defn fetch [list-id]
  (let [raw (shell-cmd (str "ls ./temp_decks" ))
        filename (find-file (:out raw) list-id)]
    (cond
      (str/includes? filename ".pending")  (throw (ex-info "Deck pending" {:type :not-found}))
      (str/includes? filename ".apkg") (FileInputStream. (str "./temp_decks/" filename))
      (str/includes? filename ".fail") (throw (ex-info (slurp (str "./temp_decks/" filename)) {:type :bad-request}))
      :else (throw (ex-info "Something bad happened..." {:type :bad-request})))))
