; TODO clean this imports
(ns basha.lists
  (:require
   [basha.db.core :as db]
   [camel-snake-kebab.core :as csk]
   [java-time :as t]
   [basha.config :refer [env]]))


; ADD cljc validation
(defn create! [params]
  (db/create-list!* params)
)
