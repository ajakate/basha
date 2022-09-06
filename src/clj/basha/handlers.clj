(ns basha.handlers
  (:require
   [basha.lists :as list]
   [basha.decks :as deck]
   [basha.translations :as translation]
   [ring.util.http-response :as response]
   [basha.auth :as auth]
   [basha.invites :as invite]
   [basha.info :as info] 
   [basha.backup :as backup]))

; AUTH

(def error-types {:conflict response/conflict
                  :bad-request response/bad-request
                  :not-found response/not-found})

(defmacro with-handle [& exp]
  `(try
     (response/ok (do ~@exp))
     (catch clojure.lang.ExceptionInfo ~'e
       (((-> ~'e ex-data :type) error-types) {:message (.getMessage ~'e)}))))

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

(defn create-list [{{:keys [id]} :identity {:keys [file name source_language target_language has_latin_script]} :params}]
  (with-handle (list/create!
                name
                source_language
                target_language
                has_latin_script
                (:tempfile file) id)))

(defn get-lists [{{:keys [id]} :identity}]
  (with-handle (list/get-summary id)))

(defn delete-list [{{:keys [id]} :body-params}]
  (with-handle (list/delete-list id)))

(defn delete-translation [{{:keys [id]} :body-params}]
  (with-handle (translation/delete-translation id)))

(defn get-list [{{:keys [id]} :path-params}]
  (with-handle (list/fetch id)))

(defn get-translation [{{:keys [id]} :path-params}]
  (with-handle (translation/fetch id)))

(defn edit-translation [{{:keys [id]} :identity body-params :params path-params :path-params}]
  (let [t-id (:id path-params)]
    (with-handle (translation/update-translation t-id id body-params))))

(defn delete-audio [{{:keys [id]} :path-params}]
  (with-handle (translation/delete-audio id)))

; TODO: return value?
(defn create-deck [{{:keys [id]} :path-params}]
  (deck/create id)
  (response/ok {:ok "hi"}))

(defn fetch-deck [{{:keys [id]} :path-params}]
  (with-handle (deck/fetch id)))

(defn fetch-invite [{{:keys [code]} :path-params}]
  (with-handle
    (let [response (invite/fetch code)]
      (assoc response :code code))))

(defn create-share [{{:keys [list_id user_id]} :body-params}]
  (with-handle (invite/create-share user_id list_id)))

(defn fetch-info [_]
  (with-handle (info/fetch)))

(defn backup [_]
  (with-handle (backup/backup)))

(defn restore [{{:keys [file]} :params}]
  (with-handle (backup/restore (:tempfile file))))
