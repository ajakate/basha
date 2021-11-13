(ns basha.files
  (:require
   [basha.db.core :as db]
   [camel-snake-kebab.core :as csk]
   [java-time :as t]
   [amazonica.aws.s3 :as s3]
   [basha.auth :as auth]
   [basha.config :refer [env]]
   [clojure.java.io :as io]))


(defonce cred {:access-key (:aws-access-key-id env)
               :secret-key (:aws-secret-access-key env)
               :endpoint   "us-east-1"})

(defn upload-to-s3 [filename content]
  (s3/put-object cred
                 :bucket-name "basha-mp3-version-1-dev"
                 :key filename
                 :file content))

(defn local-save [filename content]
  (io/copy (io/file content) (io/file (str "./resources/files/dev/" filename))))


(defn save-file [filename content]
  (if (:store-files-in-s3 env)
    (upload-to-s3 filename content)
    (local-save filename content)))
