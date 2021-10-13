(ns basha.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[basha started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[basha has shut down successfully]=-"))
   :middleware identity})
