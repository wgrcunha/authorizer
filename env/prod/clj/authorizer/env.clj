(ns authorizer.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[authorizer started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[authorizer has shut down successfully]=-"))
   :middleware identity})
