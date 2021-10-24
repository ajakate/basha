; TODO clean this imports
(ns basha.lists
  (:require
   [buddy.auth :refer [authenticated?]]
   [buddy.hashers :as hashers]
   [next.jdbc :as jdbc]
   [buddy.auth.backends :as backends]
   [buddy.auth.middleware :refer [wrap-authentication]]
   [basha.db.core :as db]
   [buddy.sign.jwt :as jwt]
   [camel-snake-kebab.core :as csk]
   [java-time :as t]
   [basha.config :refer [env]]))


; ADD cljc validation
(defn create! [params]
  (db/create-list!* params)
)
