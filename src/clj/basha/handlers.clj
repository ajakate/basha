(ns basha.handlers
  (:require
   [basha.middleware :as middleware]
   [basha.lists :as list]
   [ring.util.http-response :as response]
   [basha.auth :as auth]))

; AUTH

; TODO: try catch wrapper
(defn signup [{{:keys [username password]} :body-params}]
  (try
    (auth/create-user! username password)
    (response/ok
     {:message
      "User registration successful. Please log in."})
    (catch clojure.lang.ExceptionInfo e
      (response/ok
       {:message
        (str "something happened: " e)}))))

(defn login [{{:keys [username password]} :body-params}]
  (try
    (response/ok
     (auth/login username password))
    (catch clojure.lang.ExceptionInfo e
      (response/ok
       {:message
        (str "something happened: " e)}))))

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