(ns basha.handlers
  (:require
   [basha.lists :as list]
   [ring.util.http-response :as response]
   [basha.auth :as auth]))

; AUTH

(def error-types {:conflict response/conflict
                  :bad-request response/bad-request})

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


(defn refresh [{{:keys [username refresh-token]} :body-params}]
  (try
    (response/ok
     (auth/refresh username refresh-token))
    (catch clojure.lang.ExceptionInfo e
      (response/ok
       {:message
        (str "something happened: " e)}))))

; LISTS

(defn create-list [{:keys [body-params identity]}]
  (let [id (:id identity)]
    (response/ok
     (list/create! (assoc body-params :user_id (java.util.UUID/fromString id)))))
  )