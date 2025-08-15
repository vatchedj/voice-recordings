(ns voice-recordings.pages.initiate-call
  (:require
    [accountant.core :as accountant]
    [cljs-http.client :as http]
    [cljs.core.async :refer [<! timeout]]
    [reagent.core :refer [atom]]
    [voice-recordings.common :as common]
    [voice-recordings.util :as util])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

(def phone-number (atom ""))
(def phone-valid? (atom true))
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
            (common/path-for :recording {:recording-uuid (:uuid recording)}))
          (do
            (<! (timeout delay))
            (recur (inc attempt)
                   (min 10000 (* delay 1.5)))))))))

(defn initiate-call! []
  (if-let [valid-phone (util/valid-phone-number @phone-number)]
    (go
      (reset! call-button-disabled? true)
      (reset! phone-valid? true)
      (let [response (<! (http/post
                           "/api/initiate-call"
                           {:json-params {:phone-number valid-phone}
                            :headers     {"x-csrf-token" (common/get-anti-forgery-token)}}))]
        (if (= 201 (:status response))
          (let [recording-url (get-in response [:headers "location"])]
            (poll-recording-status! recording-url))
          (reset! call-button-disabled? false))))
    (reset! phone-valid? false)))

(defn- phone-number-on-blur []
  (if (or (util/is-valid-phone-number? @phone-number)
          (empty? @phone-number))
    (reset! phone-valid? true)
    (reset! phone-valid? false)))

(defn initiate-call-page []
  (fn []
    [:div.main
     [:img {:src "/img/typing.png"}]
     [:h1 "Record your voice over the phone and we’ll transcribe your story."]
     [:label {:for "phone"} "Your phone number"]
     [:input#phone {:type        "tel"
                    :name        "phone"
                    :value       @phone-number
                    :pattern     "[0-9]{3}-[0-9]{3}-[0-9]{4}"
                    :placeholder "123-456-7890"
                    :required    true
                    :class       (when-not @phone-valid? "invalid")
                    :on-change   #(reset! phone-number (.. % -target -value))
                    :on-blur     phone-number-on-blur}]
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
