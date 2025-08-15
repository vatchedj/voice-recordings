(ns voice-recordings.common
  (:require
    [reitit.frontend :as reitit]))

(def router
  (reitit/router
    [["/" :initiate-call]
     ["/recordings/:recording-uuid" :recording]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

(defn get-anti-forgery-token []
  (-> js/document
      (.getElementById "__anti-forgery-token")
      (.getAttribute "data-token")))
