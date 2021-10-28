; TODO clean this imports
(ns basha.sentences
  (:require
   [basha.db.core :as db]
   [camel-snake-kebab.core :as csk]
   [java-time :as t]
   [basha.config :refer [env]]))


; ADD cljc validation
(defn create! [params]
  (db/create-sentence!* params))

(comment
  
  (def myd (java.util.UUID/fromString "96e5de37-a729-4151-a6c3-cf0af4044e66"))
  (create! {:text "hi" :language "EN" :creator_id myd})

  
  )
