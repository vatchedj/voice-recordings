(ns voice-recordings.recording
  (:require
    [cljs-http.client :as http]
    [cljs.core.async :refer [<! timeout]]
    [reagent.core :as reagent :refer [atom]]
    [reagent.session :as session])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

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
