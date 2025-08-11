(ns voice-recordings.handler
  (:require
    [cheshire.core :as json]
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as string]
    [clojure.tools.logging :as log]
    [config.core :refer [env]]
    [hiccup.page :refer [include-js include-css html5]]
    [reitit.ring :as reitit-ring]
    [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
    [ring.util.response :as response]
    [voice-recordings.middleware :refer [middleware]]
    [voice-recordings.twilio :as twilio]))

; TODO: Use environ????
(def db-spec
  {:dbtype   "postgresql"
   :dbname   (env :postgres-db)
   :user     (env :postgres-user)
   :password (env :postgres-password)
   :host     (env :pghost)
   :port     (env :pgport)})

(log/info "TEST 1")

(def mount-target
  [:div#app
   [:h2 "Welcome to voice-recordings"]
   [:p "please wait while Figwheel/shadow-cljs is waking up ..."]
   [:p "(Check the js console for hints if nothing exciting happens.)"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css (if (env :dev) "/css/foundation.css" "/css/foundation.min.css"))
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
   (head)
   [:body {:class "body-container"}
    [:div#__anti-forgery-token {:data-token *anti-forgery-token*}] ; Add this line
    mount-target
    (include-js "/js/app.js")]))


(defn index-handler
  [_request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (loading-page)})

(defn api-handler [_request]

  (println "api-handler!!!!!!!!!!!!!")
  (log/info "api-handler!!!!!!!!!!!!!")

  (-> {:message "Hello from API"
       :data    [1 2 3 4 5]
       :url     (-> (jdbc/query db-spec ["SELECT * from recording"])
                    first
                    :url)}
      json/generate-string
      response/response
      (response/content-type "application/json")))

(defn initiate-call-handler [request]
  (println "initiate-call-handler request (print):" request)
  (log/debug "initiate-call-handler request (log):" request)

  (let [body (-> request :body slurp (json/parse-string true))
        phone-number (:phone-number body)
        sanitized-phone-number (as-> phone-number $
                                     (string/replace $ #"\-" "")
                                     (str "+1" $))]
    (println "sanitized-phone-number" sanitized-phone-number)

    (twilio/make-call sanitized-phone-number)

    (response/created "RESPONSE!!!")
    ))

(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [["/" {:get {:handler index-handler}}]
     ["/api"
      ["" {:get {:handler api-handler}}]
      ["/initiate-call" {:post {:handler initiate-call-handler}}]]
     ["/items"
      ["" {:get {:handler index-handler}}]
      ["/:item-id" {:get {:handler index-handler
                          :parameters {:path {:item-id int?}}}}]]
     ["/about" {:get {:handler index-handler}}]
     ["/initiate-call" {:get {:handler index-handler}}]])
   (reitit-ring/routes
    (reitit-ring/create-resource-handler {:path "/" :root "/public"})
    (reitit-ring/create-default-handler))
   {:middleware middleware}))
