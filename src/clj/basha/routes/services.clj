(ns basha.routes.services)

(ns basha.routes.services
  (:require
   [basha.middleware :as middleware]
   [ring.util.http-response :as response]
   [basha.auth :as auth]))

(defn service-routes []
  ["/api"
   {:middleware [middleware/wrap-formats]}
   ["/signup" {:post (fn [{{:keys [user pass]} :body-params session :session}]
                       (try
                         (auth/create-user! user pass)
                         (response/ok
                          {:message
                           "User registration successful. Please log in."})
                         (catch clojure.lang.ExceptionInfo e
                           (response/ok
                            {:message
                             (str "something happened: " e)}))))}]])
