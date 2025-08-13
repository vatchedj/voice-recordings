(ns voice-recordings.handler
  (:require
    [cheshire.core :as json]
    [clojure.tools.logging :as log]
    [config.core :refer [env]]
    [hiccup.page :refer [include-js include-css html5]]
    [reitit.ring :as reitit-ring]
    [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
    [ring.util.response :as response]
    [voice-recordings.db :as db]
    [voice-recordings.middleware :refer [middleware]]
    [voice-recordings.twilio :as twilio]))

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
       :url     (-> (db/get-recordings!)
                    first
                    :recording_url)}
      json/generate-string
      response/response
      (response/content-type "application/json")))

(defn initiate-call-handler
  [request]
  (println "initiate-call-handler request (print):" request)
  (log/debug "initiate-call-handler request (log):" request)
  (let [body (-> request :body slurp (json/parse-string true))
        phone-number (:phone-number body)
        sanitized-phone-number (str "+1-" phone-number)
        subject (db/create-or-get-subject! sanitized-phone-number)
        call (twilio/make-call! sanitized-phone-number)
        call-sid (.getSid call)
        recording (db/create-recording! (:id subject) call-sid)]
    ; TODO: Return URL of recording?
    (response/created "RESPONSE!!!")))

(defn update-recording-handler
  [request]
  (println "update-recording-handler request (print):" request)
  (log/debug "update-recording-handler request (log):" request)
  (let [params (-> request :params)
        _ (println "params" params)
        call-sid (:CallSid params)
        _ (println "call-sid" call-sid)
        recording-url (:RecordingUrl params)
        _ (println "recording-url" recording-url)]
    (db/update-recording-by-call-sid!
      call-sid recording-url)
    (response/status 200)))

(defn get-recording-handler
  [request]
  (let [recording-id (-> request :path-params :recording-id)
        recording-url (some-> recording-id
                              db/get-recording!
                              :recording_url)
        recording (twilio/get-recording! recording-url)
        headers (:headers recording)]
    (-> (:body recording)
        response/response
        (response/content-type (:Content-Type headers)))))

(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [["/" {:get {:handler index-handler}}]
     ["/api"
      ["" {:get {:handler api-handler}}]
      ["/initiate-call" {:post {:handler initiate-call-handler}}]
      ["/recording-status-callback" {:post {:handler update-recording-handler}}]
      ["/recordings/:recording-id" {:get {:handler get-recording-handler}}]]
     ["/items"
      ["" {:get {:handler index-handler}}]
      ["/:item-id" {:get {:handler index-handler
                          :parameters {:path {:item-id int?}}}}]]
     ["/about" {:get {:handler index-handler}}]
     ["/initiate-call" {:get {:handler index-handler}}]
     ["/recordings/:recording-id" {:get {:handler index-handler}}]])
   (reitit-ring/routes
    (reitit-ring/create-resource-handler {:path "/" :root "/public"})
    (reitit-ring/create-default-handler))
   {:middleware middleware}))
