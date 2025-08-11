(ns voice-recordings.twilio
  (:require
    [clojure.java.io :as io]
    [config.core :refer [env]])
  (:import
    (com.twilio Twilio)
    (com.twilio.rest.api.v2010.account Call)
    (com.twilio.type PhoneNumber Twiml)))

(def ^String account-sid (env :twilio-account-sid))
(def ^String auth-token (env :twilio-auth-token))

(defn make-call
  "Initiates a call with recording.
  Returns the call's SID."
  [to-phone-number]
  (Twilio/init account-sid auth-token)
  (let [twiml (-> "twiml/outbound.xml"
                  io/resource
                  slurp)
        call (-> (Call/creator
                   (PhoneNumber. to-phone-number)
                   (PhoneNumber. (env :twilio-phone-number))
                   (Twiml. twiml))
                 (.create))]
    (.getSid call)))
