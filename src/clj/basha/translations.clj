; TODO clean this imports
(ns basha.translations
  (:require
   [basha.db.core :as db]
   [camel-snake-kebab.core :as csk]
   [java-time :as t]
   [amazonica.aws.s3 :as s3]
   [basha.auth :as auth]
   [basha.files :as files]
   [clojure.java.io :as io]
   [basha.config :refer [env]]))

(defn file2bytes [path]
  (with-open [in (io/input-stream path)
              out (java.io.ByteArrayOutputStream.)]
    (io/copy in out)
    (.toByteArray out)))

(defonce cred {:access-key (:aws-access-key-id env)
               :secret-key (:aws-secret-access-key env)
               :endpoint   "us-east-1"})

(defn upload-audio-for-sentence! [sentence_id file]
  (if-let [sentence (db/get-sentence-by-id {:id sentence_id})]
    (let [audio-id (.toString (java.util.UUID/randomUUID))]
      (files/save-file (str audio-id ".mp3") file)
      (db/update-sentence-audio!* {:audio-link audio-id :id (:id sentence)}))
    (throw (ex-info "no sentence found for audio" {:type :bad-request}))))

(defn fetch [id]
  (db/get-translation {:id (java.util.UUID/fromString id)}))

(defn update [id user-id params]
  (let [audio (-> params :audio :tempfile)
        params (dissoc params :audio)]
    (db/update-translation (assoc params
                                  :id (java.util.UUID/fromString id)
                                  :translator_id (java.util.UUID/fromString user-id)
                                  :audio (if audio (file2bytes audio) nil)))))

(defn delete-audio [id]
  (db/delete-audio-for-translation {:id (java.util.UUID/fromString id)}))
