(ns voice-recordings.core
  (:require
   [accountant.core :as accountant]
   [clerk.core :as clerk]
   [cljs-http.client :as http]
   [cljs.core.async :refer [<! timeout]]
   [reagent.core :as reagent :refer [atom]]
   [reagent.dom :as rdom]
   [reagent.session :as session]
   [reitit.frontend :as reitit])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))


(defn get-anti-forgery-token []
  (-> js/document
      (.getElementById "__anti-forgery-token")
      (.getAttribute "data-token")))


;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/initiate-call" :initiate-call]
    ["/recordings/:recording-uuid" :recording]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Initiate call page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def phone-number (atom ""))
(def call-button-disabled? (atom false))

(defn check-recording-status! [recording-url]
  (go
    (let [response (<! (http/get recording-url))]
      (when (= 200 (:status response))
        (let [recording (js->clj (:body response) :keywordize-keys true)]
          (when (:recording_url recording)
            recording))))))

(defn poll-recording-status! [recording-url]
  (go
    (loop [attempt 1
           delay 1000]
      (when (<= attempt 100)
        (if-let [recording (<! (check-recording-status! recording-url))]
          (accountant/navigate!
            (path-for :recording {:recording-uuid (:uuid recording)}))
          (do
            (<! (timeout delay))
            (recur (inc attempt)
                   (min 10000 (* delay 1.5)))))))))

(defn initiate-call! []
  (go
    (reset! call-button-disabled? true)
    (let [response (<! (http/post
                         "/api/initiate-call"
                         {:json-params {:phone-number @phone-number}
                          :headers     {"x-csrf-token" (get-anti-forgery-token)}}))]
      (when (= 201 (:status response))
        (let [recording-url (get-in response [:headers "location"])]
          (poll-recording-status! recording-url))))))

(defn initiate-call-page []
  (fn []
    [:div.main
     [:img {:src "/img/typing.png"}]
     [:h1 "Record your voice over the phone and we’ll transcribe your story."]
     [:label {:for "phone"} "Your phone number"]
     [:input#phone {:type        "tel"
                    :name        "phone"
                    :value       @phone-number
                    :pattern     "[0-9]{3}-[0-9]{2}-[0-9]{3}"
                    :placeholder "123-456-7890"
                    :required    true
                    :on-change   #(reset! phone-number (.. % -target -value))}]
     [:input.call
      {:type     "button"
       :value    "Call me now"
       :on-click initiate-call!
       :disabled @call-button-disabled?}]
     [:hr]
     [:h2 "How it works"]
     [:ol
      [:li "Enter your phone number and Storyworth will call you to record your story."]
      [:li "During the call, we’ll record your story over the phone."]
      [:li "After the call, an audio clip of your recording will be uploaded to your story where you can then request it to be transcribed."]]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; Recording page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def transcribed? (atom false))
(def added-to-story? (atom false))
(def recording-date-time (atom ""))

(defn- transcribe! []
  (reset! transcribed? true))

(defn- add-to-story! []
  (reset! added-to-story? true))

(defn- get-ordinal-suffix [day]
  (let [day-num (js/parseInt day)]
    (cond
      (and (>= day-num 11) (<= day-num 13)) "th"
      (= (mod day-num 10) 1) "st"
      (= (mod day-num 10) 2) "nd"
      (= (mod day-num 10) 3) "rd"
      :else "th")))

(defn- format-12-hour [date]
  (let [hours (.getHours date)
        minutes (.getMinutes date)
        am-pm (if (< hours 12) "AM" "PM")
        display-hours (if (= hours 0) 12
                                      (if (> hours 12) (- hours 12) hours))
        display-minutes (if (< minutes 10) (str "0" minutes) (str minutes))]
    (str display-hours ":" display-minutes am-pm)))

(defn utc-to-local-formatted [utc-string]
  (let [date (js/Date. utc-string)
        days ["Sunday" "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday"]
        months ["January" "February" "March" "April" "May" "June"
                "July" "August" "September" "October" "November" "December"]
        day-of-week (nth days (.getDay date))
        month (nth months (.getMonth date))
        day (.getDate date)
        year (.getFullYear date)
        time (format-12-hour date)]
    (str day-of-week ", " month " " day (get-ordinal-suffix day) ", " year " " time)))

(defn- update-recording-date-time!
  [date-created]
  (reset! recording-date-time date-created))

(defn fetch-recording! []
  (go
    (let [routing-data (session/get :route)
          recording-uuid (get-in routing-data [:route-params :recording-uuid])
          recording-url (str "/api/recordings/" recording-uuid)
          response (<! (http/get recording-url))]
      (when (= 200 (:status response))
        (-> response
            :body
            (js->clj :keywordize-keys true)
            :date_created
            utc-to-local-formatted
            update-recording-date-time!)))))

(defn recording-page []
  (reagent/create-class
    {:component-did-mount fetch-recording!
     :reagent-render
     (fn []
       (let [routing-data (session/get :route)
             recording-uuid (get-in routing-data [:route-params :recording-uuid])
             recording-url (str "/api/recordings/" recording-uuid "/stream")]
         [:div.main
          [:img {:src "/img/typing.png"}]
          [:h1 "Listen to your recording and transcribe it."]
          [:h2 @recording-date-time]
          [:audio {:controls true}
           [:source {:type "audio/x-wav" :src recording-url}]
           "Your browser does not support the audio element."]
          (when-not @transcribed?
            [:input.transcribe
             {:type "button" :value "Transcribe" :on-click transcribe!}])
          (when @transcribed?
            [:input.add-to-story
             {:type     "button"
              :value    (if-not @added-to-story? "Add to story" "Added to story")
              :disabled @added-to-story?
              :on-click add-to-story!}])
          [:hr]
          [:h2 "What happens next"]
          [:ol
           [:li "You can listen to your phone recording above."]
           [:li "If you like your recording, you can transcribe it into text."]
           [:li "After that, you can add it to your story."]]]))}))

;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :initiate-call #'initiate-call-page
    :recording #'recording-page))


;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [page]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (rdom/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))

(defn ^:dev/after-load reload! []
  (mount-root))
