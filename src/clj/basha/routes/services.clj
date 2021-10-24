(ns basha.routes.services)

(ns basha.routes.services
  (:require
   [basha.middleware :as middleware]
   [ring.util.http-response :as response]
   [basha.auth :as auth]
   [basha.handlers :as handle]
   [schema.core :as s]))

(defn service-routes []
  ["/api"
   {:middleware [middleware/wrap-formats]}
   ["/signup" {:post {:parameters {:body {:username s/Str :password s/Str}}
                      :handler handle/signup}}]
   ["/login" {:post {:parameters {:body {:username s/Str :password s/Str}}
                     :handler handle/login}}]
   ["/refresh" {:post {:parameters {:body {:username s/Str :refresh-token s/Str}}
                       :handler handle/refresh}}]])
