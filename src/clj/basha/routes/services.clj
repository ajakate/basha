(ns basha.routes.services)

(ns basha.routes.services
  (:require
   [basha.middleware :as middleware]
   [ring.util.http-response :as response]
   [basha.auth :as auth]))

(defn service-routes []
  ["/api"
   {:middleware [middleware/wrap-formats]}
   ["/signup" {:post (fn [{{:keys [username password]} :body-params}]
                       (try
                         (auth/create-user! username password)
                         (response/ok
                          {:message
                           "User registration successful. Please log in."})
                         (catch clojure.lang.ExceptionInfo e
                           (response/ok
                            {:message
                             (str "something happened: " e)}))))}]
   ["/login" {:post (fn [{{:keys [username password]} :body-params}]
                      (try
                        (response/ok
                         (auth/login username password))
                        (catch clojure.lang.ExceptionInfo e
                          (response/ok
                           {:message
                            (str "something happened: " e)}))))}]
   ["/refresh" {:post (fn [{{:keys [username refresh-token]} :body-params}]
                      (try
                        (response/ok
                         (auth/refresh username refresh-token))
                        (catch clojure.lang.ExceptionInfo e
                          (response/ok
                           {:message
                            (str "something happened: " e)}))))}]])
