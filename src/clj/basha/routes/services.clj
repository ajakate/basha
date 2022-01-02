(ns basha.routes.services
  (:require
   [basha.middleware :as middleware]
   [ring.util.http-response :as response]
   [basha.auth :refer [wrap-jwt-authentication auth-middleware]]
   [reitit.ring.middleware.multipart :as multipart]
   [basha.handlers :as handle]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [schema.core :as s]))

(defn service-routes []
  ["/api"
   {:middleware [middleware/wrap-formats]}
   ["/signup" {:post {:parameters {:body {:username s/Str :password s/Str}}
                      :handler handle/signup}}]
   ["/login" {:post {:parameters {:body {:username s/Str :password s/Str}}
                     :handler handle/login}}]
   ["/refresh" {:post {:middleware [wrap-jwt-authentication auth-middleware]
                       :handler handle/refresh}}]
   ["/lists" {:post {:middleware [wrap-jwt-authentication auth-middleware wrap-params wrap-multipart-params]
                     :parameters {:multipart {:file multipart/temp-file-part
                                              :name s/Str
                                              :source_language s/Str
                                              :target_language s/Str}}
                     :handler handle/create-list}
              :get {:middleware [wrap-jwt-authentication auth-middleware]
                    :handler handle/get-lists}}]
   ["/lists/:id" {:get {:middleware [wrap-jwt-authentication auth-middleware]
                        :handler handle/get-list}}]
   ["/translations/:id" {:get {:middleware [wrap-jwt-authentication auth-middleware]
                               :handler handle/get-translation}
                         :post {:middleware [wrap-jwt-authentication auth-middleware wrap-params wrap-multipart-params]
                                :handler handle/edit-translation}}]
   ["/assignees/:id" {:post {:middleware [wrap-jwt-authentication auth-middleware]
                             :handler handle/update-users}}]])
