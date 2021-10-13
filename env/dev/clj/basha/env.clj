(ns basha.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [basha.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[basha started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[basha has shut down successfully]=-"))
   :middleware wrap-dev})
