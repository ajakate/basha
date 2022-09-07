(ns basha.backup
  (:require
   [basha.config :refer [env]]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.java.io :as io])
  (:import (java.io FileInputStream)))

(defn shell-cmd [cmd]
  (apply shell/sh (str/split cmd #" ")))

(defn backup []
  (let [filepath "./temp_decks/db.dump"]
    (shell-cmd (str "rm -f " filepath))
    (shell-cmd (str "bash bin/dump_db.sh " (:database-url env) " " filepath))
    (FileInputStream. (str "./temp_decks/db.dump"))))

(defn restore [file]
  (let [filepath "./temp_decks/restore.dump"]
    (shell-cmd (str "rm -f " filepath))
    (io/copy file (io/file filepath))
    (shell-cmd (str "bash bin/restore_db.sh " (:database-url env) " " filepath))
    {:okay :success}))
