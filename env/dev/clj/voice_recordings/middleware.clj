(ns voice-recordings.middleware
  (:require
   [ring.middleware.anti-forgery :as af]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.params :refer [wrap-params]]
   [prone.middleware :refer [wrap-exceptions]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(defn request-token-with-exemptions
  "Returns nil for exception endpoints, to not enforce
  anti-forgery token. Otherwise, runs the default logic."
  [request]

  (println "request-method:" (:request-method request)
           "uri:" (:uri request))

  (when-not (and (= (:request-method request) :post)
                 (= (:uri request) "/api/recording-status-callback"))
    (#'af/default-request-token request)))

(def options
  (assoc-in
    site-defaults
    [:security :anti-forgery]
    #_{:read-token request-token-with-exemptions}
    false))

(def middleware
  [#(wrap-defaults % options)
   wrap-exceptions
   wrap-reload])
