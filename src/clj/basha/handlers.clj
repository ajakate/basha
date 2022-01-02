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
       (((-> ~'e ex-data :type) error-types) {:message (.getMessage ~'e)}))
     ;; TODO: add this back when done?
    ;;  (catch Exception ~'e
    ;;    (response/internal-server-error {:message (.getMessage ~'e)}))

     ))

(defn signup [{{:keys [username password]} :body-params}]
  (with-handle
    (auth/create-user! username password)
    (auth/login username password)))

(defn login [{{:keys [username password]} :body-params}]
  (with-handle
    (auth/login username password)))

(defn refresh [{{:keys [username]} :identity}]
  (with-handle
    (auth/refresh username)))

; LISTS

(defn create-list [{{:keys [id]} :identity {:keys [file name source_language target_language]} :params}]
  (with-handle (list/create!
                name
                source_language
                target_language
                (:tempfile file) id)))

(defn get-lists [{{:keys [id]} :identity}]
  (with-handle (list/get-summary id)))

(defn get-list [{{:keys [id]} :path-params}]
  (with-handle (list/fetch id)))

(defn get-translation [{{:keys [id]} :path-params}]
  (with-handle (translation/fetch id)))

(defn edit-translation [{{:keys [id]} :identity body-params :params path-params :path-params}]
  (let [t-id (:id path-params)]
    (with-handle (translation/update t-id id body-params))))

; TODO: why does with-handle not work here?
(defn update-users [{{:keys [id]} :path-params body-params :params}]
  (let [resp (list/update-users id (:users body-params))]
    (if (:error resp)
      (response/bad-request resp)
      (response/ok resp))))
