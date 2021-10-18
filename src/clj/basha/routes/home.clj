(ns basha.routes.home
  (:require
   [basha.layout :as layout]
   [basha.db.core :as db]
   [clojure.java.io :as io]
   [basha.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [basha.auth :as auth]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]
   ["/signup" {:post (fn [{{:keys [user pass]} :body-params}]
                       (try
                         (auth/create-user! user pass)
                         (response/ok
                          {:message
                           "User registration successful. Please log in."})
                         (catch clojure.lang.ExceptionInfo e
                           (response/ok
                            {:message
                             (str "something happened: " e)}))))}]])
