(ns basha.handlers
  (:require
   [basha.lists :as list]
   [basha.translations :as translation]
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

(defn get-lists [{{:keys [id]} :identity}]
  (with-handle (list/get-summary id)))

(defn get-list [{{:keys [id]} :path-params}]
  (with-handle (list/get id)))

(defn get-translation [{{:keys [id]} :path-params}]
  (with-handle (translation/fetch id)))

(defn edit-translation [{{:keys [id]} :identity body-params :params path-params :path-params}]
  (let [t-id (:id path-params)]
    (with-handle (translation/update t-id id body-params))))


;; (defn upload-audio [{{:keys [file sentence_id]} :params}]
;;   (sentence/upload-audio-for-sentence! sentence_id
;;                                       (:tempfile file))
;;   (response/ok
;;    (assoc file :sentence_id sentence_id)))
