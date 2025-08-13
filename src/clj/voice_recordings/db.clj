(ns voice-recordings.db
  (:require
    [clojure.java.jdbc :as jdbc]
    [config.core :refer [env]]))

(def db-spec
  {:dbtype   "postgresql"
   :dbname   (env :postgres-db)
   :user     (env :postgres-user)
   :password (env :postgres-password)
   :host     (env :pghost)
   :port     (env :pgport)})

(defn get-recording!
  [recording-id]
  (-> (jdbc/query
        db-spec
        ["select * from recording where id = ?" recording-id])
      first))

(defn get-recordings! []
  (jdbc/query db-spec ["select * from recording"]))

(defn create-or-get-subject!
  "Create a subject with the provided phone number
  and returns the record.
  If a record for that phone number already exists,
  returns the existing record."
  [phone-number]
  (jdbc/with-db-transaction [txn db-spec]
    (let [subjects (->> {:phone_number phone-number}
                        (jdbc/find-by-keys txn :subject))]
      (if (empty? subjects)
        (->> {:phone_number phone-number}
             (jdbc/insert! txn :subject)
             first)
        (first subjects)))))

(defn create-recording!
  "Creates a recording with the provided values."
  [subject-id call-sid]
  (->> {:subject_id      subject-id
        :twilio_call_sid call-sid}
       (jdbc/insert! db-spec :recording)
       first))

(defn update-recording-by-call-sid!
  [call-sid recording-url]
  (jdbc/update!
    db-spec
    :recording
    {:recording_url recording-url}
    ["twilio_call_sid = ?" call-sid]))
