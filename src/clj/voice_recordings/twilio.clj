(ns voice-recordings.twilio
  (:require
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [config.core :refer [env]])
  (:import
    (com.twilio Twilio)
    (com.twilio.rest.api.v2010.account Call)
    (com.twilio.type PhoneNumber Twiml)))

(def ^String account-sid (env :twilio-account-sid))
(def ^String auth-token (env :twilio-auth-token))

(def ^String status-callback-url
  (if (env :production)
    (str "https://" (env :railway-public-domain) "/api/recording-status-callback")
    nil))

(defn make-call
  "Initiates a call with recording.
  Returns the call's SID."
  [to-phone-number]
  (Twilio/init account-sid auth-token)
  (let [twiml (-> "twiml/outbound.xml"
                  io/resource
                  slurp)
        _ (log/info "twiml" twiml)
        _ (log/info "to-phone-number" to-phone-number)
        _ (log/info "from-phone-number" (env :twilio-phone-number))
        call (-> (Call/creator
                   (PhoneNumber. to-phone-number)
                   (PhoneNumber. (env :twilio-phone-number))
                   (Twiml. twiml))
                 #_(.setRecordingStatusCallback status-callback-url)
                 (.create))]
    call))
