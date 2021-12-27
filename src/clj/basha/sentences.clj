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

; TODO: delete this file

(defonce cred {:access-key (:aws-access-key-id env)
               :secret-key (:aws-secret-access-key env)
               :endpoint   "us-east-1"})

;; (defn format-sentence [resp]
;;   (first resp))

; ADD cljc validation
(defn create! [params]
  (println params)
  (let [new-sentence (db/create-sentence!* params)]
    (if-let [source_id (:source_id params)]
      (db/create-translation!* {:source_id (java.util.UUID/fromString source_id)
                                :target_id (:id new-sentence)})
      new-sentence)))

(defn upload-audio-for-sentence! [sentence_id file]
  (if-let [sentence (db/get-sentence-by-id {:id sentence_id})]
    (let [audio-id (.toString (java.util.UUID/randomUUID))]
      (files/save-file (str audio-id ".mp3") file)
      (db/update-sentence-audio!* {:audio-link audio-id :id (:id sentence)}))
    (throw (ex-info "no sentence found for audio" {:type :bad-request}))))

;; TODO: add translation logic
(defn get [id]
  (db/get-sentence {:id (java.util.UUID/fromString id)}))
