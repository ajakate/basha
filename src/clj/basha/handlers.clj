(ns basha.handlers
  (:require
   [basha.lists :as list]
   [basha.sentences :as sentence]
   [ring.util.http-response :as response]
   [amazonica.aws.s3 :as s3]
   [basha.auth :as auth]
   [basha.config :refer [env]]))

; AUTH

(def error-types {:conflict response/conflict
                  :bad-request response/bad-request})

(defonce cred {:access-key (:aws-access-key-id env)
           :secret-key (:aws-secret-access-key env)
           :endpoint   "us-east-1"})

(defmacro with-handle [& exp]
  `(try
     (response/ok (do ~@exp))
     (catch clojure.lang.ExceptionInfo ~'e
       (((-> ~'e ex-data :type) error-types) {:message (.getMessage ~'e)}))))

(defn signup [{{:keys [username password]} :body-params}]
  (with-handle
    (auth/create-user! username password)
    {:message "try logging in"}))

(defn login [{{:keys [username password]} :body-params}]
  (with-handle
    (auth/login username password)))

(defn refresh [{{:keys [username]} :identity}]
  (with-handle
    (auth/refresh username)))

; LISTS

(defn create-list [{{:keys [id]} :identity {:keys [file name source_language target_language]} :params}]
  (list/create!
   name
   source_language
   target_language
   (:tempfile file) id)
  (response/ok {:hi (:filename file) :poo id}))

; SENTENCES

(defn create-sentence [{:keys [body-params identity]}]
  (let [id (:id identity)]
    (response/ok
     (sentence/create! (assoc body-params :creator_id (java.util.UUID/fromString id))))))

(defn upload-audio [{{:keys [file sentence_id]} :params}]
  (sentence/upload-audio-for-sentence! sentence_id
                                      (:tempfile file))
  (response/ok
   (assoc file :sentence_id sentence_id)))

(defn upload-list [{{:keys [file list_id]} :params}]
  (response/ok
   (assoc file :sentence_id list_id)))
