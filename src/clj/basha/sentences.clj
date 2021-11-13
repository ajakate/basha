; TODO clean this imports
(ns basha.sentences
  (:require
   [basha.db.core :as db]
   [camel-snake-kebab.core :as csk]
   [java-time :as t]
   [amazonica.aws.s3 :as s3]
   [basha.auth :as auth]
   [basha.files :as files]
   [basha.config :refer [env]]))

(defonce cred {:access-key (:aws-access-key-id env)
               :secret-key (:aws-secret-access-key env)
               :endpoint   "us-east-1"})

; ADD cljc validation
(defn create! [params]
  (let [new-sentence (db/create-sentence!* params)]
    (if-let [source_id (:sentence_id params)]
      (db/create-translation!* {:source_id (java.util.UUID/fromString source_id)
                                :target_id (:id new-sentence)})
      new-sentence)))

(defn upload-audio-for-sentence! [sentence_id file]
  (if-let [sentence (db/get-sentence-by-id {:id sentence_id})]
    (let [audio-id (.toString (java.util.UUID/randomUUID))]
      (files/save-file (str audio-id ".mp3") file)
      (db/update-sentence-audio!* {:audio-link audio-id :id (:id sentence)}))
    (throw (ex-info "no sentence found for audio" {:type :bad-request}))))

(comment
  
  (def myd (java.util.UUID/fromString "96e5de37-a729-4151-a6c3-cf0af4044e66"))
  (create! {:text "hi" :language "EN" :creator_id myd})

  
  )
