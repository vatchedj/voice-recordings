(ns voice-recordings.handler
  (:require
    [cheshire.core :as json]
    [reitit.ring :as reitit-ring]
    [ring.util.response :as response]
    [voice-recordings.middleware :refer [middleware]]
    [hiccup.page :refer [include-js include-css html5]]
    [config.core :refer [env]]))

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
   (include-css (if (env :dev) "/css/site.css" "/css/site.min.css"))])

(defn loading-page []
  (html5
   (head)
   [:body {:class "body-container"}
    mount-target
    (include-js "/js/app.js")]))


(defn index-handler
  [_request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (loading-page)})

(defn api-handler [_request]
  (-> {:message "Hello from API"
       :data [1 2 3 4 5]}
      json/generate-string
      response/response
      (response/content-type "application/json")))

(def app
  (reitit-ring/ring-handler
   (reitit-ring/router
    [["/" {:get {:handler index-handler}}]
     ["/api" {:get {:handler api-handler}}]
     ["/items"
      ["" {:get {:handler index-handler}}]
      ["/:item-id" {:get {:handler index-handler
                          :parameters {:path {:item-id int?}}}}]]
     ["/about" {:get {:handler index-handler}}]])
   (reitit-ring/routes
    (reitit-ring/create-resource-handler {:path "/" :root "/public"})
    (reitit-ring/create-default-handler))
   {:middleware middleware}))
