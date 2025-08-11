(ns voice-recordings.server
    (:require
     [config.core :refer [env]]
     [ring.adapter.jetty :refer [run-jetty]]
     [voice-recordings.handler :refer [app]])
    (:gen-class))

(defn -main [& args]
  (let [port (or (env :port) 3000)]
    (run-jetty #'app {:port port :join? false})))
